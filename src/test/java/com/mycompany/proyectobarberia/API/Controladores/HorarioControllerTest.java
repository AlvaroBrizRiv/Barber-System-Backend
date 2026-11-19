package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.Logica.Agenda;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Validaciones de horarios que ocurren antes de tocar la base de datos. */
class HorarioControllerTest {

    @Test
    void soloElAdminDeLaBarberiaPuedeCambiarElHorario() {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("1");
        when(ctx.<Integer>attribute("userId")).thenReturn(5);
        when(ctx.<Rol>attribute("rol")).thenReturn(Rol.ADMIN_BARBERIA);
        when(ctx.<Integer>attribute("barberiaId")).thenReturn(2);
        assertThrows(ForbiddenResponse.class, () -> HorarioController.guardarHorario(ctx));
    }

    @Test
    void leerDiasInterpretaAbiertoYCerrado() {
        List<Agenda.HorarioDia> dias = HorarioController.leerDias(Map.of("dias", List.of(
                Map.of("diaSemana", 1, "abierto", true, "apertura", "10:00", "cierre", "19:30"),
                Map.of("diaSemana", 7, "abierto", false))));
        assertEquals(new Agenda.HorarioDia(1, true, LocalTime.of(10, 0), LocalTime.of(19, 30)), dias.get(0));
        assertEquals(new Agenda.HorarioDia(7, false, null, null), dias.get(1));
    }

    @Test
    void leerDiasAceptaHorasConSegundos() {
        List<Agenda.HorarioDia> dias = HorarioController.leerDias(Map.of("dias", List.of(
                Map.of("diaSemana", 2, "apertura", "09:00:00", "cierre", "18:00:00"))));
        assertEquals(LocalTime.of(9, 0), dias.get(0).apertura());
    }

    @Test
    void leerDiasRechazaCuerposInvalidos() {
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerDias(Map.of()));
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerDias(Map.of("dias", List.of(Map.of("abierto", true)))));
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerDias(Map.of("dias", List.of(
                Map.of("diaSemana", 1, "apertura", "9am", "cierre", "18:00")))));
    }

    @Test
    void leerFechaExigeFormatoYLimite() {
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerFecha(null));
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerFecha("07/01/2030"));
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerFecha(LocalDate.now().plusDays(365).toString()));
        assertEquals(LocalDate.now(), HorarioController.leerFecha(LocalDate.now().toString()));
    }

    @Test
    void leerIdsSeparaPorComas() {
        assertEquals(List.of(1, 2, 3), HorarioController.leerIds("1, 2,3"));
        assertTrue(HorarioController.leerIds(null).isEmpty());
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerIds("1,abc"));
        assertThrows(BadRequestResponse.class, () -> HorarioController.leerIds("-4"));
    }
}
