package com.mycompany.proyectobarberia.API.Seguridad;

import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SecurityMiddlewareTest {

    private static Context peticion(HandlerType metodo, String ruta, String authHeader) {
        Context ctx = mock(Context.class);
        when(ctx.method()).thenReturn(metodo);
        when(ctx.path()).thenReturn(ruta);
        when(ctx.header("Authorization")).thenReturn(authHeader);
        when(ctx.ip()).thenReturn("127.0.0.1");
        return ctx;
    }

    /** Contexto ya autenticado (atributos que inyecta el middleware). */
    static Context autenticado(int userId, Rol rol, Integer barberiaId) {
        Context ctx = mock(Context.class);
        when(ctx.<Integer>attribute("userId")).thenReturn(userId);
        when(ctx.<Rol>attribute("rol")).thenReturn(rol);
        when(ctx.<Integer>attribute("barberiaId")).thenReturn(barberiaId);
        return ctx;
    }

    @Nested
    class Validar {

        @Test
        void preflightOptionsPasaSinToken() {
            assertDoesNotThrow(() -> SecurityMiddleware.validar(peticion(HandlerType.OPTIONS, "/api/citas", null)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/barberias", "/api/barberias/1", "/api/servicios/barberia/1",
                "/api/productos/barberia/1", "/api/suscripciones/barberia/1", "/api/empleados/barberos/1"})
        void catalogoPublicoPorGet(String ruta) {
            assertDoesNotThrow(() -> SecurityMiddleware.validar(peticion(HandlerType.GET, ruta, null)));
        }

        @Test
        void loginYRegistroSonPublicos() {
            assertDoesNotThrow(() -> SecurityMiddleware.validar(peticion(HandlerType.POST, "/api/auth/login", null)));
            assertDoesNotThrow(() -> SecurityMiddleware.validar(peticion(HandlerType.POST, "/api/auth/registro", null)));
        }

        @Test
        void escrituraSobrePrefijoPublicoExigeToken() {
            // PATCH /api/barberias/1/suscripciones empieza con un prefijo público, pero no es GET
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.PATCH, "/api/barberias/1/suscripciones", null)));
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.PUT, "/api/barberias/1", null)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"/api/ganancias/mias", "/api/ganancias/barberia/1", "/api/perfil", "/api/citas/mis-citas"})
        void rutasPrivadasSinTokenDevuelven401(String ruta) {
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.GET, ruta, null)));
        }

        @Test
        void eliminarCuentaSinTokenDevuelve401() {
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.DELETE, "/api/perfil", null)));
        }

        @Test
        void cabeceraSinBearerDevuelve401() {
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.GET, "/api/perfil", "Basic abc")));
        }

        @Test
        void tokenInvalidoDevuelve401() {
            assertThrows(UnauthorizedResponse.class, () ->
                    SecurityMiddleware.validar(peticion(HandlerType.GET, "/api/perfil", "Bearer x.y.z")));
        }

        @Test
        void tokenValidoInyectaDatosDelUsuario() {
            String token = JwtUtils.generarToken(4, Rol.BARBERO, 1);
            Context ctx = peticion(HandlerType.GET, "/api/ganancias/mias", "Bearer " + token);
            SecurityMiddleware.validar(ctx);
            verify(ctx).attribute("userId", 4);
            verify(ctx).attribute("rol", Rol.BARBERO);
            verify(ctx).attribute("barberiaId", 1);
        }

        @Test
        void rutaPublicaNoLeeElToken() {
            Context ctx = peticion(HandlerType.GET, "/api/barberias", "Bearer basura");
            SecurityMiddleware.validar(ctx);
            verify(ctx, never()).attribute(anyString(), any());
        }
    }

    @Nested
    class Autorizacion {

        @Test
        void adminDeOtraBarberiaEsRechazado() {
            Context ctx = autenticado(2, Rol.ADMIN_BARBERIA, 1);
            assertThrows(ForbiddenResponse.class, () -> SecurityMiddleware.requerirAdminDeBarberia(ctx, 2));
            assertDoesNotThrow(() -> SecurityMiddleware.requerirAdminDeBarberia(ctx, 1));
        }

        @Test
        void barberoNoEsAdmin() {
            Context ctx = autenticado(3, Rol.BARBERO, 1);
            assertThrows(ForbiddenResponse.class, () -> SecurityMiddleware.requerirAdminDeBarberia(ctx, 1));
        }

        @Test
        void superadminAccedeACualquierBarberia() {
            Context ctx = autenticado(1, Rol.SUPERADMIN, null);
            assertDoesNotThrow(() -> SecurityMiddleware.requerirAdminDeBarberia(ctx, 99));
        }

        @Test
        void clienteNoEsEmpleado() {
            Context ctx = autenticado(5, Rol.CLIENTE, null);
            assertThrows(ForbiddenResponse.class, () -> SecurityMiddleware.requerirEmpleadoDeBarberia(ctx, 1));
        }

        @Test
        void barberoSoloEnSuBarberia() {
            Context ctx = autenticado(3, Rol.BARBERO, 1);
            assertDoesNotThrow(() -> SecurityMiddleware.requerirEmpleadoDeBarberia(ctx, 1));
            assertThrows(ForbiddenResponse.class, () -> SecurityMiddleware.requerirEmpleadoDeBarberia(ctx, 2));
        }

        @Test
        void rolExactoNoPermiteEscaladaPorJerarquia() {
            Context ctx = autenticado(1, Rol.SUPERADMIN, null);
            assertThrows(ForbiddenResponse.class, () -> SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE));
        }

        @Test
        void requerirBarberiaIdFallaSinBarberia() {
            assertThrows(ForbiddenResponse.class, () ->
                    SecurityMiddleware.requerirBarberiaId(autenticado(5, Rol.CLIENTE, null)));
            assertEquals(1, SecurityMiddleware.requerirBarberiaId(autenticado(3, Rol.BARBERO, 1)));
        }
    }
}
