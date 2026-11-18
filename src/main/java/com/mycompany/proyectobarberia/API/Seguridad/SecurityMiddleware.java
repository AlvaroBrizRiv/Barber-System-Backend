package com.mycompany.proyectobarberia.API.Seguridad;

import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.ForbiddenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

/**
 * Middleware de seguridad para la API REST.
 *
 * Responsabilidades:
 *   1. Verificar que el token JWT sea válido en cada request protegido.
 *   2. Inyectar userId, rol y barberiaId como atributos del contexto Javalin
 *      para que los Controllers los usen sin re-parsear el token.
 *   3. Proveer métodos de autorización para que los Controllers verifiquen
 *      si el usuario tiene permisos suficientes y pertenece a la barbería correcta.
 *
 * PRINCIPIO DE SEGURIDAD:
 *   El barberiaId SIEMPRE se obtiene del token JWT, nunca del request body o URL.
 *   Esto impide que un admin de barbería A modifique datos de barbería B cambiando
 *   el parámetro en la request.
 */
public class SecurityMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityMiddleware.class);

    // Endpoints que NO requieren autenticación para cualquier método (POST, etc.)
    private static final Set<String> PUBLICAS_ANY = Set.of(
            "/api/auth/login",
            "/api/auth/registro",
            "/api/auth/sso/google",
            "/api/auth/mfa/verify",
            "/api/auth/recuperar/solicitar",
            "/api/auth/recuperar/verificar",
            "/api/auth/recuperar/restablecer"
    );

    // Endpoints públicos exclusivamente para GET
    private static final Set<String> PUBLICAS_GET = Set.of(
            "/api",
            "/api/",
            "/api/barberias"
    );

    // Prefijos de endpoints públicos exclusivamente para GET
    private static final Set<String> PREFIJOS_PUBLICOS = Set.of(
            "/api/barberias/",
            "/api/servicios/barberia/",
            "/api/calificaciones/barberia/",
            "/api/calificaciones/promedio/",
            "/api/calificaciones/barbero/",
            "/api/empleados/barberos/",
            "/api/productos/barberia/",
            "/api/suscripciones/barberia/"
    );

    /**
     * Determina si la ruta y método HTTP solicitados corresponden a un recurso público.
     * Los prefijos y rutas de consulta son públicos EXCLUSIVAMENTE para peticiones GET.
     */
    private static boolean esPublica(Context ctx) {
        String ruta = ctx.path();
        if (PUBLICAS_ANY.contains(ruta)) return true;
        if (ctx.method() != HandlerType.GET) return false;
        return PUBLICAS_GET.contains(ruta) || PREFIJOS_PUBLICOS.stream().anyMatch(ruta::startsWith);
    }

    // ─── Validación del JWT en cada request ──────────────────────────

    /**
     * Valida el JWT en el header Authorization.
     * Llamado por Javalin como before-handler en cada request.
     * Los Controllers pueden leer userId, rol y barberiaId del contexto.
     *
     * @param ctx Contexto de Javalin del request actual.
     */
    public static void validar(Context ctx) {
        // OPTIONS siempre pasa (pre-flight CORS)
        if (ctx.method() == HandlerType.OPTIONS) {
            return;
        }

        // Rutas y prefijos públicos
        if (esPublica(ctx)) {
            return;
        }

        // Todas las demás rutas requieren JWT válido
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Token de autenticación requerido.");
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = JwtUtils.validarToken(token);

            // Inyectar datos del token en el contexto para los Controllers
            ctx.attribute("userId",     JwtUtils.getUserId(claims));
            ctx.attribute("rol",        JwtUtils.getRol(claims));
            ctx.attribute("barberiaId", JwtUtils.getBarberiaId(claims));

        } catch (JwtException e) {
            LOG.warn("Token inválido desde {}: {}", ctx.ip(), e.getMessage());
            throw new UnauthorizedResponse("Token inválido o expirado. Inicia sesión nuevamente.");
        }
    }

    // ─── Métodos de autorización para Controllers ─────────────────────

    /**
     * Obtiene el userId del contexto (inyectado por el middleware).
     * Solo llamar después de que el middleware haya validado el token.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @return El ID del usuario actual
     */
    public static int getUserId(Context ctx) {
        return ctx.attribute("userId");
    }

    /**
     * Obtiene el rol del contexto.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @return El rol asignado al usuario
     */
    public static Rol getRol(Context ctx) {
        return ctx.attribute("rol");
    }

    /**
     * Obtiene el barberiaId del contexto (SIEMPRE del token, no del request).
     * Puede ser null para CLIENTE y SUPERADMIN.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @return El ID de la barbería o null
     */
    public static Integer getBarberiaId(Context ctx) {
        return ctx.attribute("barberiaId");
    }

    /**
     * Valida que el rol del usuario coincida exactamente con alguno de los permitidos.
     * Evita escalamiento de privilegios por ordinal y garantiza asignación de rol estricto.
     */
    public static void requerirRolExacto(Context ctx, Rol... permitidos) {
        Rol r = getRol(ctx);
        if (r == null || !Arrays.asList(permitidos).contains(r)) {
            throw new ForbiddenResponse("Acceso denegado.");
        }
    }

    /**
     * Obtiene el barberiaId obligatorio del JWT.
     * Lanza ForbiddenResponse si el usuario no pertenece a ninguna barbería (ej. SUPERADMIN o CLIENTE).
     */
    public static int requerirBarberiaId(Context ctx) {
        Integer id = getBarberiaId(ctx);
        if (id == null) {
            throw new ForbiddenResponse("Operación solo para personal de una barbería.");
        }
        return id;
    }

    /**
     * Verifica que el usuario tenga al menos el rol especificado.
     * Lanza ForbiddenResponse si no tiene permisos.
     *
     * @param ctx     Contexto del request.
     * @param rolMin  Rol mínimo requerido para la operación.
     */
    public static void requerirRol(Context ctx, Rol rolMin) {
        Rol rolUsuario = getRol(ctx);
        if (rolUsuario == null || !rolUsuario.tienePrivilegiosDeAlMenos(rolMin)) {
            throw new ForbiddenResponse("Acceso denegado: se requiere rol " + rolMin.name());
        }
    }

    /**
     * Verifica que el usuario sea ADMIN de UNA barbería específica.
     *
     * REGLA CRÍTICA: Un admin de barbería A NO puede modificar barbería B.
     * El barberiaId del JWT se compara con el recurso solicitado.
     *
     * @param ctx             Contexto del request.
     * @param barberiaIdRuta  ID de la barbería del recurso que se quiere acceder.
     */
    public static void requerirAdminDeBarberia(Context ctx, int barberiaIdRuta) {
        Rol rol = getRol(ctx);

        // SUPERADMIN puede acceder a cualquier barbería
        if (Rol.SUPERADMIN.equals(rol)) return;

        // Para ADMIN_BARBERIA, verificar que sea de ESA barbería
        if (!Rol.ADMIN_BARBERIA.equals(rol)) {
            throw new ForbiddenResponse("Acceso denegado: se requiere rol ADMIN_BARBERIA.");
        }

        Integer barberiaIdToken = getBarberiaId(ctx);
        if (barberiaIdToken == null || barberiaIdToken != barberiaIdRuta) {
            throw new ForbiddenResponse(
                    "Acceso denegado: no tienes permisos sobre esta barbería.");
        }
    }

    /**
     * Verifica que el usuario sea BARBERO o ADMIN de UNA barbería específica.
     * Usado para la agenda y acciones que los barberos pueden realizar.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @param barberiaIdRuta ID de la barbería extraída de la URL
     * @throws ForbiddenResponse si el usuario no tiene permisos sobre la barbería
     */
    public static void requerirEmpleadoDeBarberia(Context ctx, int barberiaIdRuta) {
        Rol rol = getRol(ctx);
        if (Rol.SUPERADMIN.equals(rol)) return;

        if (!Rol.BARBERO.equals(rol) && !Rol.ADMIN_BARBERIA.equals(rol)) {
            throw new ForbiddenResponse("Acceso denegado: se requiere ser empleado.");
        }

        Integer barberiaIdToken = getBarberiaId(ctx);
        if (barberiaIdToken == null || barberiaIdToken != barberiaIdRuta) {
            throw new ForbiddenResponse(
                    "Acceso denegado: no eres empleado de esta barbería.");
        }
    }

    // Constructor privado
    private SecurityMiddleware() {}
}
    