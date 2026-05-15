package com.mycompany.proyectobarberia.Persistencia;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {
    // Datos de configuración (Ajusta según tu phpMyAdmin)
    private static final String URL = "jdbc:mysql://localhost:3306/barberia_db";
    private static final String USER = "root"; // Usuario por defecto en XAMPP
    private static final String PASSWORD = ""; // Contraseña por defecto (vacia en XAMPP)

    public static Connection obtenerConexion() {
        Connection conexion = null;
        try {
            // Registrar el Driver (opcional en versiones nuevas, pero buena práctica)
            Class.forName("com.mysql.cj.jdbc.Driver");
            conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión exitosa a la base de datos.");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
        return conexion;
    }
}
