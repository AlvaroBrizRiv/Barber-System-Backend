package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Cita;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CitaDAO {

    // SQL parametrizado para blindar el sistema contra SQL Injection
    private static final String SQL_INSERT_CITA
            = "INSERT INTO citas (fecha, hora, servicio, cliente_id, empleado_id) VALUES (?, ?, ?, ?, ?)";

    public boolean registrarCita(Cita cita) {

        // El Try-with-resources abre y autocierra los recursos de manera limpia
        try (Connection conn = Conexion.obtenerConexion(); PreparedStatement ps = conn.prepareStatement(SQL_INSERT_CITA)) {

            if (conn == null) {
                return false;
            }
            // Asignamos los tipos de datos correspondientes a cada '?'
            ps.setString(1, cita.getFecha());
            ps.setString(2, cita.getHora());
            ps.setString(3, cita.getServicio());

            // Navegamos por composición para extraer las llaves foráneas (IDs)
            ps.setInt(4, cita.getCliente().getId());
            ps.setInt(5, cita.getEmpleado().getId());

            // Ejecutamos la inserción en MySQL
            int filasAfectadas = ps.executeUpdate();

            // Si devolvió más de 0, significa que se insertó correctamente
            return filasAfectadas > 0;
        }
        catch (SQLException e) {
            System.out.println("Error crítico al registrar la cita en la base de datos: " + e.getMessage());
        return false;
    }
    }
}