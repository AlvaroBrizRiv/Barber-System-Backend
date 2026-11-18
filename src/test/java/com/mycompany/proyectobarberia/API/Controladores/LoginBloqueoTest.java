package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.PoliticaBloqueo;
import com.mycompany.proyectobarberia.Persistencia.DAO.BloqueoLoginDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.ClienteDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.SuperAdminDAO;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Login con bloqueo progresivo: se simula el estado guardado en BD con un mapa en memoria
 * y un reloj controlable para avanzar el tiempo.
 */
class LoginBloqueoTest {

    private static final ZoneId ZONA = ZoneId.of("UTC");
    private static final AtomicInteger IP = new AtomicInteger();
    private static final String EMAIL = "pedro@test.cl";
    private static final String PASSWORD = "Cliente123!";

    private BloqueoLoginDAO bloqueo;
    private PoliticaBloqueo.Estado estadoBd;
    private Instant ahora;

    @BeforeEach
    void setUp() throws Exception {
        ClienteDAO clientes = mock(ClienteDAO.class);
        EmpleadoDAO empleados = mock(EmpleadoDAO.class);
        SuperAdminDAO supers = mock(SuperAdminDAO.class);
        bloqueo = mock(BloqueoLoginDAO.class);

        Cliente pedro = new Cliente("33333333-3", "RUT", "Pedro", "González", EMAIL, null,
                BCrypt.withDefaults().hashToString(4, PASSWORD.toCharArray()), "CL");
        pedro.setId(9);
        when(clientes.buscarPorEmail(EMAIL)).thenReturn(pedro);

        estadoBd = PoliticaBloqueo.Estado.LIMPIO;
        when(bloqueo.obtener(9)).thenAnswer(i -> estadoBd);
        doAnswer(i -> { estadoBd = i.getArgument(1); return null; }).when(bloqueo).guardar(eq(9), any());
        doAnswer(i -> { estadoBd = PoliticaBloqueo.Estado.LIMPIO; return null; }).when(bloqueo).reiniciar(9);

        ahora = Instant.parse("2026-11-18T12:00:00Z");
        AuthController.configurar(clientes, empleados, supers, bloqueo, relojMovil());
    }

    @AfterEach
    void tearDown() {
        AuthController.configurar(new ClienteDAO(), new EmpleadoDAO(), new SuperAdminDAO(),
                new BloqueoLoginDAO(), Clock.systemDefaultZone());
    }

    /** Reloj que siempre devuelve el valor actual de "ahora" (permite avanzar el tiempo). */
    private Clock relojMovil() {
        return new Clock() {
            @Override public ZoneId getZone() { return ZONA; }
            @Override public Clock withZone(ZoneId z) { return this; }
            @Override public Instant instant() { return ahora; }
        };
    }

    /** Resultado de un intento: status HTTP (200/401/423/429) y JSON devuelto. */
    private record Intento(int status, Map<String, Object> json) { }

    @SuppressWarnings("unchecked")
    private Intento intentar(String password) throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.ip()).thenReturn("10.20." + IP.incrementAndGet() + ".1");
        when(ctx.bodyAsClass(Map.class)).thenReturn(Map.of("email", EMAIL, "password", password));
        when(ctx.status(anyInt())).thenReturn(ctx);
        try {
            AuthController.login(ctx);
        } catch (UnauthorizedResponse e) {
            return new Intento(401, Map.of("error", e.getMessage()));
        }
        ArgumentCaptor<Integer> status = ArgumentCaptor.forClass(Integer.class);
        verify(ctx, atMost(1)).status(status.capture());
        ArgumentCaptor<Object> json = ArgumentCaptor.forClass(Object.class);
        verify(ctx).json(json.capture());
        int s = status.getAllValues().isEmpty() ? 200 : status.getValue();
        return new Intento(s, (Map<String, Object>) json.getValue());
    }

    private void avanzar(long segundos) { ahora = ahora.plusSeconds(segundos); }

    @Test
    void flujoCompletoDeBloqueoProgresivo() throws Exception {
        // Fallos 1-4 → 401
        for (int i = 1; i <= 4; i++) assertEquals(401, intentar("mala").status(), "fallo " + i);

        // Fallo 5 → espera 15 s
        Intento quinto = intentar("mala");
        assertEquals(429, quinto.status());
        assertEquals("SUSPENDIDA", quinto.json().get("codigo"));
        assertEquals(15L, quinto.json().get("segundosRestantes"));

        // Durante la espera, ni siquiera la contraseña correcta entra y no suma intentos
        avanzar(10);
        Intento enEspera = intentar(PASSWORD);
        assertEquals(429, enEspera.status());
        assertEquals(5L, enEspera.json().get("segundosRestantes"));
        assertEquals(5, estadoBd.intentosFallidos());

        // Pasada la espera: fallos 6-9 → 401, fallo 10 → espera 30 s
        avanzar(5);
        for (int i = 6; i <= 9; i++) assertEquals(401, intentar("mala").status(), "fallo " + i);
        Intento decimo = intentar("mala");
        assertEquals(429, decimo.status());
        assertEquals(30L, decimo.json().get("segundosRestantes"));

        avanzar(29);
        assertEquals(429, intentar("mala").status(), "todavía en espera");
        avanzar(1);

        // Fallos 11-14 → 401, fallo 15 → cuenta bloqueada
        for (int i = 11; i <= 14; i++) assertEquals(401, intentar("mala").status(), "fallo " + i);
        Intento quince = intentar("mala");
        assertEquals(423, quince.status());
        assertEquals("BLOQUEADA", quince.json().get("codigo"));
        assertTrue(estadoBd.bloqueada());

        // Bloqueada: ni con la contraseña correcta, ni pasando el tiempo
        avanzar(86_400);
        assertEquals(423, intentar(PASSWORD).status());
    }

    @Test
    void loginCorrectoReiniciaElContador() throws Exception {
        for (int i = 0; i < 3; i++) intentar("mala");
        assertEquals(3, estadoBd.intentosFallidos());

        Intento ok = intentar(PASSWORD);
        assertEquals(200, ok.status());
        assertNotNull(ok.json().get("token"));
        verify(bloqueo).reiniciar(9);
        assertEquals(0, estadoBd.intentosFallidos());

        // Tras reiniciar, vuelve a tener 4 fallos "libres"
        for (int i = 1; i <= 4; i++) assertEquals(401, intentar("mala").status());
        assertEquals(429, intentar("mala").status());
    }

    @Test
    void loginCorrectoSinFallosPreviosNoEscribeEnBd() throws Exception {
        assertEquals(200, intentar(PASSWORD).status());
        verify(bloqueo, never()).reiniciar(anyInt());
        verify(bloqueo, never()).guardar(anyInt(), any());
    }

    @Test
    void cuentaYaBloqueadaNoEvaluaLaContrasena() throws Exception {
        estadoBd = new PoliticaBloqueo.Estado(15, null, true);
        Intento r = intentar(PASSWORD);
        assertEquals(423, r.status());
        assertTrue(r.json().get("error").toString().contains("Olvidaste tu contraseña"));
        verify(bloqueo, never()).guardar(anyInt(), any());
    }

    @Test
    void esperaGuardadaSeRespetaAlRecargar() throws Exception {
        estadoBd = new PoliticaBloqueo.Estado(10, LocalDateTime.ofInstant(ahora, ZONA).plusSeconds(20), false);
        Intento r = intentar("mala");
        assertEquals(429, r.status());
        assertEquals(20L, r.json().get("segundosRestantes"));
    }
}
