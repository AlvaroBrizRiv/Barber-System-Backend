package com.mycompany.proyectobarberia.API.dto;

import com.mycompany.proyectobarberia.Logica.enums.PeriodoReporte;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporteGananciasTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 16);

    @Test
    void rellenaLosDiasSinCitasConCero() {
        PeriodoReporte.Rango rango = PeriodoReporte.SEMANA.rango(LUNES);
        ReporteGanancias r = ReporteGanancias.construir(PeriodoReporte.SEMANA, rango, List.of(),
                List.of(new GananciaDia(LUNES.plusDays(2), 2, new BigDecimal("30000"))));

        assertEquals(7, r.serie().size());
        assertEquals(LUNES, r.serie().get(0).fecha());
        assertEquals(BigDecimal.ZERO, r.serie().get(0).total());
        assertEquals(new BigDecimal("30000"), r.serie().get(2).total());
        assertEquals(new BigDecimal("30000"), r.total());
        assertEquals(2, r.totalCitas());
    }

    @Test
    void sumaVariosDiasEIgnoraFechasFueraDelRango() {
        PeriodoReporte.Rango rango = PeriodoReporte.SEMANA.rango(LUNES);
        ReporteGanancias r = ReporteGanancias.construir(PeriodoReporte.SEMANA, rango, List.of(), List.of(
                new GananciaDia(LUNES, 1, new BigDecimal("15000.50")),
                new GananciaDia(LUNES.plusDays(6), 3, new BigDecimal("45000")),
                new GananciaDia(LUNES.minusDays(1), 9, new BigDecimal("999999")))); // fuera de rango

        assertEquals(new BigDecimal("60000.50"), r.total());
        assertEquals(4, r.totalCitas());
    }

    @Test
    void totalNuloSeTrataComoCero() {
        PeriodoReporte.Rango rango = PeriodoReporte.DIA.rango(LUNES);
        ReporteGanancias r = ReporteGanancias.construir(PeriodoReporte.DIA, rango, List.of(),
                List.of(new GananciaDia(LUNES, 1, null)));
        assertEquals(BigDecimal.ZERO, r.total());
        assertEquals(BigDecimal.ZERO, r.serie().get(0).total());
    }

    @Test
    void sinDatosDevuelveCeros() {
        PeriodoReporte.Rango rango = PeriodoReporte.MES.rango(LUNES);
        ReporteGanancias r = ReporteGanancias.construir(PeriodoReporte.MES, rango, List.of(), List.of());
        assertEquals(30, r.serie().size());
        assertEquals(BigDecimal.ZERO, r.total());
        assertEquals(0, r.totalCitas());
        assertTrue(r.porBarbero().isEmpty());
    }

    @Test
    void desglosePorBarberoEsInmutable() {
        PeriodoReporte.Rango rango = PeriodoReporte.DIA.rango(LUNES);
        ReporteGanancias r = ReporteGanancias.construir(PeriodoReporte.DIA, rango,
                List.of(new GananciaBarbero(3, "Juan", "Pérez", 1, BigDecimal.TEN)), List.of());
        assertThrows(UnsupportedOperationException.class, () -> r.porBarbero().clear());
    }
}
