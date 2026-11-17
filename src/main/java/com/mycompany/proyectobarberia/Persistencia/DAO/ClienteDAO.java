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
            "WHERE p.email = ? AND p.activo = 1";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, " +
            "       p.fecha_registro, p.activo, c.puntos_fidelidad " +
            "FROM personas p " +
            "INNER JOIN clientes c ON p.id = c.id " +
            "WHERE p.id = ? AND p.activo = 1";

    private static final String SQL_ACTUALIZAR_PERFIL =
            "UPDATE personas SET nombre = COALESCE(?, nombre), apellido = COALESCE(?, apellido), " + 
            "telefono = COALESCE(?, telefono), foto_url = COALESCE(?, foto_url)," + 
            "idioma_preferido = COALESCE(?, idioma_preferido) WHERE id = ?";

    // ─── Eliminación de cuenta (anonimización) ────────────────────────
    // Las citas y calificaciones se conservan para la contabilidad de la barbería,
    // pero los datos personales se reemplazan para que no sean identificables y el
    // email/documento queden libres para un registro futuro.
    private static final String SQL_CANCELAR_CITAS_FUTURAS =
            "UPDATE citas SET estado = 'CANCELADA' WHERE cliente_id = ? AND fecha >= ? " +
            "AND estado IN ('PENDIENTE','CONFIRMADA')";
    private static final String SQL_VACIAR_CARRITO =
            "DELETE FROM carritos_items WHERE cliente_id = ?";
    private static final String SQL_DESACTIVAR_SUSCRIPCIONES =
            "UPDATE cliente_suscripciones SET activa = 0 WHERE cliente_id = ?";
    private static final String SQL_ANONIMIZAR_PERSONA =
            "UPDATE personas SET activo = 0, nombre = 'Usuario', apellido = 'Eliminado', " +
            "email = ?, documento_identidad = ?, telefono = NULL, foto_url = NULL, password_hash = '!' " +
            "WHERE id = ? AND activo = 1";

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
     * Elimina la cuenta de un cliente de forma atómica:
     * cancela sus citas futuras, vacía su carrito, desactiva sus suscripciones
     * y anonimiza sus datos personales (soft-delete con borrado de PII).
     *
     * @param id  ID del cliente.
     * @param hoy Fecha actual (se pasa desde Java para no depender de funciones de fecha del motor).
     * @return true si la cuenta existía y fue eliminada.
     * @throws SQLException si falla alguna operación (se hace rollback completo).
     */
    public boolean eliminarCuenta(int id, java.time.LocalDate hoy) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psCitas = conn.prepareStatement(SQL_CANCELAR_CITAS_FUTURAS);
                 PreparedStatement psCarrito = conn.prepareStatement(SQL_VACIAR_CARRITO);
                 PreparedStatement psSubs = conn.prepareStatement(SQL_DESACTIVAR_SUSCRIPCIONES);
                 PreparedStatement psPersona = conn.prepareStatement(SQL_ANONIMIZAR_PERSONA)) {

                psPersona.setString(1, "eliminado+" + id + "@cuenta.invalid");
                psPersona.setString(2, "DEL-" + id);
                psPersona.setInt(3, id);
                if (psPersona.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }

                psCitas.setInt(1, id);
                psCitas.setDate(2, java.sql.Date.valueOf(hoy));
                psCitas.executeUpdate();

                psCarrito.setInt(1, id);
                psCarrito.executeUpdate();

                psSubs.setInt(1, id);
                psSubs.executeUpdate();

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
            ps.setString(1, campos.get("nombre"));
            ps.setString(2, campos.get("apellido"));
            ps.setString(3, campos.get("telefono"));
            ps.setString(4, campos.get("fotoUrl"));
            ps.setString(5, campos.get("idiomaPreferido"));
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
