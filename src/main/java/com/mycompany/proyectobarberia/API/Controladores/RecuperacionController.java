package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.API.Seguridad.JwtUtils;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import com.mycompany.proyectobarberia.Persistencia.DAO.RecuperacionDAO;
import com.mycompany.proyectobarberia.Servicios.EmailService;
import com.mycompany.proyectobarberia.Servicios.EmailServiceFactory;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.util.NaiveRateLimit;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * "¿Olvidaste tu contraseña?" en tres pasos (todos públicos):
 *
 *   1. POST /api/auth/recuperar/solicitar   { email }
 *      → Envía un código de 6 dígitos al correo (válido 15 min).
 *        Responde SIEMPRE lo mismo, exista o no el correo (evita enumerar usuarios).
 *   2. POST /api/auth/recuperar/verificar   { email, codigo }
 *      → Si el código es correcto devuelve un resetToken (válido 10 min).
 *        Máximo 5 intentos por código.
 *   3. POST /api/auth/recuperar/restablecer { resetToken, password }
 *      → Cambia la contraseña y consume el código (un solo uso).
 *
 * Seguridad: el código se guarda como hash BCrypt, se genera con SecureRandom,
 * hay rate limit por IP y un máximo de 3 solicitudes por cuenta cada 15 minutos.
 * El trabajo costoso (hash + envío) se hace en segundo plano para que el tiempo de
 * respuesta no revele si el correo existe.
 */
public class RecuperacionController {

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacionController.class);

    static final int MINUTOS_VALIDEZ_CODIGO = 15;
    static final int MINUTOS_VALIDEZ_TOKEN = 10;
    static final int MAX_INTENTOS = 5;
    static final int MAX_SOLICITUDES_VENTANA = 3;

    static final String MSG_SOLICITUD =
            "Si el correo está registrado, te enviamos un código de 6 dígitos. Revisa también la carpeta de spam.";
    static final String MSG_CODIGO_INVALIDO = "Código inválido o expirado.";
    static final String MSG_TOKEN_INVALIDO =
            "La verificación expiró o ya fue usada. Solicita un nuevo código.";

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CODIGO = Pattern.compile("^\\d{6}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    // Dependencias reemplazables en pruebas
    private static RecuperacionDAO dao = new RecuperacionDAO();
    private static EmailService email;
    private static Executor tareas = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "envio-correos");
        t.setDaemon(true);
        return t;
    });
    private static Clock reloj = Clock.systemDefaultZone();

    private RecuperacionController() { }

    static void configurar(RecuperacionDAO nuevoDao, EmailService nuevoEmail, Executor nuevasTareas, Clock nuevoReloj) {
        dao = nuevoDao;
        email = nuevoEmail;
        tareas = nuevasTareas;
        reloj = nuevoReloj;
    }

    private static synchronized EmailService email() {
        if (email == null) email = EmailServiceFactory.desdeEntorno(Conexion.cargarDotenv());
        return email;
    }

    /** Código numérico de 6 dígitos (admite ceros a la izquierda). */
    static String generarCodigo() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    // ─── 1. Solicitar código ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    static void solicitar(Context ctx) throws SQLException {
        NaiveRateLimit.requestPerTimeUnit(ctx, 5, TimeUnit.MINUTES);
        String correo = normalizarEmail(ctx.bodyAsClass(Map.class));

        RecuperacionDAO.PersonaRecuperable persona = dao.buscarPersonaActiva(correo);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (persona != null
                && dao.contarSolicitudesDesde(persona.id(), ahora.minusMinutes(MINUTOS_VALIDEZ_CODIGO)) < MAX_SOLICITUDES_VENTANA) {
            tareas.execute(() -> generarYEnviar(persona, ahora));
        }
        ctx.json(Map.of("mensaje", MSG_SOLICITUD));
    }

    private static void generarYEnviar(RecuperacionDAO.PersonaRecuperable persona, LocalDateTime ahora) {
        try {
            String codigo = generarCodigo();
            String hash = BCrypt.withDefaults().hashToString(10, codigo.toCharArray());
            dao.crearCodigo(persona.id(), hash, ahora, ahora.plusMinutes(MINUTOS_VALIDEZ_CODIGO));
            email().enviar(persona.email(), "Tu código para restablecer la contraseña",
                    "Hola " + (persona.nombre() != null ? persona.nombre() : "") + ",\n\n"
                    + "Tu código para restablecer la contraseña en Barber System es:\n\n"
                    + "    " + codigo + "\n\n"
                    + "Vence en " + MINUTOS_VALIDEZ_CODIGO + " minutos y solo se puede usar una vez.\n"
                    + "Si no lo solicitaste, ignora este correo: tu contraseña no cambiará.\n\n"
                    + "— Barber System");
        } catch (Exception e) {
            LOG.error("No se pudo generar/enviar el código de recuperación para la persona {}: {}",
                    persona.id(), e.getMessage(), e);
        }
    }

    // ─── 2. Verificar código ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    static void verificar(Context ctx) throws SQLException {
        NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.MINUTES);
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        String correo = normalizarEmail(body);
        Object codigoObj = body.get("codigo");
        String codigo = codigoObj instanceof String c ? c.trim() : null;
        if (codigo == null || !CODIGO.matcher(codigo).matches()) {
            throw new BadRequestResponse("Ingresa el código de 6 dígitos.");
        }

        RecuperacionDAO.PersonaRecuperable persona = dao.buscarPersonaActiva(correo);
        RecuperacionDAO.Codigo vigente = persona == null ? null : dao.buscarVigente(persona.id());
        if (vigente == null
                || LocalDateTime.now(reloj).isAfter(vigente.expira())
                || vigente.intentos() >= MAX_INTENTOS) {
            throw new BadRequestResponse(MSG_CODIGO_INVALIDO);
        }
        if (!BCrypt.verifyer().verify(codigo.toCharArray(), vigente.codigoHash()).verified) {
            dao.registrarIntentoFallido(vigente.id());
            throw new BadRequestResponse(MSG_CODIGO_INVALIDO);
        }

        String token = JwtUtils.generarTokenRecuperacion(persona.id(), vigente.id(), MINUTOS_VALIDEZ_TOKEN);
        ctx.json(Map.of("resetToken", token, "expiraEnMinutos", MINUTOS_VALIDEZ_TOKEN));
    }

    // ─── 3. Restablecer contraseña ────────────────────────────────────

    @SuppressWarnings("unchecked")
    static void restablecer(Context ctx) throws SQLException {
        NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.MINUTES);
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        if (body == null) throw new BadRequestResponse(MSG_TOKEN_INVALIDO);
        Object tokenObj = body.get("resetToken");
        Object passObj = body.get("password");
        if (!(tokenObj instanceof String token) || token.isBlank()) throw new BadRequestResponse(MSG_TOKEN_INVALIDO);
        String password = passObj instanceof String p ? p : null;
        AuthController.validarPassword(password);

        JwtUtils.DatosRecuperacion datos;
        try {
            datos = JwtUtils.validarTokenRecuperacion(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestResponse(MSG_TOKEN_INVALIDO);
        }

        String nuevoHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        if (!dao.restablecerPassword(datos.codigoId(), datos.personaId(), nuevoHash)) {
            throw new BadRequestResponse(MSG_TOKEN_INVALIDO);
        }
        ctx.json(Map.of("mensaje", "Contraseña actualizada. Ya puedes iniciar sesión."));
    }

    // ─── Utilidades ───────────────────────────────────────────────────

    private static String normalizarEmail(Map<String, Object> body) {
        Object valor = body == null ? null : body.get("email");
        String correo = valor instanceof String s ? s.trim() : "";
        if (correo.isEmpty() || correo.length() > 150 || !EMAIL.matcher(correo).matches()) {
            throw new BadRequestResponse("Ingresa un correo electrónico válido.");
        }
        return correo;
    }
}
