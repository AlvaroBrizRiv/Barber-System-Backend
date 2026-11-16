package com.mycompany.proyectobarberia.Persistencia;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestión del pool de conexiones a la base de datos usando HikariCP.
 *
 * Soporta tanto SQL Server LocalDB ((localdb)\MSSQLLocalDB), SQL Server TCP/IP
 * como MySQL/MariaDB, leyendo la configuración desde el archivo ".env".
 */
public class Conexion {

    private static HikariDataSource dataSource;
    private static ServerSocket localDbBridgeServer;

    // ─── Inicialización perezosa (evita deadlock <clinit> con LocalDB-TDS-Bridge) ───

    private static String requerido(Dotenv env, String clave) {
        String valor = env.get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta variable de entorno requerida: " + clave + " (.env)");
        }
        return valor.trim();
    }

    private static synchronized HikariDataSource inicializarDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            return dataSource;
        }
        try {
            Dotenv env = cargarDotenv();

            HikariConfig config = new HikariConfig();

            // Conexión fail-closed desde variables de entorno obligatorias (.env)
            String rawUrl = requerido(env, "DB_URL");
            String jdbcUrl = resolverJdbcUrl(rawUrl);

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(requerido(env, "DB_USER"));
            config.setPassword(requerido(env, "DB_PASSWORD"));

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
            return dataSource;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Error al inicializar el pool de conexiones: " + e.getMessage(), e);
        }
    }

    /**
     * Carga el archivo .env buscando tanto en el directorio actual como en Barber-System-Backend
     * (útil cuando se abre la carpeta raíz en VS Code).
     */
    public static Dotenv cargarDotenv() {
        DotenvBuilder builder = Dotenv.configure().ignoreIfMissing();
        if (!new File(".env").exists() && new File("Barber-System-Backend/.env").exists()) {
            builder.directory("Barber-System-Backend");
        }
        return builder.load();
    }

    /**
     * Si la URL apunta a (localdb)\Instancia, levanta un puente local TCP -> Named Pipe
     * para que el driver oficial mssql-jdbc pueda conectarse a SQL Server LocalDB.
     */
    private static String resolverJdbcUrl(String rawUrl) throws IOException, InterruptedException {
        Pattern pattern = Pattern.compile("jdbc:sqlserver://\\(localdb\\)[\\\\/]([^;]+)(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawUrl);
        if (!matcher.matches()) {
            return rawUrl;
        }

        String instanceName = matcher.group(1).trim();
        String urlSuffix = matcher.group(2);

        String pipePath = obtenerNamedPipeLocalDb(instanceName);
        int bridgePort = iniciarPuenteLocalDb(pipePath);

        return "jdbc:sqlserver://127.0.0.1:" + bridgePort + urlSuffix;
    }

    private static String obtenerNamedPipeLocalDb(String instanceName) throws IOException, InterruptedException {
        String pipe = consultarPipeSqllocaldb(instanceName);
        if (pipe == null) {
            // Intentar iniciar la instancia de LocalDB si estaba detenida
            new ProcessBuilder("sqllocaldb", "start", instanceName)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor();
            pipe = consultarPipeSqllocaldb(instanceName);
        }
        if (pipe == null) {
            throw new IOException("No se pudo obtener el Named Pipe para (localdb)\\" + instanceName);
        }
        return pipe;
    }

    private static String consultarPipeSqllocaldb(String instanceName) throws IOException, InterruptedException {
        Process proc = new ProcessBuilder("sqllocaldb", "info", instanceName)
                .redirectErrorStream(true)
                .start();
        String pipePath = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf("np:\\\\.\\pipe\\");
                if (idx >= 0) {
                    pipePath = line.substring(idx + 3).trim(); // Quita el prefijo "np:"
                }
            }
        }
        proc.waitFor();
        return pipePath;
    }

    private static synchronized int iniciarPuenteLocalDb(String pipePath) throws IOException {
        if (localDbBridgeServer != null && !localDbBridgeServer.isClosed()) {
            return localDbBridgeServer.getLocalPort();
        }
        localDbBridgeServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Thread acceptThread = new Thread(() -> {
            while (!localDbBridgeServer.isClosed()) {
                try {
                    Socket clientSocket = localDbBridgeServer.accept();
                    clientSocket.setTcpNoDelay(true);
                    Thread worker = new Thread(() -> atenderConexionTds(clientSocket, pipePath));
                    worker.setDaemon(true);
                    worker.start();
                } catch (IOException ignored) {
                    break;
                }
            }
        }, "LocalDB-TDS-Bridge");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return localDbBridgeServer.getLocalPort();
    }

    /**
     * Reenvía tráfico de forma no bloqueante entre el Socket TCP de mssql-jdbc
     * y el Named Pipe de SQL Server LocalDB usando PeekNamedPipe (pipeIn.available()).
     */
    private static void atenderConexionTds(Socket socket, String pipePath) {
        try (Socket s = socket;
             InputStream sockIn = s.getInputStream();
             OutputStream sockOut = s.getOutputStream();
             RandomAccessFile pipe = new RandomAccessFile(pipePath, "rw");
             FileInputStream pipeIn = new FileInputStream(pipe.getFD());
             FileOutputStream pipeOut = new FileOutputStream(pipe.getFD())) {

            byte[] buf = new byte[65536];

            while (!s.isClosed() && !s.isInputShutdown() && !s.isOutputShutdown()) {
                boolean activity = false;

                int fromClient = sockIn.available();
                if (fromClient > 0) {
                    int n = sockIn.read(buf, 0, Math.min(buf.length, fromClient));
                    if (n < 0) {
                        break;
                    }
                    pipeOut.write(buf, 0, n);
                    activity = true;
                }

                int fromServer = pipeIn.available();
                if (fromServer > 0) {
                    int n = pipeIn.read(buf, 0, Math.min(buf.length, fromServer));
                    if (n < 0) {
                        break;
                    }
                    sockOut.write(buf, 0, n);
                    sockOut.flush();
                    activity = true;
                }

                if (!activity) {
                    Thread.sleep(1);
                }
            }
        } catch (IOException | InterruptedException ignored) {
            // Conexión cerrada
        }
    }

    // ─── API pública ──────────────────────────────────────────────────

    /**
     * Obtiene una conexión del pool. Debe cerrarse con try-with-resources.
     *
     * @return Conexión activa del pool de HikariCP.
     * @throws SQLException si no hay conexiones disponibles en el pool.
     */
    public static Connection obtenerConexion() throws SQLException {
        return inicializarDataSource().getConnection();
    }

    /**
     * Expone el DataSource para librerías que lo necesiten directamente.
     * @return El HikariDataSource configurado.
     */
    public static DataSource getDataSource() {
        return inicializarDataSource();
    }

    /**
     * Cierra el pool completo. Llamar solo al apagar el servidor.
     */
    public static void cerrarPool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (localDbBridgeServer != null && !localDbBridgeServer.isClosed()) {
            try {
                localDbBridgeServer.close();
            } catch (IOException ignored) {}
        }
    }

    // Constructor privado: no se debe instanciar esta clase
    private Conexion() {}
}
