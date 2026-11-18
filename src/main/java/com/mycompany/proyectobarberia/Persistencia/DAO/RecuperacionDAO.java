package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Acceso a datos para la recuperación de contraseña.
 *
 * Solo se almacena el hash BCrypt del código. Las fechas se calculan en Java
 * (no con funciones del motor) para que el SQL sea idéntico en SQL Server y PostgreSQL.
 */
public class RecuperacionDAO {

    /** Persona activa que puede recuperar su contraseña. */
    public record PersonaRecuperable(int id, String nombre, String email) { }

    /** Código de recuperación vigente (no usado). */
    public record Codigo(int id, int personaId, String codigoHash, LocalDateTime expira, int intentos) { }

    private static final String SQL_PERSONA =
            "SELECT id, nombre, email FROM personas WHERE email = ? AND activo = 1";

    private static final String SQL_INVALIDAR_ANTERIORES =
            "UPDATE recuperaciones_password SET usado = 1 WHERE persona_id = ? AND usado = 0";

    private static final String SQL_INSERTAR =
            "INSERT INTO recuperaciones_password (persona_id, codigo_hash, expira, intentos, usado, fecha_creacion) " +
            "VALUES (?, ?, ?, 0, 0, ?)";

    private static final String SQL_CONTAR_DESDE =
            "SELECT COUNT(*) FROM recuperaciones_password WHERE persona_id = ? AND fecha_creacion >= ?";

    private static final String SQL_VIGENTE =
            "SELECT id, persona_id, codigo_hash, expira, intentos FROM recuperaciones_password " +
            "WHERE persona_id = ? AND usado = 0 ORDER BY id DESC";

    private static final String SQL_INTENTO_FALLIDO =
            "UPDATE recuperaciones_password SET intentos = intentos + 1 WHERE id = ?";

    private static final String SQL_CONSUMIR =
            "UPDATE recuperaciones_password SET usado = 1 WHERE id = ? AND persona_id = ? AND usado = 0";

    private static final String SQL_CAMBIAR_PASSWORD =
            "UPDATE personas SET password_hash = ? WHERE id = ? AND activo = 1";

    /**
     * Busca una persona activa por email (clientes, empleados y superadmins).
     *
     * @param email Correo ingresado.
     * @return La persona o null si no existe o su cuenta fue eliminada.
     * @throws SQLException si falla la consulta
     */
    public PersonaRecuperable buscarPersonaActiva(String email) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_PERSONA)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PersonaRecuperable(rs.getInt("id"), rs.getString("nombre"), rs.getString("email"));
                }
            }
        }
        return null;
    }

    /**
     * Cuántos códigos se pidieron para la persona desde una fecha (límite anti-abuso).
     *
     * @param personaId ID de la persona.
     * @param desde     Inicio de la ventana de tiempo.
     * @return Número de solicitudes.
     * @throws SQLException si falla la consulta
     */
    public int contarSolicitudesDesde(int personaId, LocalDateTime desde) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CONTAR_DESDE)) {
            ps.setInt(1, personaId);
            ps.setTimestamp(2, Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Invalida los códigos anteriores de la persona y guarda uno nuevo (transacción).
     *
     * @param personaId  ID de la persona.
     * @param codigoHash Hash BCrypt del código.
     * @param ahora      Momento de creación.
     * @param expira     Momento de expiración.
     * @return ID del código creado.
     * @throws SQLException si falla alguna operación (se hace rollback)
     */
    public int crearCodigo(int personaId, String codigoHash, LocalDateTime ahora, LocalDateTime expira) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psInv = conn.prepareStatement(SQL_INVALIDAR_ANTERIORES);
                 PreparedStatement psIns = conn.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
                psInv.setInt(1, personaId);
                psInv.executeUpdate();

                psIns.setInt(1, personaId);
                psIns.setString(2, codigoHash);
                psIns.setTimestamp(3, Timestamp.valueOf(expira));
                psIns.setTimestamp(4, Timestamp.valueOf(ahora));
                psIns.executeUpdate();

                int id;
                try (ResultSet rs = psIns.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("No se generó ID para el código de recuperación.");
                    id = rs.getInt(1);
                }
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Devuelve el código más reciente no usado de la persona.
     *
     * @param personaId ID de la persona.
     * @return El código o null si no hay ninguno pendiente.
     * @throws SQLException si falla la consulta
     */
    public Codigo buscarVigente(int personaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_VIGENTE)) {
            ps.setInt(1, personaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Codigo(rs.getInt("id"), rs.getInt("persona_id"), rs.getString("codigo_hash"),
                            rs.getTimestamp("expira").toLocalDateTime(), rs.getInt("intentos"));
                }
            }
        }
        return null;
    }

    /**
     * Suma un intento fallido al código.
     *
     * @param codigoId ID del código.
     * @throws SQLException si falla la actualización
     */
    public void registrarIntentoFallido(int codigoId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INTENTO_FALLIDO)) {
            ps.setInt(1, codigoId);
            ps.executeUpdate();
        }
    }

    /**
     * Consume el código y cambia la contraseña en una sola transacción.
     * Si el código ya fue usado o reemplazado por uno nuevo, no cambia nada.
     *
     * @param codigoId  ID del código validado.
     * @param personaId Dueño del código.
     * @param nuevoHash Hash BCrypt de la nueva contraseña.
     * @return true si la contraseña se cambió.
     * @throws SQLException si falla alguna operación (se hace rollback)
     */
    public boolean restablecerPassword(int codigoId, int personaId, String nuevoHash) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCod = conn.prepareStatement(SQL_CONSUMIR);
                 PreparedStatement psPwd = conn.prepareStatement(SQL_CAMBIAR_PASSWORD)) {
                psCod.setInt(1, codigoId);
                psCod.setInt(2, personaId);
                if (psCod.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                psPwd.setString(1, nuevoHash);
                psPwd.setInt(2, personaId);
                if (psPwd.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
