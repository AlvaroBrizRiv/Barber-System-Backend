package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Validaciones que ocurren ANTES de tocar la base de datos
 * (permisos y datos de entrada), por lo que no requieren conexión.
 */
class CuentaYBarberiaControllerTest {

    private static Context ctx(int userId, Rol rol, Integer barberiaId, Map<String, Object> body) {
        Context ctx = mock(Context.class);
        when(ctx.<Integer>attribute("userId")).thenReturn(userId);
        when(ctx.<Rol>attribute("rol")).thenReturn(rol);
        when(ctx.<Integer>attribute("barberiaId")).thenReturn(barberiaId);
        when(ctx.ip()).thenReturn("10.0.0." + userId);
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);
        return ctx;
    }

    // ─── DELETE /api/perfil ───────────────────────────────────────────

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = {"BARBERO", "ADMIN_BARBERIA", "SUPERADMIN"})
    void soloElClientePuedeEliminarSuCuenta(Rol rol) {
        Context c = ctx(100 + rol.ordinal(), rol, 1, Map.of("password", "x"));
        assertThrows(ForbiddenResponse.class, () -> PerfilController.eliminarCuenta(c));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void eliminarCuentaExigeContrasena(String pwd) {
        Map<String, Object> body = new HashMap<>();
        body.put("password", pwd);
        Context c = ctx(200, Rol.CLIENTE, null, body);
        assertThrows(BadRequestResponse.class, () -> PerfilController.eliminarCuenta(c));
    }

    @Test
    void eliminarCuentaRechazaContrasenaNoTexto() {
        Context c = ctx(201, Rol.CLIENTE, null, Map.of("password", 12345));
        assertThrows(BadRequestResponse.class, () -> PerfilController.eliminarCuenta(c));
    }

    @Test
    void eliminarCuentaConCuerpoVacio() {
        Context c = ctx(202, Rol.CLIENTE, null, null);
        assertThrows(BadRequestResponse.class, () -> PerfilController.eliminarCuenta(c));
    }

    // ─── PATCH /api/barberias/{id}/suscripciones ──────────────────────

    @Test
    void barberoNoPuedeConfigurarSuscripciones() {
        Context c = ctx(3, Rol.BARBERO, 1, Map.of("habilitadas", true));
        when(c.pathParam("id")).thenReturn("1");
        assertThrows(ForbiddenResponse.class, () -> BarberiaController.configurarSuscripciones(c));
    }

    @Test
    void adminNoPuedeConfigurarOtraBarberia() {
        Context c = ctx(2, Rol.ADMIN_BARBERIA, 1, Map.of("habilitadas", true));
        when(c.pathParam("id")).thenReturn("2");
        assertThrows(ForbiddenResponse.class, () -> BarberiaController.configurarSuscripciones(c));
    }

    @Test
    void habilitadasDebeSerBooleano() {
        Context c = ctx(2, Rol.ADMIN_BARBERIA, 1, Map.of("habilitadas", "si"));
        when(c.pathParam("id")).thenReturn("1");
        assertThrows(BadRequestResponse.class, () -> BarberiaController.configurarSuscripciones(c));

        Context sinCampo = ctx(2, Rol.ADMIN_BARBERIA, 1, Map.of());
        when(sinCampo.pathParam("id")).thenReturn("1");
        assertThrows(BadRequestResponse.class, () -> BarberiaController.configurarSuscripciones(sinCampo));
    }

    // ─── AuthController ───────────────────────────────────────────────

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1234567"})
    void passwordCortaONulaEsRechazada(String pwd) {
        assertThrows(BadRequestResponse.class, () -> AuthController.validarPassword(pwd));
    }

    @Test
    void passwordMayorA72BytesEsRechazada() {
        assertThrows(BadRequestResponse.class, () -> AuthController.validarPassword("a".repeat(73)));
        assertDoesNotThrow(() -> AuthController.validarPassword("a".repeat(72)));
        assertDoesNotThrow(() -> AuthController.validarPassword("Cliente123!"));
    }

    @Test
    void datosUsuarioNoExponePiiSensible() {
        Cliente c = new Cliente("12345678-9", "RUT", "Ana", "López", "ana@test.cl", "+569", "$2a$12$hash", "CL");
        Map<String, Object> datos = AuthController.datosUsuario(c);
        assertFalse(datos.containsKey("passwordHash"));
        assertFalse(datos.containsKey("documentoIdentidad"));
        assertFalse(datos.containsValue("$2a$12$hash"));
        assertEquals("Ana", datos.get("nombre"));
        assertEquals("es", datos.get("idiomaPreferido"));
        assertEquals("", datos.get("fotoUrl"));
    }

    @Test
    void permisosSeVerificanAntesDeLeerElCuerpo() {
        Context c = ctx(3, Rol.BARBERO, 1, Map.of());
        when(c.pathParam("id")).thenReturn("1");
        assertThrows(ForbiddenResponse.class, () -> BarberiaController.configurarSuscripciones(c));
        verify(c, never()).bodyAsClass(any());
    }
}
