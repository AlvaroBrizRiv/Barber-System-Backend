package com.mycompany.proyectobarberia;

import com.mycompany.proyectobarberia.Persistencia.Conexion;
import java.sql.Connection;

public class ProyectoBarberia {
    public static void main(String[] args) {
        // Intento de conexión
        Connection cn = Conexion.obtenerConexion();
        
        if (cn != null) {
            System.out.println("¡Felicidades! Java ya está hablando con MySQL.");
            try {
                cn.close(); // Siempre cerrar la conexión después de probar
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("La conexión falló. Revisa que MySQL esté activo en phpMyAdmin.");
        }
    }
}
