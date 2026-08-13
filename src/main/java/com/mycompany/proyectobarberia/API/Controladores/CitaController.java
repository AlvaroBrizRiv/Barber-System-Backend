package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Cita;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.enums.EstadoCita;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.CitaDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para citas/reservas.
 *
 * GET  /api/citas/barberia/{id}?fecha=YYYY-MM-DD → Agenda de la barbería (ADMIN/BARBERO).
 * GET  /api/citas/barbero/{id}?fecha=YYYY-MM-DD  → Agenda del barbero (BARBERO/ADMIN).
 * GET  /api/citas/mis-citas                      → Historial del cliente autenticado.
 * POST /api/citas                                → Agendar nueva cita (CLIENTE).
 * PUT  /api/citas/{id}/estado                    → Cambiar estado (ADMIN/BARBERO).
 */
public class CitaController {

    private static final CitaDAO dao = new CitaDAO();

    public static void registrarRutas() {
        ApiBuilder.get("/barberia/{barberiaId}", CitaController::agendaBarberia);
        ApiBuilder.get("/barbero/{barberoId}",   CitaController::agendaBarbero);
        ApiBuilder.get("/mis-citas",             CitaController::misCitas);
        ApiBuilder.post("/",                     CitaController::agendar);
        ApiBuilder.put("/{id}/estado",           CitaController::cambiarEstado);
    }

    /**
     * Obtiene la agenda de una barbería para una fecha específica.
     * SEGURIDAD: solo el admin o barbero de ESA barbería puede verla.
     * 
     * @param ctx Contexto HTTP de Javalin, que incluye el ID de la barbería en la URL y la fecha en query string
     * @throws SQLException si ocurre un error en la base de datos
     * @throws ForbiddenResponse si el usuario no tiene permisos sobre la barbería
     */
    private static void agendaBarberia(Context ctx) throws SQLException {
        // Extrae el ID de la barbería desde la URL
        int barberiaId = Integer.parseInt(ctx.pathParam("barberiaId"));
        
        // Verifica que el empleado autenticado pertenezca a esta barbería
        SecurityMiddleware.requerirEmpleadoDeBarberia(ctx, barberiaId);

        // Parsea la fecha o usa la de hoy si no se envía
        LocalDate fecha = parseFecha(ctx.queryParam("fecha"));
        
        // Ejecuta la consulta y retorna la lista de citas en formato JSON
        List<Cita> citas = dao.buscarPorBarberiaYFecha(barberiaId, fecha);
        ctx.json(citas);
    }

    /**
     * Obtiene la agenda personal de un barbero para una fecha.
     * El barbero solo puede ver su propia agenda (verificado por userId del JWT).
     * 
     * @param ctx Contexto HTTP de Javalin con el ID del barbero y la fecha opcional
     * @throws SQLException si ocurre un error en la base de datos
     * @throws ForbiddenResponse si el usuario intenta ver la agenda de otro barbero sin ser ADMIN
     */
    private static void agendaBarbero(Context ctx) throws SQLException {
        int barberoId  = Integer.parseInt(ctx.pathParam("barberoId"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);
        Rol rol = SecurityMiddleware.getRol(ctx);

        // Un BARBERO solo puede ver SU agenda; el ADMIN puede ver cualquier barbero de SU barbería
        if (Rol.BARBERO.equals(rol)) {
            int userId = SecurityMiddleware.getUserId(ctx);
            // Bloquea el acceso si el barbero intenta consultar la agenda de otro
            if (userId != barberoId) {
                throw new ForbiddenResponse("Solo puedes ver tu propia agenda.");
            }
        } else if (Rol.ADMIN_BARBERIA.equals(rol)) {
            // Admin puede ver cualquier barbero de su barbería (verificado a nivel SQL)
        } else if (!Rol.SUPERADMIN.equals(rol)) {
            // Cualquier otro rol (como CLIENTE) tiene el acceso denegado
            throw new ForbiddenResponse("Acceso denegado.");
        }

        // Parsea la fecha y busca las citas correspondientes
        LocalDate fecha = parseFecha(ctx.queryParam("fecha"));
        List<Cita> citas = dao.buscarPorBarberoYFecha(barberoId, barberiaId, fecha);
        ctx.json(citas);
    }

    /**
     * Obtiene el historial de citas del cliente autenticado.
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws ForbiddenResponse si el usuario no tiene rol de CLIENTE
     */
    private static void misCitas(Context ctx) throws SQLException {
        // Exige el rol CLIENTE y obtiene el ID del usuario actual
        SecurityMiddleware.requerirRol(ctx, Rol.CLIENTE);
        int clienteId = SecurityMiddleware.getUserId(ctx);
        
        // Retorna las citas asociadas a este cliente
        ctx.json(dao.buscarPorCliente(clienteId));
    }

    /**
     * Agenda una nueva cita. Acción exclusiva para clientes autenticados.
     * Body esperado: { "barberiaId": 1, "empleadoId": 2, "fecha": "2026-08-10", "hora": "10:30", "notas": "..." }
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si faltan campos obligatorios o hay formatos erróneos
     */
    @SuppressWarnings("unchecked")
    private static void agendar(Context ctx) throws SQLException {
        // Verifica que el usuario sea un CLIENTE
        SecurityMiddleware.requerirRol(ctx, Rol.CLIENTE);
        int clienteId = SecurityMiddleware.getUserId(ctx);

        // Extrae los datos del cuerpo de la petición
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int barberiaId = ((Number) body.get("barberiaId")).intValue();
        int empleadoId = ((Number) body.get("empleadoId")).intValue();
        String fechaStr = (String) body.get("fecha");
        String horaStr  = (String) body.get("hora");
        String notas    = (String) body.getOrDefault("notas", "");

        // Valida que la fecha y la hora estén presentes
        if (fechaStr == null || horaStr == null) {
            throw new BadRequestResponse("Fecha y hora son requeridos.");
        }

        // Construir objetos mínimos para la FK (sin datos completos, solo IDs)
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        Empleado empleado = new Empleado();
        empleado.setId(empleadoId);

        // Inicializa la nueva cita con la información proveída
        Cita cita = new Cita(barberiaId, cliente, empleado,
                LocalDate.parse(fechaStr),
                java.time.LocalTime.parse(horaStr),
                notas);

        // Registra la cita y devuelve un mensaje de éxito
        int id = dao.registrarCita(cita);
        ctx.status(201).json(Map.of("id", id, "mensaje", "Cita agendada exitosamente."));
    }

    /**
     * Cambia el estado de una cita (CONFIRMADA, CANCELADA, COMPLETADA).
     * Solo el admin o barbero de la barbería correspondiente puede hacerlo.
     * Body esperado: { "estado": "CONFIRMADA" }
     * 
     * @param ctx Contexto HTTP de Javalin con el ID de la cita en la URL
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si el estado provisto es inválido o no existe la cita
     */
    @SuppressWarnings("unchecked")
    private static void cambiarEstado(Context ctx) throws SQLException {
        // Obtiene el ID de la cita y la barbería del empleado autenticado
        int citaId = Integer.parseInt(ctx.pathParam("id"));
        Integer barberiaId = SecurityMiddleware.getBarberiaId(ctx);
        SecurityMiddleware.requerirEmpleadoDeBarberia(ctx, barberiaId);

        // Extrae el nuevo estado
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String estadoStr = body.get("estado");

        EstadoCita nuevoEstado;
        try {
            // Intenta convertir el string al enum de estado
            nuevoEstado = EstadoCita.valueOf(estadoStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Estado inválido: " + estadoStr);
        }

        // Ejecuta el cambio de estado; si retorna false, falla la operación
        boolean actualizado = dao.cambiarEstado(citaId, barberiaId, nuevoEstado);
        if (!actualizado) {
            throw new BadRequestResponse("No se pudo actualizar el estado. Verifica el ID y los permisos.");
        }

        ctx.json(Map.of("mensaje", "Estado de la cita actualizado a " + nuevoEstado.name()));
    }

    /**
     * Convierte una cadena de texto a LocalDate. Si no se provee fecha, utiliza la actual.
     * 
     * @param fechaStr Cadena de fecha en formato YYYY-MM-DD
     * @return Objeto LocalDate correspondiente
     * @throws BadRequestResponse si el formato es inválido
     */
    private static LocalDate parseFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(fechaStr);
        } catch (Exception e) {
            throw new BadRequestResponse("Formato de fecha inválido. Use YYYY-MM-DD.");
        }
    }
}
