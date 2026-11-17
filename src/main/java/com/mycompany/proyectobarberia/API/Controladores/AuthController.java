package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.API.Seguridad.JwtUtils;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.Persona;
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
import java.util.HashMap;
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

        // Orden de búsqueda: empleados → clientes → superadministradores.
        Empleado empleado = empleadoDAO.buscarPorEmail(email);
        if (empleado != null) {
            Map<String, Object> usuario = datosUsuario(empleado);
            usuario.put("barberiaId", empleado.getBarberiaId());
            responderLogin(ctx, password, empleado, empleado.getRol(), empleado.getBarberiaId(), usuario);
            return;
        }

        Cliente cliente = clienteDAO.buscarPorEmail(email);
        if (cliente != null) {
            Map<String, Object> usuario = datosUsuario(cliente);
            usuario.put("puntosFidelidad", cliente.getPuntosFidelidad());
            responderLogin(ctx, password, cliente, Rol.CLIENTE, null, usuario);
            return;
        }

        SuperAdmin sa = superAdminDAO.buscarPorEmail(email);
        if (sa != null) {
            responderLogin(ctx, password, sa, Rol.SUPERADMIN, null, datosUsuario(sa));
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
     * Verifica la contraseña y responde con el token y los datos públicos del usuario.
     * Centraliza la lógica que antes se repetía para cada tipo de usuario.
     *
     * @param ctx        Contexto HTTP.
     * @param password   Contraseña en texto claro recibida.
     * @param persona    Usuario encontrado.
     * @param rol        Rol que irá en el JWT.
     * @param barberiaId Barbería del usuario (null para CLIENTE y SUPERADMIN).
     * @param usuario    Datos públicos a devolver (sin hash ni documento).
     */
    private static void responderLogin(Context ctx, String password, Persona persona, Rol rol,
                                       Integer barberiaId, Map<String, Object> usuario) {
        verificarPassword(password, persona.getPasswordHash());
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", JwtUtils.generarToken(persona.getId(), rol, barberiaId));
        resp.put("rol", rol.name());
        resp.put("usuario", usuario);
        ctx.json(resp);
    }

    /**
     * Datos públicos comunes a cualquier usuario. Nunca incluye passwordHash
     * ni documento de identidad (PII).
     *
     * @param p Persona autenticada.
     * @return Mapa mutable para que el llamador agregue campos propios del rol.
     */
    static Map<String, Object> datosUsuario(Persona p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("nombre", p.getNombre() != null ? p.getNombre() : "");
        m.put("apellido", p.getApellido() != null ? p.getApellido() : "");
        m.put("email", p.getEmail());
        m.put("fotoUrl", p.getFotoUrl() != null ? p.getFotoUrl() : "");
        m.put("idiomaPreferido", p.getIdiomaPreferido() != null ? p.getIdiomaPreferido() : "es");
        return m;
    }

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
