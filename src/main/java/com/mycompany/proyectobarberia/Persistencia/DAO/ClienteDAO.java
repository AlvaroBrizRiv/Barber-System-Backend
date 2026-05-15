package com.mycompany.proyectobarberia.Persistencia.DAO;

import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Persistencia.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ClienteDAO {
    
    // SQL seguro con parámetros (?) para evitar SQL Injection
    private static final String SQL_INSERT = 
        "INSERT INTO personas (rut, nombre, apellido, email, telefono) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_INSERT_CLIENTE = 
        "INSERT INTO clientes (id, punto_fidelidad) VALUES (?, ?)";

    public boolean registrarCliente(Cliente cliente) {
        Connection conn = null;
        PreparedStatement psPersona = null;
        PreparedStatement psCliente = null;
        
        try {
            conn = Conexion.obtenerConexion();
            conn.setAutoCommit(false); // Transacción para asegurar integridad de datos

            // 1. Insertar en la tabla base 'personas'
            psPersona = conn.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS);
            psPersona.setInt(1, cliente.getRut());
            psPersona.setString(2, cliente.getNombre());
            psPersona.setString(3, cliente.getApellido());
            psPersona.setString(4, cliente.getEmail());
            psPersona.setInt(5, cliente.getTelefono());
            psPersona.executeUpdate();

            // Obtener el ID generado para la relación
            var rs = psPersona.getGeneratedKeys();
            if (rs.next()) {
                int idGenerado = rs.getInt(1);
                
                // 2. Insertar en la tabla 'clientes'
                psCliente = conn.prepareStatement(SQL_INSERT_CLIENTE);
                psCliente.setInt(1, idGenerado);
                psCliente.setInt(2, cliente.getPuntoFidelidad());
                psCliente.executeUpdate();
            }

            conn.commit(); // Confirmar cambios
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            // Cerrar recursos para evitar fugas de memoria (Memory Leaks)
            try { if (psPersona != null) psPersona.close(); } catch (SQLException e) {}
            try { if (psCliente != null) psCliente.close(); } catch (SQLException e) {}
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }
}
