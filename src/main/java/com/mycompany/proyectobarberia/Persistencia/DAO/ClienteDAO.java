package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteDAO {
    
    private static final String SQL_INSERT_PERSONA = 
        "INSERT INTO personas (rut, nombre, apellido, email, telefono) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_CLIENTE = 
        "INSERT INTO clientes (id, punto_fidelidad) VALUES (?, ?)";

    public boolean registrarCliente(Cliente cliente) {
        // Al abrir la conexión dentro del try, Java asegura su cierre automático
        try (Connection conn = Conexion.obtenerConexion()) {
            if (conn == null) return false;
            
            conn.setAutoCommit(false); // Transacción segura

            try (PreparedStatement psPersona = conn.prepareStatement(SQL_INSERT_PERSONA, PreparedStatement.RETURN_GENERATED_KEYS);
                 PreparedStatement psCliente = conn.prepareStatement(SQL_INSERT_CLIENTE)) {
                
                // 1. Insertar en la tabla base 'personas'
                psPersona.setInt(1, cliente.getRut());
                psPersona.setString(2, cliente.getNombre());
                psPersona.setString(3, cliente.getApellido());
                psPersona.setString(4, cliente.getEmail());
                psPersona.setInt(5, cliente.getTelefono());
                psPersona.executeUpdate();

                // Obtener el ID generado por la base de datos
                try (ResultSet rs = psPersona.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);
                        
                        // 2. Insertar en la tabla 'clientes' usando el ID relacional
                        psCliente.setInt(1, idGenerado);
                        psCliente.setInt(2, cliente.getPuntoFidelidad());
                        psCliente.executeUpdate();
                    }
                }

                conn.commit(); // Confirmar la transacción completa
                return true;
                
            } catch (SQLException e) {
                conn.rollback(); // Si algo falla en el proceso, deshace los cambios
                e.printStackTrace();
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}