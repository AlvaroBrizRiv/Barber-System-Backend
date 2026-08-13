package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Servicio;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.ServicioDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para servicios de barberías.
 *
 * GET    /api/servicios/barberia/{id} → Lista servicios de una barbería (público).
 * POST   /api/servicios               → Crear servicio (ADMIN de la barbería).
 * PUT    /api/servicios/{id}          → Actualizar servicio (ADMIN).
 * DELETE /api/servicios/{id}          → Desactivar servicio (ADMIN).
 *
 * Requisito: Cada barbería define sus propios servicios.
 */
public class ServicioController {

    private static final ServicioDAO dao = new ServicioDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}", ServicioController::listar);
        ApiBuilder.post("/",             ServicioController::crear);
        ApiBuilder.put("/{id}",          ServicioController::actualizar);
        ApiBuilder.delete("/{id}",       ServicioController::desactivar);
    }

    /**
     * Lista los servicios activos de una barbería (endpoint público para clientes).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     */
    private static void listar(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(dao.listarPorBarberia(barberiaId));
    }

    /**
     * Crea un nuevo servicio. El barberia_id viene del JWT del admin.
     * Body esperado: { "nombre": "...", "descripcion": "...", "precio": 15000, "duracionMinutos": 30 }
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error de inserción
     * @throws BadRequestResponse si faltan campos obligatorios
     */
    private static void crear(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        Servicio servicio = ctx.bodyAsClass(Servicio.class);
        servicio.setBarberiaId(barberiaId); // Siempre del JWT, no del body

        if (servicio.getNombre() == null || servicio.getPrecio() <= 0) {
            throw new BadRequestResponse("Nombre y precio son requeridos.");
        }

        int id = dao.crear(servicio);
        ctx.status(201).json(Map.of("id", id, "mensaje", "Servicio creado exitosamente."));
    }

    /**
     * Actualiza un servicio. El barberia_id en el WHERE garantiza el aislamiento multi-tenant.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws NotFoundResponse si el servicio no existe o no corresponde a esta barbería
     */
    private static void actualizar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);
        int servicioId = Integer.parseInt(ctx.pathParam("id"));

        Servicio servicio = ctx.bodyAsClass(Servicio.class);
        servicio.setId(servicioId);
        servicio.setBarberiaId(barberiaId);

        if (!dao.actualizar(servicio)) throw new NotFoundResponse("Servicio no encontrado.");
        ctx.json(Map.of("mensaje", "Servicio actualizado correctamente."));
    }

    /**
     * Desactiva un servicio (soft-delete).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si falla la base de datos
     * @throws NotFoundResponse si el servicio no existe o faltan permisos
     */
    private static void desactivar(Context ctx) throws SQLException {
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        int servicioId = Integer.parseInt(ctx.pathParam("id"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        if (!dao.desactivar(servicioId, barberiaId))
            throw new NotFoundResponse("Servicio no encontrado.");
        ctx.json(Map.of("mensaje", "Servicio desactivado."));
    }
}
