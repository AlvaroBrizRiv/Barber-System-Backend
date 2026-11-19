package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.Seguridad.SecurityMiddleware;
import com.mycompany.proyectobarberia.Logica.Agenda;
import com.mycompany.proyectobarberia.Persistencia.DAO.BarberiaDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import com.mycompany.proyectobarberia.Persistencia.DAO.HorarioDAO;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.ConflictResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Horario de atención de cada barbería y horas disponibles para reservar.
 * Las rutas se registran dentro de /api/barberias (ver {@link BarberiaController}).
 *
 * GET /api/barberias/{id}/horarios                         → Horario semanal (público).
 * PUT /api/barberias/{id}/horarios                         → Cambiar horario (ADMIN de esa barbería).
 * GET /api/barberias/{id}/disponibilidad?barberoId=&fecha=&servicios=1,2 → Horas del día (público).
 */
public class HorarioController {

    private static final HorarioDAO dao = new HorarioDAO();
    private static final BarberiaDAO barberiaDAO = new BarberiaDAO();
    private static final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    /** Máximo de días hacia adelante que se pueden consultar o reservar. */
    static final int DIAS_MAXIMOS_ADELANTE = 90;

    private HorarioController() { }

    static void obtenerHorario(Context ctx) throws SQLException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        if (barberiaDAO.buscarPorId(id) == null) throw new NotFoundResponse("Barbería no encontrada.");
        ctx.json(Map.of("dias", dao.semana(id)));
    }

    /**
     * Body: { "dias": [ { "diaSemana": 1, "abierto": true, "apertura": "09:00", "cierre": "20:00" }, … 7 días ] }
     */
    @SuppressWarnings("unchecked")
    static void guardarHorario(Context ctx) throws SQLException {
        int id = Integer.parseInt(ctx.pathParam("id"));
        SecurityMiddleware.requerirAdminDeBarberia(ctx, id);

        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        List<Agenda.HorarioDia> dias = leerDias(body);
        String error = Agenda.validar(dias);
        if (error != null) throw new BadRequestResponse(error);

        dao.guardar(id, dias);
        ctx.json(Map.of("mensaje", "Horario actualizado.", "dias", dias));
    }

    static void disponibilidad(Context ctx) throws SQLException {
        int barberiaId = Integer.parseInt(ctx.pathParam("id"));
        int barberoId = enteroObligatorio(ctx.queryParam("barberoId"), "barberoId");
        LocalDate fecha = leerFecha(ctx.queryParam("fecha"));
        List<Integer> servicios = leerIds(ctx.queryParam("servicios"));

        if (!empleadoDAO.esBarberoActivoDe(barberoId, barberiaId)) {
            throw new NotFoundResponse("El barbero no pertenece a esta barbería.");
        }

        Agenda.HorarioDia dia = Agenda.horarioDe(dao.semana(barberiaId), fecha);
        int duracion = duracionDe(barberiaId, servicios);
        List<Agenda.Hora> horas = Agenda.horas(dia, fecha, dao.ocupados(barberoId, fecha), duracion, LocalDateTime.now());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("fecha", fecha.toString());
        resp.put("abierto", dia.abierto());
        resp.put("apertura", dia.apertura() != null ? dia.apertura().toString() : null);
        resp.put("cierre", dia.cierre() != null ? dia.cierre().toString() : null);
        resp.put("duracionMinutos", duracion);
        resp.put("horas", horas);
        ctx.json(resp);
    }

    /**
     * Comprueba, antes de guardar una cita, que la hora esté dentro del horario de la
     * barbería y no se cruce con otra cita del mismo barbero.
     *
     * @throws BadRequestResponse si la barbería no atiende a esa hora
     * @throws ConflictResponse   si la hora ya fue tomada
     */
    static void validarReserva(int barberiaId, int barberoId, LocalDate fecha, LocalTime hora,
                               List<Integer> servicios) throws SQLException {
        if (fecha.isAfter(LocalDate.now().plusDays(DIAS_MAXIMOS_ADELANTE))) {
            throw new BadRequestResponse("Solo puedes reservar hasta " + DIAS_MAXIMOS_ADELANTE + " días hacia adelante.");
        }
        Agenda.HorarioDia dia = Agenda.horarioDe(dao.semana(barberiaId), fecha);
        if (!dia.abierto()) {
            throw new BadRequestResponse("La barbería no atiende el " + Agenda.nombreDia(dia.diaSemana()) + ".");
        }
        int duracion = duracionDe(barberiaId, servicios);
        if (hora.isBefore(dia.apertura()) || hora.plusMinutes(duracion).isAfter(dia.cierre())
                || hora.plusMinutes(duracion).isBefore(hora)) {
            throw new BadRequestResponse("Ese día la barbería atiende de " + dia.apertura() + " a " + dia.cierre()
                    + " y la cita dura " + duracion + " min. Elige otra hora.");
        }
        if (!Agenda.estaLibre(dia, fecha, hora, duracion, dao.ocupados(barberoId, fecha), LocalDateTime.now())) {
            throw new ConflictResponse("Esa hora ya no está disponible con este barbero. Elige otra.");
        }
    }

    private static int duracionDe(int barberiaId, List<Integer> servicios) throws SQLException {
        int d = dao.duracionServicios(barberiaId, servicios);
        return d > 0 ? d : Agenda.DURACION_POR_DEFECTO;
    }

    // ─── Lectura de parámetros (sin base de datos, se prueban de forma aislada) ───

    @SuppressWarnings("unchecked")
    static List<Agenda.HorarioDia> leerDias(Map<String, Object> body) {
        Object raw = body == null ? null : body.get("dias");
        if (!(raw instanceof List<?> lista)) throw new BadRequestResponse("El campo 'dias' (lista de 7 días) es requerido.");
        List<Agenda.HorarioDia> dias = new ArrayList<>();
        for (Object o : lista) {
            if (!(o instanceof Map<?, ?> m)) throw new BadRequestResponse("Cada día debe ser un objeto.");
            Object dia = m.get("diaSemana");
            if (!(dia instanceof Number n)) throw new BadRequestResponse("Cada día necesita 'diaSemana' (1 = lunes … 7 = domingo).");
            boolean abierto = !Boolean.FALSE.equals(m.get("abierto"));
            LocalTime apertura = abierto ? leerHora(m.get("apertura")) : null;
            LocalTime cierre = abierto ? leerHora(m.get("cierre")) : null;
            dias.add(new Agenda.HorarioDia(n.intValue(), abierto, apertura, cierre));
        }
        return dias;
    }

    private static LocalTime leerHora(Object valor) {
        if (!(valor instanceof String s) || s.isBlank()) return null;
        try {
            return LocalTime.parse(s.length() == 5 ? s : s.substring(0, Math.min(5, s.length())));
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse("Hora inválida: " + s + ". Usa el formato HH:mm.");
        }
    }

    static LocalDate leerFecha(String valor) {
        if (valor == null || valor.isBlank()) throw new BadRequestResponse("El parámetro 'fecha' (YYYY-MM-DD) es requerido.");
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new BadRequestResponse("Formato de fecha inválido. Use YYYY-MM-DD.");
        }
        if (fecha.isAfter(LocalDate.now().plusDays(DIAS_MAXIMOS_ADELANTE))) {
            throw new BadRequestResponse("Solo se puede consultar hasta " + DIAS_MAXIMOS_ADELANTE + " días hacia adelante.");
        }
        return fecha;
    }

    static List<Integer> leerIds(String valor) {
        List<Integer> ids = new ArrayList<>();
        if (valor == null || valor.isBlank()) return ids;
        for (String parte : valor.split(",")) {
            if (parte.isBlank()) continue;
            ids.add(enteroObligatorio(parte.trim(), "servicios"));
            if (ids.size() > 20) throw new BadRequestResponse("Demasiados servicios.");
        }
        return ids;
    }

    private static int enteroObligatorio(String valor, String nombre) {
        if (valor == null || valor.isBlank()) throw new BadRequestResponse("El parámetro '" + nombre + "' es requerido.");
        try {
            int n = Integer.parseInt(valor);
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("El parámetro '" + nombre + "' debe ser un número positivo.");
        }
    }
}
