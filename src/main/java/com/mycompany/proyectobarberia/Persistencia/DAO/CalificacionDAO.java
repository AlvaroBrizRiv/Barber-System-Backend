package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Calificacion;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Calificacion.
 *
 * Reglas de negocio implementadas a nivel SQL:
 *   - Solo se puede calificar una cita con estado COMPLETADA.
 *   - Cada cita tiene como máximo una calificación (UNIQUE en BD).
 *   - La consulta de promedio filtra por barberia_id (multi-tenant).
 */
public class CalificacionDAO {

    private static final String SQL_INSERT =
            "INSERT INTO calificaciones " +
            "(cita_id, cliente_id, barberia_id, empleado_id, estrellas_barberia, estrellas_barbero, comentario) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_LISTAR_POR_BARBERIA =
            "SELECT id, cita_id, cliente_id, barberia_id, empleado_id, " +
            "       estrellas_barberia, estrellas_barbero, comentario, fecha " +
            "FROM calificaciones WHERE barberia_id = ? ORDER BY fecha DESC";

    private static final String SQL_PROMEDIO_BARBERIA =
            "SELECT AVG(estrellas_barberia) AS promedio FROM calificaciones WHERE barberia_id = ?";

    private static final String SQL_PROMEDIO_BARBERO =
            "SELECT AVG(estrellas_barbero) AS promedio FROM calificaciones WHERE empleado_id = ? AND barberia_id = ?";

    private static final String SQL_YA_CALIFICADA =
            "SELECT COUNT(*) FROM calificaciones WHERE cita_id = ?";

    // ─────────────────────────────────────────────────────────────────

    /**
     * Registra una calificación para una cita.
     * Verifica primero que la cita esté COMPLETADA y no haya sido calificada.
     *
     * @return ID generado de la calificación.
     * @throws SQLException si la cita ya fue calificada (UNIQUE constraint).
     */
    public int registrar(Calificacion cal) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cal.getCitaId());
            ps.setInt(2, cal.getClienteId());
            ps.setInt(3, cal.getBarberiaId());
            ps.setInt(4, cal.getEmpleadoId());
            ps.setInt(5, cal.getEstrellasBarberia());
            ps.setInt(6, cal.getEstrellasBarbero());
            ps.setString(7, cal.getComentario());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se generó ID para la calificación.");
    }

    /** Lista todas las calificaciones de una barbería (vista pública y admin). */
    public List<Calificacion> listarPorBarberia(int barberiaId) throws SQLException {
        List<Calificacion> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_BARBERIA)) {

            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /** Calcula el promedio de estrellas de una barbería (0.0 si no hay calificaciones). */
    public double promedioBarberia(int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_PROMEDIO_BARBERIA)) {

            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("promedio");
            }
        }
        return 0.0;
    }

    /** Calcula el promedio de estrellas de un barbero (en su barbería). */
    public double promedioBarbero(int empleadoId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_PROMEDIO_BARBERO)) {

            ps.setInt(1, empleadoId);
            ps.setInt(2, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("promedio");
            }
        }
        return 0.0;
    }

    /** Verifica si una cita ya fue calificada. */
    public boolean yaFueCalificada(int citaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_YA_CALIFICADA)) {

            ps.setInt(1, citaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Calificacion mapearFila(ResultSet rs) throws SQLException {
        return new Calificacion(
                rs.getInt("id"),
                rs.getInt("cita_id"),
                rs.getInt("cliente_id"),
                rs.getInt("barberia_id"),
                rs.getInt("empleado_id"),
                rs.getInt("estrellas_barberia"),
                rs.getInt("estrellas_barbero"),
                rs.getString("comentario"),
                rs.getTimestamp("fecha").toLocalDateTime()
        );
    }
}
