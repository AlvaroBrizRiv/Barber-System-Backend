package com.mycompany.proyectobarberia.API.Controladores;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

/**
 * Controlador REST para empleados (barberos y admins de barbería).
 *
 * GET    /api/empleados/barberia/{id}   → Lista empleados de la barbería (ADMIN).
 * GET    /api/empleados/barberos/{id}   → Lista solo barberos (público para reservas).
 * POST   /api/empleados                → Crear empleado (ADMIN de la barbería).
 * DELETE /api/empleados/{id}           → Desactivar empleado (ADMIN de la barbería).
 *
 * SEGURIDAD:
 *   - Un BARBERO no puede crear/eliminar empleados (solo ADMIN_BARBERIA).
 *   - Un admin de barbería A no puede crear empleados en barbería B.
 */
public class EmpleadoController {

    private static final EmpleadoDAO dao = new EmpleadoDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{id}",  EmpleadoController::listarPorBarberia);
        ApiBuilder.get("/barberos/{id}",  EmpleadoController::listarBarberos);
        ApiBuilder.post("/",              EmpleadoController::crear);
        ApiBuilder.delete("/{id}",        EmpleadoController::desactivar);
    }

    /**
     * Lista todos los empleados de la barbería. Solo accesible por el admin de ESA barbería.
     * 
     * @param ctx Contexto HTTP de Javalin con el ID de la barbería
     * @throws SQLException si ocurre un error en la base de datos
     * @throws ForbiddenResponse si el usuario no tiene el rol o la barbería no coincide
     */
    private static void listarPorBarberia(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        
        // Verifica si el administrador actual maneja esta barbería
        SecurityMiddleware.requerirAdminDeBarberia(ctx, barberiaId);
        
        ctx.json(dao.listarPorBarberia(barberiaId));
    }

    /**
     * Lista solo los barberos de una barbería determinada.
     * Endpoint semi-público diseñado para el formulario de reservas.
     * No requiere autenticación porque el cliente (incluso no logueado) necesita verlo al reservar.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     */
    private static void listarBarberos(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        ctx.json(dao.listarBarberosPorBarberia(barberiaId));
    }

    /**
     * Crea un nuevo empleado (barbero o admin).
     * SEGURIDAD: el barberiaId viene del JWT del admin, no del body, para evitar que
     * un admin de barbería A pueda crear empleados en barbería B.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si faltan campos obligatorios o los datos son inválidos
     */
    @SuppressWarnings("unchecked")
    private static void crear(Context ctx) throws SQLException {
        // Restricción a rol ADMIN_BARBERIA y obtención de la barbería del token
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        Integer barberiaIdToken = SecurityMiddleware.getBarberiaId(ctx);

        // Parseo de los campos obligatorios y opcionales
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String documento   = body.get("documentoIdentidad");
        String tipoDoc     = body.getOrDefault("tipoDocumento", "RUT");
        String nombre      = body.get("nombre");
        String apellido    = body.get("apellido");
        String email       = body.get("email");
        String telefono    = body.get("telefono");
        String password    = body.get("password");
        String rolStr      = body.getOrDefault("rol", "BARBERO");
        String paisCodigo  = body.getOrDefault("paisCodigo", "CL");

        // Validaciones básicas de presencia
        if (documento == null || nombre == null || email == null || password == null) {
            throw new BadRequestResponse(
                "Campos requeridos: documentoIdentidad, nombre, email, password.");
        }

        // Valida el rol y bloquea la creación de un SUPERADMIN por esta vía
        Rol rol;
        try {
            rol = Rol.valueOf(rolStr);
            if (Rol.SUPERADMIN.equals(rol)) {
                throw new BadRequestResponse("No puedes crear un SUPERADMIN desde aquí.");
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Rol inválido: " + rolStr);
        }

        // Hashea la contraseña con BCrypt y un factor de costo de 12
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        // Inicializa el empleado a guardar
        Empleado nuevo = new Empleado(documento, tipoDoc, nombre, apellido,
                email, telefono, passwordHash, paisCodigo, barberiaIdToken, rol);
        
        // Registra al empleado en la DB
        int id = dao.registrarEmpleado(nuevo);

        ctx.status(201).json(Map.of("id", id, "mensaje", "Empleado registrado exitosamente."));
    }

    /**
     * Desactiva un empleado lógicamente. El barberia_id del JWT garantiza que el admin
     * solo puede desactivar empleados pertenecientes a SU propia barbería.
     * 
     * @param ctx Contexto HTTP de Javalin con el ID del empleado
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si no existe el empleado o faltan permisos
     */
    private static void desactivar(Context ctx) throws SQLException {
        // Exige permisos de administración de barbería
        SecurityMiddleware.requerirRol(ctx, Rol.ADMIN_BARBERIA);
        
        int empleadoId = Integer.parseInt(ctx.pathParam("id"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);

        // Desactiva si se cumplen ambas condiciones (mismo empleado y barbería correcta)
        boolean ok = dao.desactivar(empleadoId, barberiaId);
        if (!ok) throw new BadRequestResponse("Empleado no encontrado o sin permisos.");
        
        ctx.json(Map.of("mensaje", "Empleado desactivado correctamente."));
    }
}
