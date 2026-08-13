package com.mycompany.proyectobarberia.API;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mycompany.proyectobarberia.API.Controladores.*;
import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import io.github.cdimascio.dotenv.Dotenv;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.json.JavalinGson;
import io.javalin.plugin.bundled.CorsPlugin;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Clase principal del servidor REST Javalin 6.
 *
 * CAMBIOS DE API EN JAVALIN 6 vs 5:
 *   - Las rutas se registran DENTRO de Javalin.create() con config.router.apiBuilder().
 *   - JavalinGson(Gson) → JavalinGson() con setter, o se usa el adaptador directo.
 *   - app.routes() fue ELIMINADO; se usa config.router.apiBuilder().
 *   - app.router() tampoco existe; la configuración es exclusivamente en create().
 *
 * Para iniciar el servidor: llamar App.iniciar() desde ProyectoBarberia.java.
 */
public class App {

    /** Inicia el servidor Javalin con todas las configuraciones. */
    public static void iniciar() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        int puerto     = Integer.parseInt(env.get("SERVER_PORT", "7070"));
        String corsOrigin = env.get("CORS_ORIGIN", "*");

        // ─── Gson con soporte para Java Time API ─────────────────────
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class,     new LocalDateAdapter())
                .registerTypeAdapter(LocalTime.class,     new LocalTimeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .serializeNulls()
                .create();

        // ─── Configuración del servidor (Javalin 6 API) ───────────────
        Javalin app = Javalin.create(config -> {

            // FIX ENCODING: forzar UTF-8 en todas las respuestas JSON
            config.http.defaultContentType = "application/json; charset=utf-8";

            // JSON mapper: Javalin 6 — JavalinGson(gson, lenient)
            config.jsonMapper(new JavalinGson(gson, true));

            // CORS — permite que el frontend llame a la API sin bloqueo
            config.registerPlugin(new CorsPlugin(corsConfig ->
                corsConfig.addRule(rule -> {
                    if ("*".equals(corsOrigin)) {
                        rule.anyHost();
                    } else {
                        rule.allowHost(corsOrigin);
                    }
                })
            ));

            // ─── Rutas — Javalin 6: DEBEN ir dentro de config.router.apiBuilder ───
            config.router.apiBuilder(() ->
                ApiBuilder.path("/api", () -> {
                    // Ruta de bienvenida pública
                    ApiBuilder.get("/", ctx -> ctx.json(Map.of(
                        "sistema",  "Barbería System API",
                        "version",  "1.0",
                        "estado",   "operativo",
                        "endpoints", new String[]{
                            "/api/barberias",
                            "/api/auth/login",
                            "/api/auth/registro"
                        }
                    )));
                    ApiBuilder.path("/auth",           AuthController::registrarRutas);
                    ApiBuilder.path("/barberias",      BarberiaController::registrarRutas);
                    ApiBuilder.path("/empleados",      EmpleadoController::registrarRutas);
                    ApiBuilder.path("/citas",          CitaController::registrarRutas);
                    ApiBuilder.path("/servicios",      ServicioController::registrarRutas);
                    ApiBuilder.path("/productos",      ProductoController::registrarRutas);
                    ApiBuilder.path("/calificaciones", CalificacionController::registrarRutas);
                    ApiBuilder.path("/suscripciones",  SuscripcionController::registrarRutas);
                    ApiBuilder.path("/perfil",         PerfilController::registrarRutas);
                    ApiBuilder.path("/carrito",        CarritoController::registrarRutas);
                })
            );

            // Log de requests (modo desarrollo)
            config.requestLogger.http((ctx, ms) ->
                System.out.printf("[%s] %s %s → %d (%.1fms)%n",
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    ctx.method(), ctx.path(), ctx.status().getCode(), ms));
        });

        // ─── Middleware de seguridad JWT (before-handler global) ──────
        app.before(SecurityMiddleware::validar);

        // ─── Manejo global de excepciones ─────────────────────────────
        app.exception(java.sql.SQLException.class, (e, ctx) -> {
            System.err.println("[SQL ERROR] " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Error interno del servidor."));
        });

        app.exception(NumberFormatException.class, (e, ctx) ->
            ctx.status(400).json(Map.of("error", "Parámetro inválido en la URL.")));

        app.exception(Exception.class, (e, ctx) -> {
            System.err.println("[ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage());
            ctx.status(500).json(Map.of("error", "Error interno del servidor."));
        });

        // ─── Iniciar ──────────────────────────────────────────────────
        app.start(puerto);

        System.out.println("=========================================");
        System.out.println("  BARBERIA SYSTEM -- API REST INICIADA   ");
        System.out.printf( "  Puerto : %-31s%n", puerto);
        System.out.printf( "  URL    : http://localhost:%-15s%n", puerto + "/api");
        System.out.println("=========================================");
    }

    // ─── Adaptadores Gson ─────────────────────────────────────────────

    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override public void write(JsonWriter out, LocalDate v) throws IOException {
            out.value(v != null ? v.toString() : null);
        }
        @Override public LocalDate read(JsonReader in) throws IOException {
            String s = in.nextString();
            return (s == null || s.isEmpty()) ? null : LocalDate.parse(s);
        }
    }

    private static class LocalTimeAdapter extends TypeAdapter<LocalTime> {
        @Override public void write(JsonWriter out, LocalTime v) throws IOException {
            out.value(v != null ? v.toString() : null);
        }
        @Override public LocalTime read(JsonReader in) throws IOException {
            String s = in.nextString();
            return (s == null || s.isEmpty()) ? null : LocalTime.parse(s);
        }
    }

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        @Override public void write(JsonWriter out, LocalDateTime v) throws IOException {
            out.value(v != null ? v.format(FMT) : null);
        }
        @Override public LocalDateTime read(JsonReader in) throws IOException {
            String s = in.nextString();
            return (s == null || s.isEmpty()) ? null : LocalDateTime.parse(s, FMT);
        }
    }
}
