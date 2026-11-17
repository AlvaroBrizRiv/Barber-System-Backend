package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.API.dto.GananciaBarbero;
import com.mycompany.proyectobarberia.API.dto.GananciaDia;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de ganancias. Solo cuentan las citas en estado COMPLETADA.
 * SQL estándar (sin funciones propias del motor): funciona en SQL Server y PostgreSQL.
 */
public class GananciaDAO {

    private static final String SQL_POR_BARBERO =
            "SELECT e.id, p.nombre, p.apellido, COUNT(c.id) AS citas, " +
            "       COALESCE(SUM(c.precio_total), 0) AS total " +
            "FROM empleados e " +
            "INNER JOIN personas p ON p.id = e.id " +
            "LEFT JOIN citas c ON c.empleado_id = e.id AND c.barberia_id = e.barberia_id " +
            "       AND c.estado = 'COMPLETADA' AND c.fecha BETWEEN ? AND ? " +
            "WHERE e.barberia_id = ? %s" +
            "GROUP BY e.id, p.nombre, p.apellido, e.activo " +
            "HAVING e.activo = 1 OR COUNT(c.id) > 0 " +
            "ORDER BY total DESC, p.nombre";

    private static final String SQL_SERIE =
            "SELECT c.fecha, COUNT(*) AS citas, COALESCE(SUM(c.precio_total), 0) AS total " +
            "FROM citas c " +
            "WHERE c.barberia_id = ? AND c.estado = 'COMPLETADA' AND c.fecha BETWEEN ? AND ? %s" +
            "GROUP BY c.fecha ORDER BY c.fecha";

    /**
     * Ganancias por barbero en el rango.
     *
     * @param barberiaId ID de la barbería (del JWT).
     * @param desde      Fecha inicial inclusiva.
     * @param hasta      Fecha final inclusiva.
     * @param barberoId  Filtrar por un barbero; null para todos.
     * @return Lista ordenada de mayor a menor ganancia.
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<GananciaBarbero> porBarbero(int barberiaId, LocalDate desde, LocalDate hasta,
                                            Integer barberoId) throws SQLException {
        String sql = String.format(SQL_POR_BARBERO, barberoId != null ? "AND e.id = ? " : "");
        List<GananciaBarbero> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ps.setInt(3, barberiaId);
            if (barberoId != null) ps.setInt(4, barberoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new GananciaBarbero(
                            rs.getInt("id"), rs.getString("nombre"), rs.getString("apellido"),
                            rs.getInt("citas"), noNulo(rs.getBigDecimal("total"))));
                }
            }
        }
        return lista;
    }

    /**
     * Totales diarios en el rango (solo días con citas completadas).
     *
     * @param barberiaId ID de la barbería.
     * @param desde      Fecha inicial inclusiva.
     * @param hasta      Fecha final inclusiva.
     * @param barberoId  Filtrar por un barbero; null para toda la barbería.
     * @return Lista ordenada por fecha.
     * @throws SQLException si ocurre un error en la consulta
     */
    public List<GananciaDia> serieDiaria(int barberiaId, LocalDate desde, LocalDate hasta,
                                         Integer barberoId) throws SQLException {
        String sql = String.format(SQL_SERIE, barberoId != null ? "AND c.empleado_id = ? " : "");
        List<GananciaDia> lista = new ArrayList<>();
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, barberiaId);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            if (barberoId != null) ps.setInt(4, barberoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new GananciaDia(rs.getDate("fecha").toLocalDate(),
                            rs.getInt("citas"), noNulo(rs.getBigDecimal("total"))));
                }
            }
        }
        return lista;
    }

    private static BigDecimal noNulo(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
