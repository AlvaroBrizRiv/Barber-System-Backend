package com.mycompany.proyectobarberia.API.Controladores;

import com.mycompany.proyectobarberia.API.dto.GananciaBarbero;
import com.mycompany.proyectobarberia.API.dto.GananciaDia;
import com.mycompany.proyectobarberia.API.dto.ReporteGanancias;
import com.mycompany.proyectobarberia.Logica.enums.PeriodoReporte;
import com.mycompany.proyectobarberia.Logica.enums.Rol;
import com.mycompany.proyectobarberia.Persistencia.DAO.GananciaDAO;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GananciaControllerTest {

    private GananciaDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        dao = mock(GananciaDAO.class);
        when(dao.porBarbero(anyInt(), any(), any(), any())).thenReturn(List.of());
        when(dao.serieDiaria(anyInt(), any(), any(), any())).thenReturn(List.of());
        GananciaController.setDao(dao);
    }

    @AfterEach
    void tearDown() {
        GananciaController.setDao(new GananciaDAO());
    }

    private static Context ctx(int userId, Rol rol, Integer barberiaId) {
        Context ctx = mock(Context.class);
        when(ctx.<Integer>attribute("userId")).thenReturn(userId);
        when(ctx.<Rol>attribute("rol")).thenReturn(rol);
        when(ctx.<Integer>attribute("barberiaId")).thenReturn(barberiaId);
        return ctx;
    }

    @Test
    void barberoVeSoloSusGananciasAunqueEnvieOtroId() throws Exception {
        Context c = ctx(4, Rol.BARBERO, 1);
        when(c.queryParam("barberoId")).thenReturn("99"); // intento de ver a otro barbero: se ignora
        when(c.queryParam("periodo")).thenReturn("DIA");
        when(c.queryParam("fecha")).thenReturn("2026-11-17");

        GananciaController.mias(c);

        LocalDate f = LocalDate.of(2026, 11, 17);
        verify(dao).porBarbero(1, f, f, 4);
        verify(dao).serieDiaria(1, f, f, 4);
    }

    @Test
    void clienteNoPuedeVerGanancias() {
        assertThrows(ForbiddenResponse.class, () -> GananciaController.mias(ctx(5, Rol.CLIENTE, null)));
        verifyNoInteractions(dao);
    }

    @Test
    void barberoNoPuedeVerReporteDeLaBarberia() {
        Context c = ctx(4, Rol.BARBERO, 1);
        when(c.pathParam("id")).thenReturn("1");
        assertThrows(ForbiddenResponse.class, () -> GananciaController.deBarberia(c));
        verifyNoInteractions(dao);
    }

    @Test
    void adminNoPuedeVerOtraBarberia() {
        Context c = ctx(2, Rol.ADMIN_BARBERIA, 1);
        when(c.pathParam("id")).thenReturn("2");
        assertThrows(ForbiddenResponse.class, () -> GananciaController.deBarberia(c));
        verifyNoInteractions(dao);
    }

    @Test
    void adminConsultaSemanaYRecibeTotales() throws Exception {
        Context c = ctx(2, Rol.ADMIN_BARBERIA, 1);
        when(c.pathParam("id")).thenReturn("1");
        when(c.queryParam("periodo")).thenReturn("semana");
        when(c.queryParam("fecha")).thenReturn("2026-11-19"); // jueves
        LocalDate lunes = LocalDate.of(2026, 11, 16);
        LocalDate domingo = LocalDate.of(2026, 11, 22);
        when(dao.porBarbero(1, lunes, domingo, null)).thenReturn(
                List.of(new GananciaBarbero(3, "Juan", "Pérez", 2, new BigDecimal("40000"))));
        when(dao.serieDiaria(1, lunes, domingo, null)).thenReturn(
                List.of(new GananciaDia(lunes, 2, new BigDecimal("40000"))));

        GananciaController.deBarberia(c);

        ArgumentCaptor<Object> json = ArgumentCaptor.forClass(Object.class);
        verify(c).json(json.capture());
        ReporteGanancias r = (ReporteGanancias) json.getValue();
        assertEquals(PeriodoReporte.SEMANA, r.periodo());
        assertEquals(lunes, r.desde());
        assertEquals(domingo, r.hasta());
        assertEquals(new BigDecimal("40000"), r.total());
        assertEquals(2, r.totalCitas());
        assertEquals(7, r.serie().size());
        assertEquals(1, r.porBarbero().size());
    }

    @Test
    void adminPuedeFiltrarPorBarbero() throws Exception {
        Context c = ctx(2, Rol.ADMIN_BARBERIA, 1);
        when(c.pathParam("id")).thenReturn("1");
        when(c.queryParam("barberoId")).thenReturn("3");
        when(c.queryParam("periodo")).thenReturn("MES");
        when(c.queryParam("fecha")).thenReturn("2026-02-10");

        GananciaController.deBarberia(c);

        verify(dao).porBarbero(1, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), 3);
    }

    @Test
    void periodoInvalidoEs400() {
        Context c = ctx(4, Rol.BARBERO, 1);
        when(c.queryParam("periodo")).thenReturn("ANUAL");
        assertThrows(BadRequestResponse.class, () -> GananciaController.mias(c));
    }

    @Test
    void fechaInvalidaEs400() {
        Context c = ctx(4, Rol.BARBERO, 1);
        when(c.queryParam("fecha")).thenReturn("17-11-2026");
        assertThrows(BadRequestResponse.class, () -> GananciaController.mias(c));
    }

    @Test
    void sinParametrosUsaHoyYDia() throws Exception {
        GananciaController.mias(ctx(4, Rol.BARBERO, 1));
        LocalDate hoy = LocalDate.now();
        verify(dao).porBarbero(1, hoy, hoy, 4);
    }
}
