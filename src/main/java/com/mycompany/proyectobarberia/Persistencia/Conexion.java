package com.mycompany.proyectobarberia.Persistencia;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestión del pool de conexiones a la base de datos usando HikariCP.
 *
 * CORRECCIONES respecto a la versión anterior:
 *   - Las credenciales ya NO están hardcodeadas: se leen del archivo .env.
 *   - Se usa HikariCP (pool de conexiones) en lugar de crear/destruir una
 *     conexión en cada request (mejora de rendimiento y estabilidad crítica).
 *   - La clase es un Singleton con inicialización lazy-safe.
 *   - Ya no se imprime información de conexión en cada llamada.
 *
 * Configurar las credenciales en el archivo ".env" en la raíz del proyecto.
 * Ver ".env.example" como plantilla.
 */
public class Conexion {

    private static HikariDataSource dataSource;

    // ─── Bloque estático de configuración ────────────────────────────

    static {
        try {
            Dotenv env = Dotenv.configure()
                    .ignoreIfMissing()  // No falla si .env no existe (usa variables del sistema)
                    .load();

            HikariConfig config = new HikariConfig();

            // Conexión desde variables de entorno (nunca hardcodeadas)
            config.setJdbcUrl(env.get("DB_URL",
                    "jdbc:mysql://localhost:3306/barberia_db?useSSL=false&serverTimezone=America/Santiago&characterEncoding=UTF-8"));
            config.setUsername(env.get("DB_USER", "root"));
            config.setPassword(env.get("DB_PASSWORD", ""));

            // Pool settings — tuneados para una aplicación web de baja-media concurrencia
            String poolSizeStr = env.get("DB_POOL_SIZE", "10");
            int poolSize = Integer.parseInt(poolSizeStr);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30_000);    // 30 segundos máx para obtener conexión
            config.setIdleTimeout(600_000);          // 10 min antes de cerrar conexiones inactivas
            config.setMaxLifetime(1_800_000);        // 30 min máx de vida de una conexión
            config.setPoolName("BarberiasPool");

            // Validación de conexión (detecta conexiones caídas)
            config.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            throw new ExceptionInInitializerError(
                    "Error al inicializar el pool de conexiones: " + e.getMessage());
        }
    }

    // ─── API pública ──────────────────────────────────────────────────

    /**
     * Obtiene una conexión del pool. Debe cerrarse con try-with-resources.
     *
     * <pre>
     * try (Connection conn = Conexion.obtenerConexion()) {
     *     // usar conn...
     * }
     * </pre>
     *
     * @return Conexión activa del pool de HikariCP.
     * @throws SQLException si no hay conexiones disponibles en el pool.
     */
    public static Connection obtenerConexion() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Expone el DataSource para librerías que lo necesiten directamente.
     * @return El HikariDataSource configurado.
     */
    public static DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Cierra el pool completo. Llamar solo al apagar el servidor.
     */
    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Constructor privado: no se debe instanciar esta clase
    private Conexion() {}
}
