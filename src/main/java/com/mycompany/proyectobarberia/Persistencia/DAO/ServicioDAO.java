package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Servicio;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Servicio.
 *
 * Cada barbería define sus propios servicios (requisito del cliente).
 * TODAS las operaciones filtran por barberia_id para garantizar aislamiento.
 */
public class ServicioDAO {

    private static final String SQL_LISTAR_POR_BARBERIA =
            "SELECT id, barberia_id, nombre, descripcion, precio, duracion_minutos, activo " +
            "FROM servicios WHERE barberia_id = ? AND activo = TRUE ORDER BY nombre";

    private static final String SQL_INSERT =
            "INSERT INTO servicios (barberia_id, nombre, descripcion, precio, duracion_minutos) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE servicios SET nombre=?, descripcion=?, precio=?, duracion_minutos=? " +
            "WHERE id=? AND barberia_id=?";

    private static final String SQL_DESACTIVAR =
            "UPDATE servicios SET activo=FALSE WHERE id=? AND barberia_id=?";

    // ─────────────────────────────────────────────────────────────────

    /** Lista todos los servicios activos de una barbería (endpoint público). */
    public List<Servicio> listarPorBarberia(int barberiaId) throws SQLException {
        List<Servicio> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_BARBERIA)) {

            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /** Crea un nuevo servicio. El barberia_id viene del JWT del admin. */
    public int crear(Servicio servicio) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, servicio.getBarberiaId());
            ps.setString(2, servicio.getNombre());
            ps.setString(3, servicio.getDescripcion());
            ps.setDouble(4, servicio.getPrecio());
            ps.setInt(5, servicio.getDuracionMinutos());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se generó ID para el servicio.");
    }

    /**
     * Actualiza un servicio. El barberia_id en el WHERE garantiza que solo
     * el admin de ESA barbería puede modificar sus servicios.
     */
    public boolean actualizar(Servicio servicio) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, servicio.getNombre());
            ps.setString(2, servicio.getDescripcion());
            ps.setDouble(3, servicio.getPrecio());
            ps.setInt(4, servicio.getDuracionMinutos());
            ps.setInt(5, servicio.getId());
            ps.setInt(6, servicio.getBarberiaId()); // Garantía multi-tenant
            return ps.executeUpdate() > 0;
        }
    }

    /** Desactiva un servicio. La doble verificación (id + barberia_id) es la garantía. */
    public boolean desactivar(int servicioId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DESACTIVAR)) {

            ps.setInt(1, servicioId);
            ps.setInt(2, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    private Servicio mapearFila(ResultSet rs) throws SQLException {
        return new Servicio(
                rs.getInt("id"),
                rs.getInt("barberia_id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getDouble("precio"),
                rs.getInt("duracion_minutos"),
                rs.getBoolean("activo")
        );
    }
}
