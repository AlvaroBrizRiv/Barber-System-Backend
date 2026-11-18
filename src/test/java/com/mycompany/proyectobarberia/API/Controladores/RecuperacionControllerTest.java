package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.API.Seguridad.JwtUtils;
import com.mycompany.proyectobarberia.Persistencia.DAO.RecuperacionDAO;
import com.mycompany.proyectobarberia.Servicios.EmailService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecuperacionControllerTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final Instant INSTANTE = Instant.parse("2026-11-18T15:00:00Z");
    private static final LocalDateTime AHORA = LocalDateTime.ofInstant(INSTANTE, ZONA);
    private static final AtomicInteger IP = new AtomicInteger(1); // IP distinta por prueba (rate limit)

    private static final RecuperacionDAO.PersonaRecuperable ANA =
            new RecuperacionDAO.PersonaRecuperable(7, "Ana", "ana@test.cl");

    private RecuperacionDAO dao;
    private EmailService email;

    @BeforeEach
    void setUp() {
        dao = mock(RecuperacionDAO.class);
        email = mock(EmailService.class);
        // Executor síncrono: el envío "en segundo plano" se ejecuta dentro de la prueba
        RecuperacionController.configurar(dao, email, Runnable::run, Clock.fixed(INSTANTE, ZONA));
    }

    @AfterEach
    void tearDown() {
        RecuperacionController.configurar(new RecuperacionDAO(), null, Runnable::run, Clock.systemDefaultZone());
    }

    private static Context ctx(Map<String, Object> body) {
        Context c = mock(Context.class);
        when(c.ip()).thenReturn("10.9." + IP.incrementAndGet() + ".1");
        when(c.bodyAsClass(Map.class)).thenReturn(body);
        return c;
    }

    private static String hash(String codigo) {
        return BCrypt.withDefaults().hashToString(4, codigo.toCharArray());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> respuesta(Context c) {
        ArgumentCaptor<Object> json = ArgumentCaptor.forClass(Object.class);
        verify(c).json(json.capture());
        return (Map<String, Object>) json.getValue();
    }

    // ─── Generación del código ────────────────────────────────────────

    @Test
    void codigoTieneSeisDigitos() {
        for (int i = 0; i < 500; i++) {
            assertTrue(RecuperacionController.generarCodigo().matches("\\d{6}"));
        }
    }

    // ─── 1. Solicitar ─────────────────────────────────────────────────

    @Test
    void correoInexistenteRespondeIgualYNoEnviaNada() throws Exception {
        when(dao.buscarPersonaActiva("nadie@test.cl")).thenReturn(null);
        Context c = ctx(Map.of("email", "nadie@test.cl"));

        RecuperacionController.solicitar(c);

        assertEquals(RecuperacionController.MSG_SOLICITUD, respuesta(c).get("mensaje"));
        verify(dao, never()).crearCodigo(anyInt(), any(), any(), any());
        verifyNoInteractions(email);
    }

    @Test
    void correoExistenteGuardaHashYEnviaElMismoCodigo() throws Exception {
        when(dao.buscarPersonaActiva("ana@test.cl")).thenReturn(ANA);
        when(dao.contarSolicitudesDesde(eq(7), any())).thenReturn(0);
        Context c = ctx(Map.of("email", "  ana@test.cl "));

        RecuperacionController.solicitar(c);

        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiraCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(dao).crearCodigo(eq(7), hashCap.capture(), eq(AHORA), expiraCap.capture());
        assertEquals(AHORA.plusMinutes(15), expiraCap.getValue());

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(email).enviar(eq("ana@test.cl"), anyString(), cuerpo.capture());
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(cuerpo.getValue());
        assertTrue(m.find(), "el correo incluye el código");
        String codigo = m.group(1);

        assertNotEquals(codigo, hashCap.getValue(), "nunca se guarda el código en claro");
        assertTrue(BCrypt.verifyer().verify(codigo.toCharArray(), hashCap.getValue()).verified);
        assertEquals(RecuperacionController.MSG_SOLICITUD, respuesta(c).get("mensaje"));
    }

    @Test
    void demasiadasSolicitudesNoGeneranMasCodigos() throws Exception {
        when(dao.buscarPersonaActiva("ana@test.cl")).thenReturn(ANA);
        when(dao.contarSolicitudesDesde(eq(7), eq(AHORA.minusMinutes(15)))).thenReturn(3);
        Context c = ctx(Map.of("email", "ana@test.cl"));

        RecuperacionController.solicitar(c);

        verify(dao, never()).crearCodigo(anyInt(), any(), any(), any());
        verifyNoInteractions(email);
        assertEquals(RecuperacionController.MSG_SOLICITUD, respuesta(c).get("mensaje"), "misma respuesta");
    }

    @Test
    void falloDelCorreoNoRompeLaRespuesta() throws Exception {
        when(dao.buscarPersonaActiva("ana@test.cl")).thenReturn(ANA);
        doThrow(new RuntimeException("SMTP caído")).when(email).enviar(any(), any(), any());
        Context c = ctx(Map.of("email", "ana@test.cl"));

        assertDoesNotThrow(() -> RecuperacionController.solicitar(c));
        assertEquals(RecuperacionController.MSG_SOLICITUD, respuesta(c).get("mensaje"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "sin-arroba", "a@b", "con espacio@test.cl"})
    void correoInvalidoEs400(String correo) {
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.solicitar(ctx(Map.of("email", correo))));
    }

    @Test
    void cuerpoVacioOCorreoNoTextoEs400() {
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.solicitar(ctx(null)));
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.solicitar(ctx(Map.of("email", 123))));
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.solicitar(ctx(Map.of("email", "a".repeat(145) + "@test.cl"))));
    }

    // ─── 2. Verificar ─────────────────────────────────────────────────

    private void codigoVigente(String codigo, LocalDateTime expira, int intentos) throws Exception {
        when(dao.buscarPersonaActiva("ana@test.cl")).thenReturn(ANA);
        when(dao.buscarVigente(7)).thenReturn(new RecuperacionDAO.Codigo(55, 7, hash(codigo), expira, intentos));
    }

    @Test
    void codigoCorrectoDevuelveTokenDeRecuperacion() throws Exception {
        codigoVigente("012345", AHORA.plusMinutes(5), 0);
        Context c = ctx(Map.of("email", "ana@test.cl", "codigo", "012345"));

        RecuperacionController.verificar(c);

        Map<String, Object> r = respuesta(c);
        String token = (String) r.get("resetToken");
        JwtUtils.DatosRecuperacion datos = JwtUtils.validarTokenRecuperacion(token);
        assertEquals(7, datos.personaId());
        assertEquals(55, datos.codigoId());
        assertEquals(10, r.get("expiraEnMinutos"));
        verify(dao, never()).registrarIntentoFallido(anyInt());
    }

    @Test
    void tokenDeRecuperacionNoSirveComoSesion() throws Exception {
        codigoVigente("123456", AHORA.plusMinutes(5), 0);
        Context c = ctx(Map.of("email", "ana@test.cl", "codigo", "123456"));
        RecuperacionController.verificar(c);
        String token = (String) respuesta(c).get("resetToken");

        assertThrows(JwtException.class, () -> JwtUtils.validarToken(token));
        String sesion = JwtUtils.generarToken(7, com.mycompany.proyectobarberia.Logica.enums.Rol.CLIENTE, null);
        assertThrows(JwtException.class, () -> JwtUtils.validarTokenRecuperacion(sesion),
                "y un token de sesión tampoco sirve para restablecer");
    }

    @Test
    void codigoIncorrectoSumaIntento() throws Exception {
        codigoVigente("123456", AHORA.plusMinutes(5), 2);
        Context c = ctx(Map.of("email", "ana@test.cl", "codigo", "654321"));

        BadRequestResponse e = assertThrows(BadRequestResponse.class, () -> RecuperacionController.verificar(c));
        assertEquals(RecuperacionController.MSG_CODIGO_INVALIDO, e.getMessage());
        verify(dao).registrarIntentoFallido(55);
    }

    @Test
    void codigoExpiradoSeRechazaAunqueSeaCorrecto() throws Exception {
        codigoVigente("123456", AHORA.minusSeconds(1), 0);
        Context c = ctx(Map.of("email", "ana@test.cl", "codigo", "123456"));

        assertThrows(BadRequestResponse.class, () -> RecuperacionController.verificar(c));
        verify(dao, never()).registrarIntentoFallido(anyInt());
    }

    @Test
    void trasCincoIntentosElCodigoQuedaBloqueado() throws Exception {
        codigoVigente("123456", AHORA.plusMinutes(5), 5);
        Context c = ctx(Map.of("email", "ana@test.cl", "codigo", "123456"));
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.verificar(c));
    }

    @Test
    void correoDesconocidoDaElMismoMensaje() throws Exception {
        when(dao.buscarPersonaActiva("x@test.cl")).thenReturn(null);
        BadRequestResponse e = assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.verificar(ctx(Map.of("email", "x@test.cl", "codigo", "123456"))));
        assertEquals(RecuperacionController.MSG_CODIGO_INVALIDO, e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "1234567", "abcdef", "12 456"})
    void formatoDeCodigoInvalidoEs400SinConsultarBd(String codigo) throws Exception {
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.verificar(ctx(Map.of("email", "ana@test.cl", "codigo", codigo))));
        verify(dao, never()).buscarVigente(anyInt());
    }

    // ─── 3. Restablecer ───────────────────────────────────────────────

    @Test
    void restableceConTokenValido() throws Exception {
        String token = JwtUtils.generarTokenRecuperacion(7, 55, 10);
        when(dao.restablecerPassword(eq(55), eq(7), anyString())).thenReturn(true);
        Context c = ctx(Map.of("resetToken", token, "password", "NuevaClave2026!"));

        RecuperacionController.restablecer(c);

        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        verify(dao).restablecerPassword(eq(55), eq(7), hashCap.capture());
        assertTrue(BCrypt.verifyer().verify("NuevaClave2026!".toCharArray(), hashCap.getValue()).verified);
        assertTrue(respuesta(c).get("mensaje").toString().contains("actualizada"));
    }

    @Test
    void tokenYaUsadoEs400() throws Exception {
        String token = JwtUtils.generarTokenRecuperacion(7, 55, 10);
        when(dao.restablecerPassword(anyInt(), anyInt(), anyString())).thenReturn(false);
        BadRequestResponse e = assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.restablecer(ctx(Map.of("resetToken", token, "password", "NuevaClave2026!"))));
        assertEquals(RecuperacionController.MSG_TOKEN_INVALIDO, e.getMessage());
    }

    @Test
    void tokenInvalidoOExpiradoEs400() throws Exception {
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.restablecer(ctx(Map.of("resetToken", "x.y.z", "password", "NuevaClave2026!"))));
        String expirado = JwtUtils.generarTokenRecuperacion(7, 55, -1);
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.restablecer(ctx(Map.of("resetToken", expirado, "password", "NuevaClave2026!"))));
        verify(dao, never()).restablecerPassword(anyInt(), anyInt(), anyString());
    }

    @Test
    void passwordInvalidaEs400SinTocarBd() throws Exception {
        String token = JwtUtils.generarTokenRecuperacion(7, 55, 10);
        Map<String, Object> sinPassword = new HashMap<>();
        sinPassword.put("resetToken", token);
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.restablecer(ctx(sinPassword)));
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.restablecer(ctx(Map.of("resetToken", token, "password", "corta"))));
        assertThrows(BadRequestResponse.class, () ->
                RecuperacionController.restablecer(ctx(Map.of("resetToken", token, "password", "a".repeat(73)))));
        assertThrows(BadRequestResponse.class, () -> RecuperacionController.restablecer(ctx(null)));
        verify(dao, never()).restablecerPassword(anyInt(), anyInt(), anyString());
    }
}
