package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Barberia;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.BarberiaDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para barberías.
 *
 * GET    /api/barberias         → Lista pública de barberías activas.
 * GET    /api/barberias/{id}    → Detalle público de una barbería.
 * POST   /api/barberias         → Crear barbería (solo SUPERADMIN).
 * PUT    /api/barberias/{id}    → Actualizar MI barbería (ADMIN_BARBERIA o SUPERADMIN).
 * DELETE /api/barberias/{id}    → Desactivar barbería (solo SUPERADMIN).
 */
public class BarberiaController {

    private static final BarberiaDAO dao = new BarberiaDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/",        BarberiaController::listar);
        ApiBuilder.get("/{id}",    BarberiaController::obtener);
        ApiBuilder.post("/",       BarberiaController::crear);
        ApiBuilder.put("/{id}",    BarberiaController::actualizar);
        ApiBuilder.delete("/{id}", BarberiaController::desactivar);
    }

    /**
     * Lista pública de todas las barberías activas.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error al consultar la base de datos
     */
    private static void listar(Context ctx) throws SQLException {
        // Devuelve la lista de barberías activas en formato JSON
        ctx.json(dao.listarActivas());
    }

    /**
     * Obtiene el detalle público de una barbería por su ID.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws NotFoundResponse si no existe la barbería solicitada
     */
    private static void obtener(Context ctx) throws SQLException {
        // Obtiene el ID de la ruta
        int id = Integer.parseInt(ctx.pathParam("id"));
        
        // Busca la barbería en la base de datos
        Barberia b = dao.buscarPorId(id);
        
        // Verifica si la barbería existe antes de responder
        if (b == null) throw new NotFoundResponse("Barbería no encontrada.");
        ctx.json(b);
    }

    /**
     * Crea una nueva barbería. Operación exclusiva para el rol SUPERADMIN.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error al insertar en la base de datos
     * @throws BadRequestResponse si falta el nombre de la barbería
     */
    private static void crear(Context ctx) throws SQLException {
        // Verifica que el rol del usuario sea SUPERADMIN antes de continuar
        SecurityMiddleware.requerirRol(ctx, Rol.SUPERADMIN);
        
        // Deserializa el cuerpo de la petición a un objeto Barberia
        Barberia nueva = ctx.bodyAsClass(Barberia.class);
        
        // Valida que el nombre no sea nulo ni esté vacío
        if (nueva.getNombre() == null || nueva.getNombre().isBlank()) {
            throw new BadRequestResponse("El nombre de la barbería es requerido.");
        }
        
        // Guarda la nueva barbería y devuelve el ID generado
        int id = dao.crear(nueva);
        ctx.status(201).json(Map.of("id", id, "mensaje", "Barbería creada exitosamente."));
    }

    /**
     * Actualiza los datos de una barbería.
     * SEGURIDAD: el admin solo puede actualizar SU barbería (validado por JWT).
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error al actualizar la base de datos
     * @throws NotFoundResponse si la barbería no existe
     */
    private static void actualizar(Context ctx) throws SQLException {
        // Obtiene el ID de la ruta a modificar
        int idRuta = Integer.parseInt(ctx.pathParam("id"));
        
        // Verifica que el usuario tenga permisos sobre esta barbería específica
        SecurityMiddleware.requerirAdminDeBarberia(ctx, idRuta);

        // Deserializa la petición y asigna el ID correspondiente
        Barberia actualizada = ctx.bodyAsClass(Barberia.class);
        actualizada.setId(idRuta);

        // Ejecuta la actualización y responde en caso de error
        if (!dao.actualizar(actualizada)) throw new NotFoundResponse("Barbería no encontrada.");
        ctx.json(Map.of("mensaje", "Barbería actualizada correctamente."));
    }

    /**
     * Desactiva una barbería. Operación exclusiva para el rol SUPERADMIN.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws NotFoundResponse si no se encuentra la barbería
     */
    private static void desactivar(Context ctx) throws SQLException {
        // Verifica que el rol del usuario sea SUPERADMIN antes de continuar
        SecurityMiddleware.requerirRol(ctx, Rol.SUPERADMIN);
        
        // Obtiene el ID de la barbería a desactivar
        int id = Integer.parseInt(ctx.pathParam("id"));
        
        // Ejecuta la desactivación y lanza excepción si falla
        if (!dao.desactivar(id)) throw new NotFoundResponse("Barbería no encontrada.");
        ctx.json(Map.of("mensaje", "Barbería desactivada."));
    }
}
