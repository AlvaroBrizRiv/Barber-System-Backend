package com.mycompany.proyectobarberia.API.Seguridad;

import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Utilidades para generación y validación de JSON Web Tokens (JWT).
 *
 * El token incluye los siguientes claims:
 *   - sub:        ID del usuario (persona.id) como String.
 *   - rol:        Nombre del enum Rol (CLIENTE, BARBERO, ADMIN_BARBERIA, SUPERADMIN).
 *   - barberiaId: ID de la barbería del usuario. NULL para CLIENTE y SUPERADMIN.
 *   - exp:        Fecha de expiración (24 horas por defecto).
 *
 * SEGURIDAD:
 *   - La clave secreta se lee del archivo .env (nunca hardcodeada).
 *   - Se usa HMAC-SHA256 para la firma.
 *   - Los tokens expirados son rechazados automáticamente por JJWT.
 *
 * Usa la API de JJWT 0.12.x (sintaxis actualizada).
 */
public class JwtUtils {

    private static final SecretKey SECRET_KEY;
    private static final long EXPIRATION_MS;

    static {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();

        String secret = env.get("JWT_SECRET",
                "cambiar_esta_clave_en_produccion_minimo_32_caracteres");

        // Clave mínima de 32 bytes para HMAC-SHA256
        if (secret.length() < 32) {
            throw new ExceptionInInitializerError(
                    "JWT_SECRET debe tener al menos 32 caracteres. Revisa el archivo .env");
        }

        SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        EXPIRATION_MS = Long.parseLong(env.get("JWT_EXPIRATION_MS", "86400000")); // 24h
    }

    // ─── Generación ───────────────────────────────────────────────────

    /**
     * Genera un token JWT para un usuario autenticado.
     *
     * @param userId     ID del usuario en la tabla personas.
     * @param rol        Rol del usuario en el sistema.
     * @param barberiaId ID de la barbería (null para CLIENTE y SUPERADMIN).
     * @return Token JWT firmado y listo para enviar al cliente.
     */
    public static String generarToken(int userId, Rol rol, Integer barberiaId) {
        // Construye y firma el token con los claims necesarios
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("rol", rol.name())
                .claim("barberiaId", barberiaId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(SECRET_KEY)
                .compact();
    }

    // ─── Validación ───────────────────────────────────────────────────

    /**
     * Valida un token JWT y retorna sus claims si es válido.
     *
     * @param token El token JWT (sin el prefijo "Bearer ").
     * @return Claims del token si es válido y no ha expirado.
     * @throws io.jsonwebtoken.JwtException si el token es inválido o expirado.
     */
    public static Claims validarToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ─── Extracción de claims ─────────────────────────────────────────

    /** 
     * Extrae el ID del usuario del token ya validado.
     * 
     * @param claims Los claims extraídos del token
     * @return El ID del usuario parseado a entero
     */
    public static int getUserId(Claims claims) {
        return Integer.parseInt(claims.getSubject());
    }

    /** 
     * Extrae el Rol del token ya validado.
     * 
     * @param claims Los claims extraídos del token
     * @return El enum Rol correspondiente
     */
    public static Rol getRol(Claims claims) {
        return Rol.valueOf(claims.get("rol", String.class));
    }

    /**
     * Extrae el barberiaId del token. Puede ser null para CLIENTE/SUPERADMIN.
     * Siempre usa este método para obtener la barbería del usuario autenticado,
     * NUNCA confíes en el barberiaId del cuerpo del request.
     * 
     * @param claims Los claims extraídos del token
     * @return El ID de la barbería, o null si no aplica
     */
    public static Integer getBarberiaId(Claims claims) {
        Object val = claims.get("barberiaId");
        if (val == null) return null;
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Constructor privado: clase de utilidad estática
    private JwtUtils() {}
}
