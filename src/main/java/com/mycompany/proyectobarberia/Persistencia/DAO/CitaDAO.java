package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Cita;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.enums.EstadoCita;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos para la entidad Cita.
 * Multi-tenant por barberia_id y compatible con multi-país (documento_identidad).
 */
public class CitaDAO {

    private static final String SQL_INSERT =
            "INSERT INTO citas (barberia_id, cliente_id, empleado_id, fecha, hora, estado, notas) " +
            "VALUES (?, ?, ?, ?, ?, 'PENDIENTE', ?)";

    private static final String SQL_BASE_SELECT =
            "SELECT c.id, c.barberia_id, c.fecha, c.hora, c.estado, c.precio_total, " +
            "       c.notas, c.fecha_creacion, " +
            "       p_cli.id AS cli_id, p_cli.documento_identidad AS cli_doc, p_cli.tipo_documento AS cli_tipo_doc, " +
            "       p_cli.nombre AS cli_nombre, p_cli.apellido AS cli_apellido, p_cli.email AS cli_email, " +
            "       p_cli.telefono AS cli_tel, p_cli.foto_url AS cli_foto, p_cli.idioma_preferido AS cli_idioma, " +
            "       p_cli.pais_codigo AS cli_pais, p_cli.auth_provider AS cli_auth, p_cli.mfa_habilitado AS cli_mfa, " +
            "       p_cli.fecha_registro AS cli_fecha_reg, p_cli.activo AS cli_activo, cl.puntos_fidelidad, " +
            "       p_emp.id AS emp_id, p_emp.documento_identidad AS emp_doc, p_emp.tipo_documento AS emp_tipo_doc, " +
            "       p_emp.nombre AS emp_nombre, p_emp.apellido AS emp_apellido, p_emp.email AS emp_email, " +
            "       p_emp.telefono AS emp_tel, p_emp.foto_url AS emp_foto, p_emp.idioma_preferido AS emp_idioma, " +
            "       p_emp.pais_codigo AS emp_pais, p_emp.auth_provider AS emp_auth, p_emp.mfa_habilitado AS emp_mfa, " +
            "       p_emp.fecha_registro AS emp_fecha_reg, p_emp.activo AS emp_activo, " +
            "       e.rol, e.barberia_id AS emp_barberia_id, e.activo AS emp_empleado_activo " +
            "FROM citas c " +
            "INNER JOIN clientes cl ON c.cliente_id = cl.id " +
            "INNER JOIN personas p_cli ON cl.id = p_cli.id " +
            "INNER JOIN empleados e ON c.empleado_id = e.id " +
            "INNER JOIN personas p_emp ON e.id = p_emp.id ";

    private static final String SQL_POR_BARBERIA_Y_FECHA =
            SQL_BASE_SELECT + "WHERE c.barberia_id = ? AND c.fecha = ? ORDER BY c.hora";

    private static final String SQL_POR_BARBERO_Y_FECHA =
            SQL_BASE_SELECT + "WHERE c.empleado_id = ? AND c.barberia_id = ? AND c.fecha = ? ORDER BY c.hora";

    private static final String SQL_POR_CLIENTE =
            SQL_BASE_SELECT + "WHERE c.cliente_id = ? ORDER BY c.fecha DESC, c.hora DESC";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE citas SET estado = ? WHERE id = ? AND barberia_id = ?";

    private static final String SQL_ACTUALIZAR_PRECIO =
            "UPDATE citas SET precio_total = ? WHERE id = ? AND barberia_id = ?";

    // ─────────────────────────────────────────────────────────────────

    /**
     * Registra una nueva cita en estado PENDIENTE en la base de datos.
     * 
     * @param cita Objeto Cita a registrar
     * @return ID generado de la cita
     * @throws SQLException si ocurre un error de base de datos
     */
    public int registrarCita(Cita cita) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cita.getBarberiaId());
            ps.setInt(2, cita.getCliente().getId());
            ps.setInt(3, cita.getEmpleado().getId());
            ps.setDate(4, Date.valueOf(cita.getFecha()));
            ps.setTime(5, Time.valueOf(cita.getHora()));
            ps.setString(6, cita.getNotas());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID generado para la cita.");
    }

    /**
     * Devuelve la agenda del día completo para una barbería.
     * 
     * @param barberiaId ID de la barbería
     * @param fecha Fecha a consultar
     * @return Lista de citas agendadas
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Cita> buscarPorBarberiaYFecha(int barberiaId, LocalDate fecha) throws SQLException {
        List<Cita> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_BARBERIA_Y_FECHA)) {

            ps.setInt(1, barberiaId);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Devuelve la agenda personal de un barbero específico para un día determinado.
     * 
     * @param empleadoId ID del barbero
     * @param barberiaId ID de la barbería
     * @param fecha Fecha a consultar
     * @return Lista de citas agendadas para el barbero
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Cita> buscarPorBarberoYFecha(int empleadoId, int barberiaId, LocalDate fecha) throws SQLException {
        List<Cita> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_BARBERO_Y_FECHA)) {

            ps.setInt(1, empleadoId);
            ps.setInt(2, barberiaId);
            ps.setDate(3, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Devuelve el historial completo de citas de un cliente específico.
     * 
     * @param clienteId ID del cliente
     * @return Lista de citas ordenadas cronológicamente
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Cita> buscarPorCliente(int clienteId) throws SQLException {
        List<Cita> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_POR_CLIENTE)) {

            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearFila(rs));
            }
        }
        return lista;
    }

    /**
     * Cambia el estado actual de una cita (ej. PENDIENTE -> COMPLETADA).
     * 
     * @param citaId ID de la cita
     * @param barberiaId ID de la barbería (para seguridad multi-tenant)
     * @param nuevoEstado El estado a asignar
     * @return true si la actualización tuvo éxito, false si no se encontró
     * @throws SQLException si ocurre un error de base de datos
     */
    public boolean cambiarEstado(int citaId, int barberiaId, EstadoCita nuevoEstado) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_CAMBIAR_ESTADO)) {

            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, citaId);
            ps.setInt(3, barberiaId);
            return ps.executeUpdate() > 0;
        }
    }

    // ─── Mapeo de ResultSet → Objeto ─────────────────────────────────

    /**
     * Mapea una fila del ResultSet a un objeto Cita,
     * inicializando también los objetos Cliente y Empleado anidados.
     * 
     * @param rs El ResultSet posicionado
     * @return Objeto Cita construido
     * @throws SQLException si ocurre un error de lectura de columnas
     */
    private Cita mapearFila(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getInt("cli_id"),
                rs.getString("cli_doc"),
                rs.getString("cli_tipo_doc"),
                rs.getString("cli_nombre"),
                rs.getString("cli_apellido"),
                rs.getString("cli_email"),
                rs.getString("cli_tel"),
                null, // No se expone el hash
                rs.getString("cli_foto"),
                rs.getString("cli_idioma"),
                rs.getString("cli_pais"),
                rs.getString("cli_auth"),
                rs.getBoolean("cli_mfa"),
                rs.getTimestamp("cli_fecha_reg") != null ? rs.getTimestamp("cli_fecha_reg").toLocalDateTime() : null,
                rs.getBoolean("cli_activo"),
                rs.getInt("puntos_fidelidad")
        );

        Empleado empleado = new Empleado(
                rs.getInt("emp_id"),
                rs.getString("emp_doc"),
                rs.getString("emp_tipo_doc"),
                rs.getString("emp_nombre"),
                rs.getString("emp_apellido"),
                rs.getString("emp_email"),
                rs.getString("emp_tel"),
                null, // No se expone el hash
                rs.getString("emp_foto"),
                rs.getString("emp_idioma"),
                rs.getString("emp_pais"),
                rs.getString("emp_auth"),
                rs.getBoolean("emp_mfa"),
                rs.getTimestamp("emp_fecha_reg") != null ? rs.getTimestamp("emp_fecha_reg").toLocalDateTime() : null,
                rs.getBoolean("emp_activo"),
                rs.getInt("emp_barberia_id"),
                Rol.valueOf(rs.getString("rol")),
                rs.getBoolean("emp_empleado_activo")
        );

        return new Cita(
                rs.getInt("id"),
                rs.getInt("barberia_id"),
                cliente,
                empleado,
                rs.getDate("fecha").toLocalDate(),
                rs.getTime("hora").toLocalTime(),
                EstadoCita.valueOf(rs.getString("estado")),
                rs.getDouble("precio_total"),
                rs.getString("notas"),
                rs.getTimestamp("fecha_creacion") != null
                        ? rs.getTimestamp("fecha_creacion").toLocalDateTime()
                        : null
        );
    }
}