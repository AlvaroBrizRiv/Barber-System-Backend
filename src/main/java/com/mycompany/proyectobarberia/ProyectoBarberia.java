package com.mycompany.proyectobarberia;

import com.mycompany.proyectobarberia.API.App;
import com.mycompany.proyectobarberia.Persistencia.Conexion;

/**
 * Punto de entrada principal del sistema de barbería.
 *
 * Inicia el servidor REST Javalin que expone la API para:
 *   - El sitio web (frontend HTML/JS en VS Code).
 *   - La futura aplicación móvil.
 *   - El panel de administración de barberías.
 *
 * Para ejecutar:
 *   1. Copia .env.example a .env y completa las credenciales.
 *   2. Ejecuta el schema.sql en MySQL para crear las tablas.
 *   3. Ejecuta este archivo (mvn exec:java o botón Run en NetBeans).
 *   4. La API estará en http://localhost:7070/api
 *   5. Abre el frontend con VS Code Live Server en http://localhost:5500
 */
public class ProyectoBarberia {

    /**
     * Método principal que inicializa la aplicación.
     * Configura y arranca el servidor HTTP Javalin, y registra el hook de cerrado.
     * 
     * @param args Argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        // Inicializar el servidor REST
        App.iniciar();

        // Cerrar el pool de conexiones al detener el servidor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Cerrando servidor y pool de conexiones...");
            Conexion.cerrarPool();
        }));
    }
}
