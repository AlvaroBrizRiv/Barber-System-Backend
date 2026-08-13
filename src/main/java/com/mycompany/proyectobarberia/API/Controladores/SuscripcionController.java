package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Suscripcion;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.SuscripcionDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para planes de suscripción.
 *
 * GET    /api/suscripciones/barberia/{id} → Lista planes de la barbería (público).
 * POST   /api/suscripciones               → Crear plan (ADMIN de la barbería).
 * PUT    /api/suscripciones/{id}          → Actualizar plan (ADMIN).
 * PATCH  /api/suscripciones/{id}/toggle   → Activar/desactivar plan (ADMIN).
 *
 * Requisito: El administrador de cada barbería gestiona sus suscripciones.
 */
public class SuscripcionController {

    private static final SuscripcionDAO dao = new SuscripcionDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}",      SuscripcionController::listar);
        ApiBuilder.post("/",                  SuscripcionController::crear);
        ApiBuilder.put("/{id}",               SuscripcionController::actualizar);
        ApiBuilder.patch("/{id}/toggle",      SuscripcionController::toggle);
    }

    /**
     * Lista los planes de suscripción de una barbería de forma pública para clientes.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error al consultar en la BD
     */
    private static void listar(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(dao.listarPorBarberia(barberiaId));
    }

    /**
     * Crea un nuevo plan de suscripción. El barberiaId proviene del JWT del administrador.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de BD
     */
    private static void crear(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        Suscripcion suscripcion = ctx.bodyAsClass(Suscripcion.class);
        suscripcion.setBarberiaId(barberiaId);

        int id = dao.crear(suscripcion);
        ctx.status(201).json(Map.of("id", id, "mensaje", "Plan de suscripción creado."));
    }

    /**
     * Actualiza un plan existente.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si hay un fallo en base de datos
     * @throws NotFoundResponse si el plan no existe o no corresponde a esta barbería
     */
    private static void actualizar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);
        int suscripcionId = Integer.parseInt(ctx.pathParam("id"));

        Suscripcion suscripcion = ctx.bodyAsClass(Suscripcion.class);
        suscripcion.setId(suscripcionId);
        suscripcion.setBarberiaId(barberiaId);

        if (!dao.actualizar(suscripcion)) throw new NotFoundResponse("Plan no encontrado.");
        ctx.json(Map.of("mensaje", "Plan actualizado correctamente."));
    }

    /**
     * Activa o desactiva un plan de suscripción (toggle).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si falla la operación
     * @throws NotFoundResponse si el plan no fue encontrado
     */
    private static void toggle(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        int suscripcionId = Integer.parseInt(ctx.pathParam("id"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        if (!dao.toggleActiva(suscripcionId, barberiaId))
            throw new NotFoundResponse("Plan no encontrado.");
        ctx.json(Map.of("mensaje", "Estado del plan actualizado."));
    }
}
