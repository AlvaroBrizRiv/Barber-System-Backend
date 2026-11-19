package com.mycompany.proyectobarberia.Logica;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgendaTest {

    private static final LocalDate LUNES = LocalDate.of(2030, 1, 7);
    private static final LocalDateTime ANTES = LUNES.minusDays(1).atStartOfDay();
    private static final Agenda.HorarioDia DIA = new Agenda.HorarioDia(1, true, LocalTime.of(10, 0), LocalTime.of(12, 0));

    private static List<String> libres(List<Agenda.Hora> horas) {
        return horas.stream().filter(Agenda.Hora::disponible).map(Agenda.Hora::hora).toList();
    }

    @Test
    void porDefectoAtiendeLosSieteDiasDeNueveAVeinte() {
        List<Agenda.HorarioDia> semana = Agenda.completarSemana(List.of());
        assertEquals(7, semana.size());
        assertTrue(semana.stream().allMatch(d -> d.abierto()
                && d.apertura().equals(LocalTime.of(9, 0)) && d.cierre().equals(LocalTime.of(20, 0))));
    }

    @Test
    void losDiasGuardadosReemplazanAlDefecto() {
        Agenda.HorarioDia domingo = new Agenda.HorarioDia(7, false, null, null);
        List<Agenda.HorarioDia> semana = Agenda.completarSemana(List.of(domingo));
        assertFalse(semana.get(6).abierto());
        assertFalse(Agenda.horarioDe(semana, LUNES.plusDays(6)).abierto());
        assertTrue(Agenda.horarioDe(semana, LUNES).abierto());
    }

    @Test
    void ofreceHorasCadaMediaHoraDentroDelHorario() {
        List<Agenda.Hora> horas = Agenda.horas(DIA, LUNES, List.of(), 30, ANTES);
        assertEquals(List.of("10:00", "10:30", "11:00", "11:30"), libres(horas));
    }

    @Test
    void laCitaDebeTerminarAntesDelCierre() {
        List<Agenda.Hora> horas = Agenda.horas(DIA, LUNES, List.of(), 60, ANTES);
        assertEquals(List.of("10:00", "10:30", "11:00"), libres(horas));
        assertEquals(4, horas.size(), "las horas que no alcanzan se muestran como no disponibles");
    }

    @Test
    void unaCitaLargaBloqueaLasHorasQueOcupa() {
        List<Agenda.Ocupado> ocupados = List.of(new Agenda.Ocupado(LocalTime.of(10, 30), 60));
        List<Agenda.Hora> horas = Agenda.horas(DIA, LUNES, ocupados, 30, ANTES);
        assertEquals(List.of("10:00", "11:30"), libres(horas));
        // Con una cita de 60 min, 10:00 ya no alcanza (se cruzaría con la de 10:30)
        assertEquals(List.of(), libres(Agenda.horas(DIA, LUNES, ocupados, 60, ANTES)).stream()
                .filter(h -> h.equals("10:00")).toList());
    }

    @Test
    void noOfreceHorasQueYaPasaron() {
        LocalDateTime ahora = LUNES.atTime(10, 45);
        assertEquals(List.of("11:00", "11:30"), libres(Agenda.horas(DIA, LUNES, List.of(), 30, ahora)));
    }

    @Test
    void unDiaCerradoNoOfreceHoras() {
        Agenda.HorarioDia cerrado = new Agenda.HorarioDia(1, false, null, null);
        assertTrue(Agenda.horas(cerrado, LUNES, List.of(), 30, ANTES).isEmpty());
        assertFalse(Agenda.estaLibre(cerrado, LUNES, LocalTime.of(10, 0), 30, List.of(), ANTES));
    }

    @Test
    void validarAceptaUnaSemanaCorrecta() {
        assertNull(Agenda.validar(Agenda.horarioPorDefecto()));
    }

    @Test
    void validarRechazaCierreAntesDeApertura() {
        List<Agenda.HorarioDia> semana = new ArrayList<>(Agenda.horarioPorDefecto());
        semana.set(0, new Agenda.HorarioDia(1, true, LocalTime.of(18, 0), LocalTime.of(9, 0)));
        assertNotNull(Agenda.validar(semana));
    }

    @Test
    void validarRechazaDiasFaltantesORepetidos() {
        List<Agenda.HorarioDia> seis = new ArrayList<>(Agenda.horarioPorDefecto().subList(0, 6));
        assertNotNull(Agenda.validar(seis));
        seis.add(seis.get(0));
        assertNotNull(Agenda.validar(seis));
    }

    @Test
    void validarExigeHorasEnPuntoOYMedia() {
        List<Agenda.HorarioDia> semana = new ArrayList<>(Agenda.horarioPorDefecto());
        semana.set(2, new Agenda.HorarioDia(3, true, LocalTime.of(9, 15), LocalTime.of(18, 0)));
        assertNotNull(Agenda.validar(semana));
    }
}
