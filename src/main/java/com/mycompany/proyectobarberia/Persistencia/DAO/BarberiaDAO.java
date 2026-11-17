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

    private static final String COLUMNAS =
            "SELECT id, nombre, direccion, telefono, email, logo_url, descripcion, activa, " +
            "       pais_codigo, moneda_codigo, zona_horaria, suscripciones_habilitadas, fecha_creacion " +
            "FROM barberias ";

    private static final String SQL_SELECT_TODAS =
            COLUMNAS + "WHERE activa = 1 ORDER BY nombre";

    /** Búsqueda insensible a mayúsculas (LOWER + LIKE funciona igual en SQL Server y PostgreSQL). */
    private static final String SQL_BUSCAR =
            COLUMNAS + "WHERE activa = 1 AND (LOWER(nombre) LIKE ? ESCAPE '!' OR LOWER(direccion) LIKE ? ESCAPE '!') " +
            "ORDER BY nombre";

    private static final String SQL_SELECT_POR_ID =
            COLUMNAS + "WHERE id = ?";

    private static final String SQL_SUSCRIPCIONES_HABILITADAS =
            "UPDATE barberias SET suscripciones_habilitadas = ? WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO barberias (nombre, direccion, telefono, email, logo_url, descripcion) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE barberias SET nombre=?, direccion=?, telefono=?, email=?, logo_url=?, descripcion=? " +
            "WHERE id=?";

    private static final String SQL_DESACTIVAR =
            "UPDATE barberias SET activa = 0 WHERE id=?";

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
     * Busca barberías activas cuyo nombre o dirección contenga el texto.
     * Si el texto está vacío equivale a {@link #listarActivas()}.
     *
     * @param texto Término de búsqueda ingresado por el usuario.
     * @return Barberías activas que coinciden.
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<Barberia> buscarActivas(String texto) throws SQLException {
        if (texto == null || texto.isBlank()) return listarActivas();
        String patron = patronLike(texto);
        List<Barberia> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Construye un patrón LIKE "%texto%" en minúsculas escapando los comodines
     * (%, _, [) para que el usuario no pueda inyectar patrones.
     *
     * @param texto Texto libre del usuario.
     * @return Patrón seguro para usar con ESCAPE '!'.
     */
    static String patronLike(String texto) {
        String limpio = texto.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
        return "%" + limpio + "%";
    }

    /**
     * Activa o desactiva la sección de suscripciones de una barbería.
     *
     * @param id          ID de la barbería.
     * @param habilitadas true para mostrar la sección en la página pública.
     * @return true si la barbería existe y se actualizó.
     * @throws SQLException si ocurre un error en la actualización
     */
    public boolean actualizarSuscripcionesHabilitadas(int id, boolean habilitadas) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_SUSCRIPCIONES_HABILITADAS)) {
            // setInt 1/0 en vez de setBoolean: compatible con BIT (SQL Server) y SMALLINT (PostgreSQL)
            ps.setInt(1, habilitadas ? 1 : 0);
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
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
        Barberia b = new Barberia(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("logo_url"),
                rs.getString("descripcion"),
                rs.getBoolean("activa"),
                rs.getString("pais_codigo"),
                rs.getString("moneda_codigo"),
                rs.getString("zona_horaria"),
                rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null
        );
        b.setSuscripcionesHabilitadas(rs.getInt("suscripciones_habilitadas") == 1);
        return b;
    }
}
