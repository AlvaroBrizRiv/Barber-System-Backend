package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Calificacion;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.CalificacionDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para calificaciones.
 *
 * GET  /api/calificaciones/barberia/{id}     → Calificaciones de una barbería (público).
 * GET  /api/calificaciones/promedio/{id}     → Promedio de una barbería (público).
 * GET  /api/calificaciones/barbero/{id}      → Promedio de un barbero (público).
 * POST /api/calificaciones                   → Calificar (solo CLIENTE, cita COMPLETADA).
 *
 * Reglas de negocio:
 *   - Solo clientes pueden calificar.
 *   - Solo se puede calificar una cita con estado COMPLETADA.
 *   - Cada cita tiene como máximo UNA calificación.
 */
public class CalificacionController {

    private static final CalificacionDAO dao = new CalificacionDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}",     CalificacionController::listarPorBarberia);
        ApiBuilder.get("/promedio/{id}",     CalificacionController::promedioBarberia);
        ApiBuilder.get("/barbero/{id}",      CalificacionController::promedioBarbero);
        ApiBuilder.post("/",                 CalificacionController::calificar);
    }

    /**
     * Lista las calificaciones públicas de una barbería.
     * 
     * @param ctx Contexto HTTP de Javalin con el ID de la barbería en la URL
     * @throws SQLException si ocurre un error en base de datos
     */
    private static void listarPorBarberia(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(dao.listarPorBarberia(barberiaId));
    }

    /**
     * Obtiene el promedio de estrellas general de una barbería.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en base de datos
     */
    private static void promedioBarberia(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        double promedio = dao.promedioBarberia(barberiaId);
        ctx.json(Map.of("barberiaId", barberiaId, "promedio", promedio));
    }

    /**
     * Obtiene el promedio de estrellas de un barbero específico.
     * Requiere barberiaId como query param para asegurar el tenant.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si hay un error en base de datos
     * @throws BadRequestResponse si no se incluye barberiaId
     */
    private static void promedioBarbero(Context ctx) throws SQLException {
        int empleadoId = Integer.parseInt(ctx.pathParam("id"));
        String barberiaIdStr = ctx.queryParam("barberiaId");
        if (barberiaIdStr == null) throw new BadRequestResponse("Se requiere ?barberiaId=...");
        int barberiaId = Integer.parseInt(barberiaIdStr);
        double promedio = dao.promedioBarbero(empleadoId, barberiaId);
        ctx.json(Map.of("barberoId", empleadoId, "promedio", promedio));
    }

    /**
     * Registra una calificación dejada por un cliente sobre una cita COMPLETADA.
     * 
     * @param ctx Contexto HTTP de Javalin con los detalles de la calificación en el body
     * @throws SQLException si ocurre un error de base de datos
     * @throws BadRequestResponse si la cita ya fue calificada o los datos son incorrectos
     */
    @SuppressWarnings("unchecked")
    private static void calificar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.CLIENTE);
        int clienteId = SecurityMiddleware.getUserId(ctx);

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int citaId        = ((Number) body.get("citaId")).intValue();
        int barberiaId    = ((Number) body.get("barberiaId")).intValue();
        int empleadoId    = ((Number) body.get("empleadoId")).intValue();
        int estBarberia   = ((Number) body.get("estrellasBarberia")).intValue();
        int estBarbero    = ((Number) body.get("estrellasBarbero")).intValue();
        String comentario = (String) body.getOrDefault("comentario", "");

        // Verificar que no haya sido calificada antes
        if (dao.yaFueCalificada(citaId)) {
            throw new BadRequestResponse("Esta cita ya fue calificada.");
        }

        try {
            Calificacion cal = new Calificacion(
                    citaId, clienteId, barberiaId, empleadoId,
                    estBarberia, estBarbero, comentario);

            int id = dao.registrar(cal);
            ctx.status(201).json(Map.of("id", id, "mensaje", "¡Gracias por tu calificación!"));

        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }
    }
}
