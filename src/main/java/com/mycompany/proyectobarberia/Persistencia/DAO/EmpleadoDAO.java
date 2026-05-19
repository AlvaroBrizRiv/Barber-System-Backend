package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EmpleadoDAO {

    //Definición de la clase y consultas SQL.
    private static final String SQL_INSERT_PERSONA
            = "INSERT INTO personas (rut, nombre, apellido, email, telefono) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_EMPLEADO
            = "INSERT INTO empleados (id, rango) VALUES (?, ?)";

    public boolean registrarEmpleado(Empleado empleado) {

        //abrimos conexión de forma segura
        try (Connection conn = Conexion.obtenerConexion()) {
            if (conn == null) {
                return false;
            }

            //Iniciar transacción: Desactivamos el auto-guardado para controlar el proceso
            conn.setAutoCommit(false);

            // 3. Preparar los Statements para ambas tablas
            try (PreparedStatement psPersona = conn.prepareStatement(SQL_INSERT_PERSONA, PreparedStatement.RETURN_GENERATED_KEYS); PreparedStatement psEmpleado = conn.prepareStatement(SQL_INSERT_EMPLEADO)) {

                // Mapear datos de la Persona
                psPersona.setInt(1, empleado.getRut());
                psPersona.setString(2, empleado.getNombre());
                psPersona.setString(3, empleado.getApellido());
                psPersona.setString(4, empleado.getEmail());
                psPersona.setInt(5, empleado.getTelefono());
                psPersona.executeUpdate();

                // Recuperar el ID generado en la tabla personas
                try (ResultSet rs = psPersona.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);

                        // Mapear datos del Empleado
                        psEmpleado.setInt(1, idGenerado);
                        psEmpleado.setString(2, empleado.getRango()); // Ejemplo: "Senior", "Junior"
                        psEmpleado.executeUpdate();
                    }
                }

                // 4. Si todo salió bien, guardamos los cambios definitivamente
                conn.commit();
                return true;

            } catch (SQLException e) {
                // 5. Si algo falla (ej. RUT duplicado), deshacemos todo para mantener la integridad
                conn.rollback();
                System.out.println("Error en la transacción, haciendo rollback: " + e.getMessage());
                return false;
            }

        } catch (SQLException e) {
            System.out.println("Error de conexión a la base de datos: " + e.getMessage());
            return false;
        }
    }
}
