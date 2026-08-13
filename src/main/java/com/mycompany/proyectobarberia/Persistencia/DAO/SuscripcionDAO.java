package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Suscripcion;
import com.mycompany.proyectobarberia.Logica.enums.TipoSuscripcion;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Suscripcion.
 *
 * El administrador de cada barbería gestiona sus propios planes.
 * Todas las operaciones filtran por barberia_id.
 */
public class SuscripcionDAO {

    private static final String SQL_LISTAR_POR_BARBERIA =
            "SELECT id, barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion, activa " +
            "FROM suscripciones WHERE barberia_id = ? ORDER BY tipo, nombre";

    private static final String SQL_INSERT =
            "INSERT INTO suscripciones (barberia_id, nombre, tipo, precio_por_cita, precio_mensual, descripcion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE suscripciones SET nombre=?, tipo=?, precio_por_cita=?, precio_mensual=?, descripcion=? " +
            "WHERE id=? AND barberia_id=?";

    private static final String SQL_TOGGLE_ACTIVA =
            "UPDATE suscripciones SET activa = NOT activa WHERE id=? AND barberia_id=?";

    // ─────────────────────────────────────────────────────────────────

    /** Lista todos los planes de suscripción de una barbería. */
    public List<Suscripcion> listarPorBarberia(int barberiaId) throws SQLException {
        List<Suscripcion> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_BARBERIA)) {

            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /** Crea un nuevo plan de suscripción para la barbería. */
    public int crear(Suscripcion suscripcion) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, suscripcion.getBarberiaId());
            ps.setString(2, suscripcion.getNombre());
            ps.setString(3, suscripcion.getTipo().name());
            setNullableDouble(ps, 4, suscripcion.getPrecioPorCita());
            setNullableDouble(ps, 5, suscripcion.getPrecioMensual());
            ps.setString(6, suscripcion.getDescripcion());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se generó ID para la suscripción.");
    }

    /** Actualiza un plan. El barberia_id garantiza aislamiento. */
    public boolean actualizar(Suscripcion suscripcion) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, suscripcion.getNombre());
            ps.setString(2, suscripcion.getTipo().name());
            setNullableDouble(ps, 3, suscripcion.getPrecioPorCita());
            setNullableDouble(ps, 4, suscripcion.getPrecioMensual());
            ps.setString(5, suscripcion.getDescripcion());
            ps.setInt(6, suscripcion.getId());
            ps.setInt(7, suscripcion.getBarberiaId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Activa o desactiva un plan (toggle). */
    public boolean toggleActiva(int suscripcionId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_TOGGLE_ACTIVA)) {

            ps.setInt(1, suscripcionId);
            ps.setInt(2, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Maneja valores Double nullable para setDouble en PreparedStatement. */
    private void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DECIMAL);
        }
    }

    private Suscripcion mapearFila(ResultSet rs) throws SQLException {
        Double precioCita = rs.getObject("precio_por_cita") != null
                ? rs.getDouble("precio_por_cita") : null;
        Double precioMes  = rs.getObject("precio_mensual") != null
                ? rs.getDouble("precio_mensual") : null;

        return new Suscripcion(
                rs.getInt("id"),
                rs.getInt("barberia_id"),
                rs.getString("nombre"),
                TipoSuscripcion.valueOf(rs.getString("tipo")),
                precioCita,
                precioMes,
                rs.getString("descripcion"),
                rs.getBoolean("activa")
        );
    }
}
