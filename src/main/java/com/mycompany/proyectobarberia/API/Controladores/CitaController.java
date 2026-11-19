package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Cita;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;
import com.mycompany.proyectobarberia.Logica.enums.EstadoCita;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.CitaDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private static final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    private static int leerEntero(Map<String, Object> body, String clave, boolean obligatorio, int porDefecto) {
        Object v = body.get(clave);
        if (v == null) {
            if (obligatorio) throw new BadRequestResponse("Falta el campo: " + clave);
            return porDefecto;
        }
        if (!(v instanceof Number n)) throw new BadRequestResponse("El campo " + clave + " debe ser un número.");
        return n.intValue();
    }

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
        // Exige el rol CLIENTE de manera exacta y obtiene el ID del usuario actual
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId = SecurityMiddleware.getUserId(ctx);
        
        // Retorna las citas asociadas a este cliente
        ctx.json(dao.buscarPorCliente(clienteId));
    }

    /**
     * Agenda una nueva cita. Acción exclusiva para clientes autenticados.
     * Body esperado: { "barberiaId": 1, "empleadoId": 2, "fecha": "2026-08-10", "hora": "10:30", "servicios": [1, 2], "notas": "..." }
     * 
     * @param ctx Contexto HTTP de Javalin
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si faltan campos obligatorios o hay formatos erróneos
     */
    @SuppressWarnings("unchecked")
    private static void agendar(Context ctx) throws SQLException {
        // Verifica que el usuario sea exactamente CLIENTE
        SecurityMiddleware.requerirRolExacto(ctx, Rol.CLIENTE);
        int clienteId = SecurityMiddleware.getUserId(ctx);

        // Extrae los datos del cuerpo de la petición con parseo seguro (SEC-09)
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int barberiaId = leerEntero(body, "barberiaId", true, 0);
        int empleadoId = leerEntero(body, "empleadoId", true, 0);
        String fechaStr = (String) body.get("fecha");
        String horaStr  = (String) body.get("hora");
        String notas    = (String) body.getOrDefault("notas", "");

        // Valida que la fecha y la hora estén presentes
        if (fechaStr == null || horaStr == null) {
            throw new BadRequestResponse("Fecha y hora son requeridos.");
        }

        LocalDate fecha;
        LocalTime hora;
        try {
            fecha = LocalDate.parse(fechaStr);
            hora  = LocalTime.parse(horaStr);
        } catch (Exception e) {
            throw new BadRequestResponse("Formato de fecha u hora inválido. Use YYYY-MM-DD y HH:mm.");
        }

        // SEC-04: Validar que la cita sea futura
        if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
            throw new BadRequestResponse("La cita debe ser programada en una fecha y hora futura.");
        }

        // SEC-04: Validar que el barbero exista, esté activo y pertenezca a la barbería seleccionada
        if (!empleadoDAO.esBarberoActivoDe(empleadoId, barberiaId)) {
            throw new BadRequestResponse("El barbero seleccionado no es válido o no pertenece a esta barbería.");
        }

        // BUG-11: Validar que se haya seleccionado al menos un servicio
        List<Number> idsRaw = (List<Number>) body.get("servicios");
        if (idsRaw == null || idsRaw.isEmpty()) {
            throw new BadRequestResponse("Selecciona al menos un servicio para la cita.");
        }
        List<Integer> servicioIds = idsRaw.stream().map(Number::intValue).toList();

        // La hora debe estar dentro del horario de la barbería y no cruzarse con otra cita
        HorarioController.validarReserva(barberiaId, empleadoId, fecha, hora, servicioIds);

        // Construir objetos mínimos para la FK (sin datos completos, solo IDs)
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        Empleado empleado = new Empleado();
        empleado.setId(empleadoId);

        // Inicializa la nueva cita con la información proveída
        Cita cita = new Cita(barberiaId, cliente, empleado, fecha, hora, notas);

        // Registra la cita y sus servicios calculando el total dentro de la transacción
        int id;
        try {
            id = dao.registrarCita(cita, servicioIds);
        } catch (SQLException e) {
            // Manejo de violación de índice único anti-doble reserva (SEC-04)
            if (e.getErrorCode() == 2601 || e.getErrorCode() == 2627) {
                throw new ConflictResponse("El horario seleccionado ya no está disponible con este barbero.");
            }
            throw e;
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse(e.getMessage());
        }

        ctx.status(201).json(Map.of("id", id, "mensaje", "Cita agendada exitosamente."));
    }

    /**
     * Cambia el estado de una cita (CONFIRMADA, CANCELADA, COMPLETADA).
     * Solo el admin de la barbería o el barbero asignado a la cita puede hacerlo.
     * Body esperado: { "estado": "CONFIRMADA" }
     * 
     * @param ctx Contexto HTTP de Javalin con el ID de la cita en la URL
     * @throws SQLException si ocurre un error en la base de datos
     * @throws BadRequestResponse si el estado provisto es inválido o no existe la cita
     */
    @SuppressWarnings("unchecked")
    private static void cambiarEstado(Context ctx) throws SQLException {
        // Exige rol BARBERO o ADMIN_BARBERIA y obtiene el barberiaId estricto
        SecurityMiddleware.requerirRolExacto(ctx, Rol.BARBERO, Rol.ADMIN_BARBERIA);
        int barberiaId = SecurityMiddleware.requerirBarberiaId(ctx);
        int citaId = Integer.parseInt(ctx.pathParam("id"));

        // Extrae el nuevo estado
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String estadoStr = body.get("estado");

        EstadoCita nuevoEstado;
        try {
            nuevoEstado = EstadoCita.valueOf(estadoStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Estado inválido: " + estadoStr);
        }

        // SEC-03: Si el usuario es BARBERO, solo puede modificar citas que tenga asignadas
        Integer soloEmpleadoId = Rol.BARBERO.equals(SecurityMiddleware.getRol(ctx)) ? SecurityMiddleware.getUserId(ctx) : null;

        // Ejecuta el cambio de estado con restricción por tenant y por barbero si aplica
        boolean actualizado = dao.cambiarEstado(citaId, barberiaId, nuevoEstado, soloEmpleadoId);
        if (!actualizado) {
            throw new NotFoundResponse("No se pudo actualizar el estado. Cita no encontrada o sin permisos.");
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
