package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Agenda;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos del horario de atención de cada barbería y de las horas
 * ya tomadas por cada barbero (para calcular la disponibilidad).
 * SQL estándar: compatible con SQL Server y PostgreSQL.
 */
public class HorarioDAO {

    private static final String SQL_LISTAR =
            "SELECT dia_semana, abierto, hora_apertura, hora_cierre FROM horarios_barberia " +
            "WHERE barberia_id = ? ORDER BY dia_semana";

    private static final String SQL_BORRAR =
            "DELETE FROM horarios_barberia WHERE barberia_id = ?";

    private static final String SQL_INSERTAR =
            "INSERT INTO horarios_barberia (barberia_id, dia_semana, abierto, hora_apertura, hora_cierre) " +
            "VALUES (?, ?, ?, ?, ?)";

    /** Citas no canceladas del barbero ese día, con la duración total de sus servicios. */
    private static final String SQL_OCUPADOS =
            "SELECT c.hora, COALESCE(SUM(s.duracion_minutos), 0) AS duracion " +
            "FROM citas c " +
            "LEFT JOIN cita_servicios cs ON cs.cita_id = c.id " +
            "LEFT JOIN servicios s ON s.id = cs.servicio_id " +
            "WHERE c.empleado_id = ? AND c.fecha = ? AND c.estado <> 'CANCELADA' " +
            "GROUP BY c.id, c.hora";

    /**
     * Semana completa de la barbería (7 días). Los días sin configurar usan el horario por defecto.
     *
     * @param barberiaId ID de la barbería.
     * @return Horario de lunes a domingo.
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Agenda.HorarioDia> semana(int barberiaId) throws SQLException {
        List<Agenda.HorarioDia> guardados = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR)) {
            ps.setInt(1, barberiaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time apertura = rs.getTime("hora_apertura");
                    Time cierre = rs.getTime("hora_cierre");
                    guardados.add(new Agenda.HorarioDia(
                            rs.getInt("dia_semana"),
                            rs.getInt("abierto") == 1,
                            apertura != null ? apertura.toLocalTime() : null,
                            cierre != null ? cierre.toLocalTime() : null));
                }
            }
        }
        return Agenda.completarSemana(guardados);
    }

    /**
     * Reemplaza el horario de la barbería dentro de una transacción.
     *
     * @param barberiaId ID de la barbería.
     * @param dias       Los 7 días ya validados con {@link Agenda#validar(List)}.
     * @throws SQLException si ocurre un error de base de datos
     */
    public void guardar(int barberiaId, List<Agenda.HorarioDia> dias) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(SQL_BORRAR)) {
                    ps.setInt(1, barberiaId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR)) {
                    for (Agenda.HorarioDia d : dias) {
                        ps.setInt(1, barberiaId);
                        ps.setInt(2, d.diaSemana());
                        // setInt 1/0: compatible con BIT (SQL Server) y SMALLINT (PostgreSQL)
                        ps.setInt(3, d.abierto() ? 1 : 0);
                        if (d.abierto()) {
                            ps.setTime(4, Time.valueOf(d.apertura()));
                            ps.setTime(5, Time.valueOf(d.cierre()));
                        } else {
                            ps.setNull(4, Types.TIME);
                            ps.setNull(5, Types.TIME);
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Horas ya tomadas por un barbero en una fecha (citas no canceladas).
     *
     * @param empleadoId ID del barbero.
     * @param fecha      Fecha consultada.
     * @return Inicio y duración de cada cita.
     * @throws SQLException si ocurre un error de base de datos
     */
    public List<Agenda.Ocupado> ocupados(int empleadoId, LocalDate fecha) throws SQLException {
        List<Agenda.Ocupado> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_OCUPADOS)) {
            ps.setInt(1, empleadoId);
            ps.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalTime hora = rs.getTime("hora").toLocalTime();
                    lista.add(new Agenda.Ocupado(hora, rs.getInt("duracion")));
                }
            }
        }
        return lista;
    }

    /**
     * Duración total en minutos de los servicios activos indicados de una barbería.
     *
     * @param barberiaId  ID de la barbería.
     * @param servicioIds IDs de servicios elegidos.
     * @return Suma de duraciones (0 si la lista está vacía o ningún servicio es válido).
     * @throws SQLException si ocurre un error de base de datos
     */
    public int duracionServicios(int barberiaId, List<Integer> servicioIds) throws SQLException {
        if (servicioIds == null || servicioIds.isEmpty()) return 0;
        String marcas = String.join(",", servicioIds.stream().map(i -> "?").toList());
        String sql = "SELECT COALESCE(SUM(duracion_minutos), 0) FROM servicios " +
                     "WHERE barberia_id = ? AND activo = 1 AND id IN (" + marcas + ")";
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barberiaId);
            for (int i = 0; i < servicioIds.size(); i++) ps.setInt(i + 2, servicioIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
