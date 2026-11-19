package com.mycompany.proyectobarberia.Logica;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reglas de horario de atención y disponibilidad de horas para reservar.
 *
 *   - Cada barbería define, por día de la semana (1 = lunes … 7 = domingo),
 *     si abre y en qué rango. Si no definió nada, se usa el horario por defecto:
 *     todos los días de 09:00 a 20:00.
 *   - Las horas se ofrecen cada {@link #INTERVALO_MINUTOS} minutos. Una hora está
 *     disponible si la cita completa (suma de duraciones de sus servicios) cabe antes
 *     del cierre, no se cruza con otra cita del mismo barbero y aún no pasó.
 *
 * Clase sin dependencias (lógica pura) para poder probarla de forma aislada.
 */
public final class Agenda {

    public static final int INTERVALO_MINUTOS = 30;
    public static final int DURACION_POR_DEFECTO = 30;
    public static final LocalTime APERTURA_POR_DEFECTO = LocalTime.of(9, 0);
    public static final LocalTime CIERRE_POR_DEFECTO = LocalTime.of(20, 0);

    /** Horario de un día de la semana. Si {@code abierto} es false, apertura y cierre son null. */
    public record HorarioDia(int diaSemana, boolean abierto, LocalTime apertura, LocalTime cierre) { }

    /** Una cita ya tomada: hora de inicio y duración total en minutos. */
    public record Ocupado(LocalTime inicio, int duracionMinutos) { }

    /** Una hora ofrecida al cliente. */
    public record Hora(String hora, boolean disponible) { }

    private Agenda() { }

    /** Horario por defecto: los 7 días de 09:00 a 20:00. */
    public static List<HorarioDia> horarioPorDefecto() {
        List<HorarioDia> dias = new ArrayList<>();
        for (int d = 1; d <= 7; d++) dias.add(new HorarioDia(d, true, APERTURA_POR_DEFECTO, CIERRE_POR_DEFECTO));
        return dias;
    }

    /**
     * Completa la semana: los días guardados reemplazan al horario por defecto.
     *
     * @param guardados Días que la barbería configuró (puede estar vacío).
     * @return Siempre 7 días, ordenados de lunes a domingo.
     */
    public static List<HorarioDia> completarSemana(List<HorarioDia> guardados) {
        Map<Integer, HorarioDia> porDia = new TreeMap<>();
        for (HorarioDia d : horarioPorDefecto()) porDia.put(d.diaSemana(), d);
        if (guardados != null) for (HorarioDia d : guardados) porDia.put(d.diaSemana(), d);
        return new ArrayList<>(porDia.values());
    }

    /**
     * Valida una semana enviada por el administrador.
     *
     * @return null si es válida, o el mensaje del problema.
     */
    public static String validar(List<HorarioDia> dias) {
        if (dias == null || dias.size() != 7) return "Envía el horario de los 7 días de la semana.";
        boolean[] vistos = new boolean[8];
        for (HorarioDia d : dias) {
            if (d.diaSemana() < 1 || d.diaSemana() > 7) return "Día de la semana inválido: " + d.diaSemana();
            if (vistos[d.diaSemana()]) return "El día " + nombreDia(d.diaSemana()) + " está repetido.";
            vistos[d.diaSemana()] = true;
            if (!d.abierto()) continue;
            if (d.apertura() == null || d.cierre() == null) {
                return "Indica la hora de apertura y de cierre del " + nombreDia(d.diaSemana()) + ".";
            }
            if (!d.apertura().isBefore(d.cierre())) {
                return "El " + nombreDia(d.diaSemana()) + " la hora de cierre debe ser posterior a la de apertura.";
            }
            if (d.apertura().getMinute() % INTERVALO_MINUTOS != 0 || d.cierre().getMinute() % INTERVALO_MINUTOS != 0) {
                return "Usa horas en punto o y media (ej: 09:00, 09:30) para el " + nombreDia(d.diaSemana()) + ".";
            }
        }
        return null;
    }

    /** Horario que corresponde a una fecha concreta. */
    public static HorarioDia horarioDe(List<HorarioDia> semana, LocalDate fecha) {
        int dia = fecha.getDayOfWeek().getValue();
        return completarSemana(semana).stream().filter(d -> d.diaSemana() == dia).findFirst().orElseThrow();
    }

    /**
     * Calcula las horas que se ofrecen para un día.
     *
     * @param dia         Horario del día.
     * @param fecha       Fecha consultada.
     * @param ocupados    Citas ya tomadas por el barbero ese día (no canceladas).
     * @param duracion    Duración de la cita a reservar en minutos.
     * @param ahora       Momento actual (las horas pasadas no se ofrecen).
     * @return Lista de horas, cada una marcada como disponible o no. Vacía si ese día no se atiende.
     */
    public static List<Hora> horas(HorarioDia dia, LocalDate fecha, List<Ocupado> ocupados,
                                   int duracion, LocalDateTime ahora) {
        List<Hora> horas = new ArrayList<>();
        if (!dia.abierto()) return horas;
        int dur = duracion > 0 ? duracion : DURACION_POR_DEFECTO;
        for (LocalTime t = dia.apertura(); t.isBefore(dia.cierre()); t = t.plusMinutes(INTERVALO_MINUTOS)) {
            horas.add(new Hora(t.toString(), estaLibre(dia, fecha, t, dur, ocupados, ahora)));
        }
        return horas;
    }

    /**
     * Indica si una cita que empieza a {@code inicio} y dura {@code duracion} minutos
     * puede reservarse: dentro del horario, en el futuro y sin cruzarse con otra cita.
     */
    public static boolean estaLibre(HorarioDia dia, LocalDate fecha, LocalTime inicio, int duracion,
                                    List<Ocupado> ocupados, LocalDateTime ahora) {
        if (!dia.abierto() || inicio.isBefore(dia.apertura())) return false;
        int dur = duracion > 0 ? duracion : DURACION_POR_DEFECTO;
        int ini = minutos(inicio);
        int fin = ini + dur;
        if (fin > minutos(dia.cierre())) return false;
        if (!LocalDateTime.of(fecha, inicio).isAfter(ahora)) return false;
        if (ocupados == null) return true;
        return ocupados.stream()
                .sorted(Comparator.comparing(Ocupado::inicio))
                .noneMatch(o -> {
                    int oIni = minutos(o.inicio());
                    int oFin = oIni + (o.duracionMinutos() > 0 ? o.duracionMinutos() : DURACION_POR_DEFECTO);
                    return ini < oFin && oIni < fin;
                });
    }

    /** Nombre del día en español (1 = lunes). */
    public static String nombreDia(int diaSemana) {
        return switch (DayOfWeek.of(diaSemana)) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miércoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }

    private static int minutos(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
