package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.PoliticaBloqueo;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Lee y guarda el estado de bloqueo por intentos fallidos de login (tabla personas).
 * SQL estándar: funciona igual en SQL Server y PostgreSQL.
 */
public class BloqueoLoginDAO {

    private static final String SQL_ESTADO =
            "SELECT intentos_fallidos, bloqueado_hasta, bloqueada FROM personas WHERE id = ?";

    private static final String SQL_GUARDAR =
            "UPDATE personas SET intentos_fallidos = ?, bloqueado_hasta = ?, bloqueada = ? WHERE id = ?";

    private static final String SQL_REINICIAR =
            "UPDATE personas SET intentos_fallidos = 0, bloqueado_hasta = NULL, bloqueada = 0 WHERE id = ?";

    /**
     * @param personaId ID de la persona.
     * @return Estado actual (LIMPIO si la persona no existe).
     * @throws SQLException si falla la consulta
     */
    public PoliticaBloqueo.Estado obtener(int personaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ESTADO)) {
            ps.setInt(1, personaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return PoliticaBloqueo.Estado.LIMPIO;
                Timestamp hasta = rs.getTimestamp("bloqueado_hasta");
                return new PoliticaBloqueo.Estado(
                        rs.getInt("intentos_fallidos"),
                        hasta != null ? hasta.toLocalDateTime() : null,
                        rs.getInt("bloqueada") == 1);
            }
        }
    }

    /**
     * @param personaId ID de la persona.
     * @param estado    Estado a guardar.
     * @throws SQLException si falla la actualización
     */
    public void guardar(int personaId, PoliticaBloqueo.Estado estado) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_GUARDAR)) {
            ps.setInt(1, estado.intentosFallidos());
            if (estado.bloqueadoHasta() != null) ps.setTimestamp(2, Timestamp.valueOf(estado.bloqueadoHasta()));
            else ps.setNull(2, Types.TIMESTAMP);
            ps.setInt(3, estado.bloqueada() ? 1 : 0);
            ps.setInt(4, personaId);
            ps.executeUpdate();
        }
    }

    /**
     * Vuelve el contador a cero (login correcto).
     *
     * @param personaId ID de la persona.
     * @throws SQLException si falla la actualización
     */
    public void reiniciar(int personaId) throws SQLException {
        try (Connection conn = Conexion.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_REINICIAR)) {
            ps.setInt(1, personaId);
            ps.executeUpdate();
        }
    }
}
