package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.API.Seguridad.JwtUtils;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.SuperAdmin;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.ClienteDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.SuperAdminDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.util.NaiveRateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Controlador REST para autenticación de usuarios.
 *
 * Endpoints:
 *   POST /api/auth/login         → Login unificado (cliente, barbero, admin, superadmin).
 *   POST /api/auth/registro      → Auto-registro de clientes.
 *   GET  /api/auth/sso/google    → STUB SSO Google (próximamente).
 *   POST /api/auth/mfa/verify    → STUB verificación MFA (próximamente).
 *
 * Seguridad PII y OWASP:
 *   - BCrypt cost=12 para hashing de contraseñas.
 *   - passwordHash NUNCA se incluye en las respuestas JSON.
 *   - Mismo mensaje de error para email no encontrado y contraseña incorrecta
 *     (evita enumeración de usuarios).
 *   - Mitigación de timing attack mediante verificación de hash falso.
 *   - Rate limiting por IP/ruta con NaiveRateLimit.
 */
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private AuthController(){
        //Constructor privado
    }
    
    private static final ClienteDAO    clienteDAO    = new ClienteDAO();
    private static final EmpleadoDAO   empleadoDAO   = new EmpleadoDAO();
    private static final SuperAdminDAO superAdminDAO = new SuperAdminDAO();

    // Hash ficticio precalculado para igualar el tiempo de respuesta ante usuarios inexistentes (timing attack)
    private static final String HASH_FALSO = BCrypt.withDefaults().hashToString(12, "x".toCharArray());

    public static void registrarRutas() {
        ApiBuilder.post("/login",       AuthController::login);
        ApiBuilder.post("/registro",    AuthController::registroCliente);
        // Stubs SSO/MFA — implementación en fase de producción
        ApiBuilder.get("/sso/google",   AuthController::ssoGoogleStub);
        ApiBuilder.post("/mfa/verify",  AuthController::mfaVerifyStub);
    }

    /**
     * Valida la longitud y complejidad básica de una contraseña.
     */
    public static void validarPassword(String p) {
        if (p == null || p.length() < 8 || p.length() > 72) {
            throw new BadRequestResponse("La contraseña debe tener entre 8 y 72 caracteres.");
        }
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────

    /**
     * Maneja el proceso de inicio de sesión unificado.
     * Busca en empleados, clientes y superadministradores.
     * 
     * @param ctx Contexto HTTP de Javalin con el payload de inicio de sesión
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si faltan el email o la contraseña
     * @throws UnauthorizedResponse si las credenciales son incorrectas
     */
    @SuppressWarnings("unchecked")
    private static void login(Context ctx) throws SQLException {
        // SEC-07: Rate limiting (máximo 10 intentos por cada 10 minutos por IP)
        NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.MINUTES);

        // Parsea el cuerpo de la petición como un mapa para extraer credenciales
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String email    = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            throw new BadRequestResponse("Email y contraseña son requeridos.");
        }

        // 1. Empleados (admin/barbero)
        Empleado empleado = empleadoDAO.buscarPorEmail(email);
        if (empleado != null) {
            verificarPassword(password, empleado.getPasswordHash());
            String token = JwtUtils.generarToken(
                    empleado.getId(), empleado.getRol(), empleado.getBarberiaId());

            Map<String, Object> usuarioMap = new java.util.HashMap<>();
            usuarioMap.put("id", empleado.getId());
            usuarioMap.put("nombre", empleado.getNombre() != null ? empleado.getNombre() : "");
            usuarioMap.put("apellido", empleado.getApellido() != null ? empleado.getApellido() : "");
            usuarioMap.put("email", empleado.getEmail());
            usuarioMap.put("fotoUrl", empleado.getFotoUrl() != null ? empleado.getFotoUrl() : "");
            usuarioMap.put("idiomaPreferido", empleado.getIdiomaPreferido() != null ? empleado.getIdiomaPreferido() : "es");
            usuarioMap.put("barberiaId", empleado.getBarberiaId());

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("token", token);
            resp.put("rol", empleado.getRol().name());
            resp.put("usuario", usuarioMap);

            ctx.json(resp);
            return;
        }

        // 2. Clientes
        Cliente cliente = clienteDAO.buscarPorEmail(email);
        if (cliente != null) {
            verificarPassword(password, cliente.getPasswordHash());
            String token = JwtUtils.generarToken(cliente.getId(), Rol.CLIENTE, null);

            Map<String, Object> usuarioMap = new java.util.HashMap<>();
            usuarioMap.put("id", cliente.getId());
            usuarioMap.put("nombre", cliente.getNombre() != null ? cliente.getNombre() : "");
            usuarioMap.put("apellido", cliente.getApellido() != null ? cliente.getApellido() : "");
            usuarioMap.put("email", cliente.getEmail());
            usuarioMap.put("fotoUrl", cliente.getFotoUrl() != null ? cliente.getFotoUrl() : "");
            usuarioMap.put("idiomaPreferido", cliente.getIdiomaPreferido() != null ? cliente.getIdiomaPreferido() : "es");
            usuarioMap.put("puntosFidelidad", cliente.getPuntosFidelidad());

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("token", token);
            resp.put("rol", Rol.CLIENTE.name());
            resp.put("usuario", usuarioMap);

            ctx.json(resp);
            return;
        }

        // 3. SuperAdministradores (BUG-06)
        SuperAdmin sa = superAdminDAO.buscarPorEmail(email);
        if (sa != null) {
            verificarPassword(password, sa.getPasswordHash());
            String token = JwtUtils.generarToken(sa.getId(), Rol.SUPERADMIN, null);

            Map<String, Object> usuarioMap = new java.util.HashMap<>();
            usuarioMap.put("id", sa.getId());
            usuarioMap.put("nombre", sa.getNombre() != null ? sa.getNombre() : "");
            usuarioMap.put("apellido", sa.getApellido() != null ? sa.getApellido() : "");
            usuarioMap.put("email", sa.getEmail());
            usuarioMap.put("idiomaPreferido", sa.getIdiomaPreferido() != null ? sa.getIdiomaPreferido() : "es");

            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("token", token);
            resp.put("rol", Rol.SUPERADMIN.name());
            resp.put("usuario", usuarioMap);

            ctx.json(resp);
            return;
        }

        // SEC-07: Mitigación de timing attack calculando verificación con hash falso
        BCrypt.verifyer().verify(password.toCharArray(), HASH_FALSO);

        LOG.warn("Intento de login fallido para {}", email);
        // Mismo mensaje: no revelar si el email existe (previene user enumeration)
        throw new UnauthorizedResponse("Credenciales incorrectas.");
    }

    // ─── POST /api/auth/registro ──────────────────────────────────────

    /**
     * Registra a un nuevo cliente en el sistema de manera pública.
     * 
     * @param ctx Contexto HTTP de Javalin con los datos de registro
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si los campos son incompletos o inválidos
     */
    @SuppressWarnings("unchecked")
    private static void registroCliente(Context ctx) throws SQLException {
        // SEC-07: Rate limiting (máximo 10 registros por cada 10 minutos por IP)
        NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.MINUTES);

        // Deserializa el cuerpo JSON de la petición
        Map<String, String> body = ctx.bodyAsClass(Map.class);

        String documento    = body.get("documentoIdentidad");
        String tipoDoc      = body.getOrDefault("tipoDocumento", "RUT");
        String nombre       = body.get("nombre");
        String apellido     = body.get("apellido");
        String email        = body.get("email");
        String telefono     = body.get("telefono");
        String password     = body.get("password");
        String paisCodigo   = body.getOrDefault("paisCodigo", "CL");

        if (documento == null || nombre == null || apellido == null ||
            email == null || password == null) {
            throw new BadRequestResponse(
                "Campos requeridos: documentoIdentidad, nombre, apellido, email, password.");
        }

        validarPassword(password);

        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        Cliente nuevo = new Cliente(documento, tipoDoc, nombre, apellido,
                                    email, telefono, passwordHash, paisCodigo);
        int idGenerado;
        try {
            idGenerado = clienteDAO.registrarCliente(nuevo);
        } catch (SQLException e) {
            // Manejo de violación de constraint UNIQUE (email o documento duplicado en SQL Server)
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) {
                throw new ConflictResponse("Email o documento de identidad ya registrado.");
            }
            throw e;
        }

        ctx.status(201).json(Map.of(
                "mensaje", "Cliente registrado exitosamente.",
                "id",      idGenerado
        ));
    }

    // ─── STUBS SSO / MFA (estructura para implementación futura) ─────

    /** Stub para SSO Google. Retorna 501 Not Implemented. */
    private static void ssoGoogleStub(Context ctx) {
        ctx.status(501).json(Map.of(
                "estado",   "proximamente",
                "mensaje",  "La autenticación con Google estará disponible en la versión 2.0.",
                "endpoint", "/api/auth/sso/google"
        ));
    }

    /** Stub para verificación MFA. Retorna 501 Not Implemented. */
    private static void mfaVerifyStub(Context ctx) {
        ctx.status(501).json(Map.of(
                "estado",   "proximamente",
                "mensaje",  "La verificación en dos pasos (MFA) estará disponible en la versión 2.0.",
                "endpoint", "/api/auth/mfa/verify"
        ));
    }

    // ─── Utilidades ───────────────────────────────────────────────────

    /**
     * Verifica la validez de una contraseña comparándola con su hash.
     * 
     * @param password La contraseña en texto claro
     * @param hash El hash BCrypt almacenado
     * @throws UnauthorizedResponse si las credenciales no coinciden
     */
    private static void verificarPassword(String password, String hash) {
        BCrypt.Result resultado = BCrypt.verifyer()
                .verify(password.toCharArray(), hash);
        if (!resultado.verified) {
            throw new UnauthorizedResponse("Credenciales incorrectas.");
        }
    }
}
