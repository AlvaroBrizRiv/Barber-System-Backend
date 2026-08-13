package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Barberia;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Barberia.
 *
 * Operaciones: listar (público), buscar por ID, crear, actualizar, desactivar.
 * El SUPERADMIN puede ver todas; el ADMIN_BARBERIA solo ve la suya (control en Controller).
 */
public class BarberiaDAO {

    private static final String SQL_SELECT_TODAS =
            "SELECT id, nombre, direccion, telefono, email, logo_url, descripcion, activa, fecha_creacion " +
            "FROM barberias WHERE activa = TRUE ORDER BY nombre";

    private static final String SQL_SELECT_POR_ID =
            "SELECT id, nombre, direccion, telefono, email, logo_url, descripcion, activa, fecha_creacion " +
            "FROM barberias WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO barberias (nombre, direccion, telefono, email, logo_url, descripcion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE barberias SET nombre=?, direccion=?, telefono=?, email=?, logo_url=?, descripcion=? " +
            "WHERE id=?";

    private static final String SQL_DESACTIVAR =
            "UPDATE barberias SET activa=FALSE WHERE id=?";

    // ─────────────────────────────────────────────────────────────────

    /**
     * Lista todas las barberías activas (endpoint público del catálogo).
     * 
     * @return Lista de objetos Barberia que están activas
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<Barberia> listarActivas() throws SQLException {
        List<Barberia> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TODAS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Busca una barbería por su ID. Retorna null si no existe.
     * 
     * @param id ID de la barbería a buscar
     * @return Objeto Barberia o null si no se encuentra
     * @throws SQLException si ocurre un error en la consulta
     */
    public Barberia buscarPorId(int id) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_POR_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearFila(rs);
            }
        }
        return null;
    }

    /**
     * Crea una nueva barbería. Solo el SUPERADMIN puede llamar esto.
     * 
     * @param barberia Datos de la nueva barbería (sin ID).
     * @return ID generado por la base de datos.
     * @throws SQLException si ocurre un error durante la inserción
     */
    public int crear(Barberia barberia) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, barberia.getNombre());
            ps.setString(2, barberia.getDireccion());
            ps.setString(3, barberia.getTelefono());
            ps.setString(4, barberia.getEmail());
            ps.setString(5, barberia.getLogoUrl());
            ps.setString(6, barberia.getDescripcion());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID generado para la barbería.");
    }

    /**
     * Actualiza los datos de una barbería. El controller verifica que el
     * admin solo pueda actualizar SU barbería (validando barberia_id del JWT).
     * 
     * @param barberia Objeto Barberia con los datos actualizados
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException si ocurre un error durante la actualización
     */
    public boolean actualizar(Barberia barberia) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, barberia.getNombre());
            ps.setString(2, barberia.getDireccion());
            ps.setString(3, barberia.getTelefono());
            ps.setString(4, barberia.getEmail());
            ps.setString(5, barberia.getLogoUrl());
            ps.setString(6, barberia.getDescripcion());
            ps.setInt(7, barberia.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Desactiva una barbería (soft-delete, no elimina el historial).
     * 
     * @param id ID de la barbería a desactivar
     * @return true si la desactivación fue exitosa, false en caso contrario
     * @throws SQLException si ocurre un error durante la ejecución
     */
    public boolean desactivar(int id) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DESACTIVAR)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Mapeo de ResultSet → Objeto ─────────────────────────────────

    /**
     * Mapea el ResultSet actual a un objeto Barberia.
     * 
     * @param rs ResultSet con el cursor en la fila actual
     * @return Objeto Barberia populado
     * @throws SQLException si ocurre un error al leer los campos
     */
    private Barberia mapearFila(ResultSet rs) throws SQLException {
        return new Barberia(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("logo_url"),
                rs.getString("descripcion"),
                rs.getBoolean("activa"),
                rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null
        );
    }
}
