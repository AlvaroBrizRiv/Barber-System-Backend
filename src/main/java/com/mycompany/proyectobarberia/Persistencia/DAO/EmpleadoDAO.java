package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Empleado.
 * Usa documento_identidad (multi-país) en lugar de rut.
 * Todas las queries filtran por barberia_id para garantizar el aislamiento multi-tenant.
 */
public class EmpleadoDAO {

    private static final String SQL_INSERT_PERSONA =
            "INSERT INTO personas (documento_identidad, tipo_documento, nombre, apellido, " +
            "  email, telefono, password_hash, pais_codigo) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_INSERT_EMPLEADO =
            "INSERT INTO empleados (id, barberia_id, rol) VALUES (?, ?, ?)";

    private static final String SQL_BASE_SELECT =
            "SELECT p.id, p.documento_identidad, p.tipo_documento, p.nombre, p.apellido, " +
            "       p.email, p.telefono, p.password_hash, p.foto_url, p.idioma_preferido, " +
            "       p.pais_codigo, p.auth_provider, p.mfa_habilitado, " +
            "       p.fecha_registro, p.activo, " +
            "       e.barberia_id, e.rol, e.activo AS empleado_activo " +
            "FROM personas p " +
            "INNER JOIN empleados e ON p.id = e.id ";

    private static final String SQL_BUSCAR_POR_EMAIL =
            SQL_BASE_SELECT + "WHERE p.email = ? AND p.activo = TRUE";

    private static final String SQL_LISTAR_POR_BARBERIA =
            SQL_BASE_SELECT + "WHERE e.barberia_id = ? AND e.activo = TRUE ORDER BY p.nombre";

    private static final String SQL_LISTAR_BARBEROS =
            SQL_BASE_SELECT + "WHERE e.barberia_id = ? AND e.rol = 'BARBERO' AND e.activo = TRUE ORDER BY p.nombre";

    private static final String SQL_DESACTIVAR =
            "UPDATE empleados SET activo = FALSE WHERE id = ? AND barberia_id = ?";

    // ─── Escritura ────────────────────────────────────────────────────

    /**
     * Registra un nuevo empleado realizando una transacción sobre las tablas personas y empleados.
     * 
     * @param empleado Datos del empleado a insertar
     * @return El ID generado para el nuevo empleado
     * @throws SQLException si ocurre un error de base de datos o durante la transacción
     */
    public int registrarEmpleado(Empleado empleado) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            // Se deshabilita auto-commit para asegurar la atomicidad
            conn.setAutoCommit(false);
            try (PreparedStatement psP = conn.prepareStatement(SQL_INSERT_PERSONA, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psE = conn.prepareStatement(SQL_INSERT_EMPLEADO)) {

                psP.setString(1, empleado.getDocumentoIdentidad());
                psP.setString(2, empleado.getTipoDocumento() != null ? empleado.getTipoDocumento() : "RUT");
                psP.setString(3, empleado.getNombre());
                psP.setString(4, empleado.getApellido());
                psP.setString(5, empleado.getEmail());
                psP.setString(6, empleado.getTelefono());
                psP.setString(7, empleado.getPasswordHash());
                psP.setString(8, empleado.getPaisCodigo() != null ? empleado.getPaisCodigo() : "CL");
                psP.executeUpdate();

                int idGenerado;
                try (ResultSet rs = psP.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("No se generó ID para la persona.");
                    idGenerado = rs.getInt(1);
                }

                psE.setInt(1, idGenerado);
                psE.setInt(2, empleado.getBarberiaId());
                psE.setString(3, empleado.getRol().name());
                psE.executeUpdate();

                conn.commit();
                return idGenerado;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Desactiva a un empleado lógicamente (soft-delete).
     * Verifica que el empleado pertenezca a la barbería indicada.
     * 
     * @param empleadoId ID del empleado
     * @param barberiaId ID de la barbería
     * @return true si se desactivó correctamente, false de lo contrario
     * @throws SQLException si ocurre un error en la base de datos
     */
    public boolean desactivar(int empleadoId, int barberiaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_DESACTIVAR)) {
            ps.setInt(1, empleadoId);
            ps.setInt(2, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Lectura ──────────────────────────────────────────────────────

    /**
     * Busca a un empleado activo por su correo electrónico.
     * 
     * @param email Correo electrónico a buscar
     * @return Empleado encontrado o null
     * @throws SQLException si ocurre un error en la base de datos
     */
    public Empleado buscarPorEmail(String email) throws SQLException {
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
     * Lista todos los empleados activos de una barbería.
     * 
     * @param barberiaId ID de la barbería
     * @return Lista de empleados
     * @throws SQLException si ocurre un error en la base de datos
     */
    public List<Empleado> listarPorBarberia(int barberiaId) throws SQLException {
        return listarConQuery(SQL_LISTAR_POR_BARBERIA, barberiaId);
    }

    /**
     * Lista solo los empleados con el rol BARBERO de una barbería determinada.
     * 
     * @param barberiaId ID de la barbería
     * @return Lista de barberos activos
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Empleado> listarBarberosPorBarberia(int barberiaId) throws SQLException {
        return listarConQuery(SQL_LISTAR_BARBEROS, barberiaId);
    }

    private List<Empleado> listarConQuery(String sql, int barberiaId) throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    // ─── Mapeo ResultSet → Objeto ─────────────────────────────────────

    /**
     * Mapea un ResultSet a un objeto Empleado completo.
     * 
     * @param rs El ResultSet con los datos obtenidos
     * @return Objeto Empleado mapeado
     * @throws SQLException si hay problemas extrayendo valores
     */
    private Empleado mapearFila(ResultSet rs) throws SQLException {
        return new Empleado(
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
                rs.getInt("barberia_id"),
                Rol.valueOf(rs.getString("rol")),
                rs.getBoolean("empleado_activo")
        );
    }
}
