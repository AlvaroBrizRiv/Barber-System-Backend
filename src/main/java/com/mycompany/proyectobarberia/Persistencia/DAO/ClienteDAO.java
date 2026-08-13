package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.Map;

/**
 * Acceso a datos para la entidad Cliente.
 * Usa documento_identidad (multi-país) en lugar de rut.
 */
public class ClienteDAO {

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, " +
            "  email, telefono, password_hash, pais_codigo) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_CLIENTE =
            "INSERT INTO clientes (id, puntos_fidelidad) VALUES (?, 0)";

    private static final String SQL_BUSCAR_POR_EMAIL =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, " +
            "       p.fecha_registro, p.activo, c.puntos_fidelidad " +
            "FROM personas p " +
            "INNER JOIN clientes c ON p.id = c.id " +
            "WHERE p.email = ? AND p.activo = TRUE";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, " +
            "       p.fecha_registro, p.activo, c.puntos_fidelidad " +
            "FROM personas p " +
            "INNER JOIN clientes c ON p.id = c.id " +
            "WHERE p.id = ?";

    private static final String SQL_ACTUALIZAR_PERFIL =
            "UPDATE personas SET nombre = ?, apellido = ?, telefono = ?, " +
            "  foto_url = ?, idioma_preferido = ? WHERE id = ?";

    private static final String SQL_ACTUALIZAR_PUNTOS =
            "UPDATE clientes SET puntos_fidelidad = ? WHERE id = ?";

    // ─── Escritura ────────────────────────────────────────────────────

    /**
     * Registra un nuevo cliente en la base de datos.
     * Realiza una transacción atómica insertando en 'personas' y luego en 'clientes'.
     * 
     * @param cliente El objeto Cliente con los datos a registrar
     * @return El ID generado para el nuevo cliente
     * @throws SQLException si ocurre un error durante las inserciones o manejo transaccional
     */
    public int registrarCliente(Cliente cliente) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            // Desactiva el auto commit para agrupar las inserciones en una transacción
            conn.setAutoCommit(false);
            try (PreparedStatement psP = conn.prepareStatement(SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psC = conn.prepareStatement(SQL_INSERT_CLIENTE)) {

                psP.setString(1, cliente.getDocumentoIdentidad());
                psP.setString(2, cliente.getTipoDocumento() != null ? cliente.getTipoDocumento() : "RUT");
                psP.setString(3, cliente.getNombre());
                psP.setString(4, cliente.getApellido());
                psP.setString(5, cliente.getEmail());
                psP.setString(6, cliente.getTelefono());
                psP.setString(7, cliente.getPasswordHash());
                psP.setString(8, cliente.getPaisCodigo() != null ? cliente.getPaisCodigo() : "CL");
                psP.executeUpdate();

                int idGenerado;
                try (ResultSet rs = psP.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("No se generó ID para la persona.");
                    idGenerado = rs.getInt(1);
                }

                psC.setInt(1, idGenerado);
                psC.executeUpdate();

                conn.commit();
                return idGenerado;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza datos de perfil básicos del cliente (nombre, teléfono, foto, idioma).
     * No actualiza email ni contraseña por este método (operaciones separadas con más validación).
     * 
     * @param id Identificador del cliente a actualizar
     * @param campos Mapa con los campos a actualizar (nombre, apellido, telefono, fotoUrl, idiomaPreferido)
     * @return true si la actualización afectó al menos una fila, false en caso contrario
     * @throws SQLException si ocurre un error en la base de datos
     */
    public boolean actualizarPerfil(int id, Map<String, String> campos) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_PERFIL)) {
            ps.setString(1, campos.getOrDefault("nombre", ""));
            ps.setString(2, campos.getOrDefault("apellido", ""));
            ps.setString(3, campos.getOrDefault("telefono", ""));
            ps.setString(4, campos.getOrDefault("fotoUrl", null));
            ps.setString(5, campos.getOrDefault("idiomaPreferido", "es"));
            ps.setInt(6, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Lectura ──────────────────────────────────────────────────────

    /**
     * Busca un cliente en la base de datos mediante su email.
     * 
     * @param email Correo electrónico a buscar
     * @return Cliente si es encontrado, null en caso contrario
     * @throws SQLException si ocurre un error en la consulta
     */
    public Cliente buscarPorEmail(String email) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearFila(rs);
            }
        }
        return null;
    }

    /**
     * Busca un cliente en la base de datos por su ID.
     * 
     * @param id ID del cliente a buscar
     * @return Cliente si es encontrado, null en caso contrario
     * @throws SQLException si ocurre un error en la consulta
     */
    public Cliente buscarPorId(int id) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearFila(rs);
            }
        }
        return null;
    }

    /**
     * Actualiza la cantidad de puntos de fidelidad de un cliente.
     * 
     * @param clienteId ID del cliente
     * @param nuevosPuntos Nueva cantidad de puntos
     * @return true si se actualizó correctamente, false en caso contrario
     * @throws SQLException si ocurre un error en la consulta
     */
    public boolean actualizarPuntos(int clienteId, int nuevosPuntos) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_PUNTOS)) {
            ps.setInt(1, nuevosPuntos);
            ps.setInt(2, clienteId);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Mapeo ResultSet → Objeto ─────────────────────────────────────

    /**
     * Mapea una fila del ResultSet hacia un objeto Cliente completo.
     * 
     * @param rs El ResultSet que contiene los datos de la fila
     * @return Un objeto Cliente poblado con los datos
     * @throws SQLException si ocurre un error extrayendo datos del ResultSet
     */
    private Cliente mapearFila(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id"),
                rs.getString("documento_identidad"),
                rs.getString("tipo_documento"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("telefono"),
                rs.getString("password_hash"),
                rs.getString("foto_url"),
                rs.getString("idioma_preferido"),
                rs.getString("pais_codigo"),
                rs.getString("auth_provider"),
                rs.getBoolean("mfa_habilitado"),
                rs.getTimestamp("fecha_registro").toLocalDateTime(),
                rs.getBoolean("activo"),
                rs.getInt("puntos_fidelidad")
        );
    }
}