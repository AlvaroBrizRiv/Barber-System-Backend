package com.mycompany.proyectobarberia.Logica.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PeriodoReporteTest {

    @Test
    void diaDevuelveSoloLaFecha() {
        LocalDate f = LocalDate.of(2026, 11, 17);
        PeriodoReporte.Rango r = PeriodoReporte.DIA.rango(f);
        assertEquals(f, r.desde());
        assertEquals(f, r.hasta());
    }

    @Test
    void semanaVaDeLunesADomingo() {
        // 2026-11-17 es martes
        PeriodoReporte.Rango r = PeriodoReporte.SEMANA.rango(LocalDate.of(2026, 11, 17));
        assertEquals(LocalDate.of(2026, 11, 16), r.desde());
        assertEquals(LocalDate.of(2026, 11, 22), r.hasta());
    }

    @Test
    void semanaCruzaCambioDeMesYAnio() {
        // 2026-01-01 es jueves → semana del lunes 29-dic-2025 al domingo 4-ene-2026
        PeriodoReporte.Rango r = PeriodoReporte.SEMANA.rango(LocalDate.of(2026, 1, 1));
        assertEquals(LocalDate.of(2025, 12, 29), r.desde());
        assertEquals(LocalDate.of(2026, 1, 4), r.hasta());
    }

    @Test
    void semanaCuandoLaFechaEsDomingoOLunes() {
        assertEquals(LocalDate.of(2026, 11, 16), PeriodoReporte.SEMANA.rango(LocalDate.of(2026, 11, 22)).desde());
        assertEquals(LocalDate.of(2026, 11, 22), PeriodoReporte.SEMANA.rango(LocalDate.of(2026, 11, 16)).hasta());
    }

    @Test
    void mesIncluyeAnioBisiesto() {
        PeriodoReporte.Rango r = PeriodoReporte.MES.rango(LocalDate.of(2028, 2, 10));
        assertEquals(LocalDate.of(2028, 2, 1), r.desde());
        assertEquals(LocalDate.of(2028, 2, 29), r.hasta());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void sinValorUsaDia(String valor) {
        assertEquals(PeriodoReporte.DIA, PeriodoReporte.desde(valor));
    }

    @Test
    void parseoInsensibleAMayusculas() {
        assertEquals(PeriodoReporte.SEMANA, PeriodoReporte.desde("semana"));
        assertEquals(PeriodoReporte.MES, PeriodoReporte.desde(" Mes "));
    }

    @Test
    void valorInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> PeriodoReporte.desde("ANIO"));
        assertThrows(IllegalArgumentException.class, () -> PeriodoReporte.desde("DIA; DROP TABLE citas"));
    }
}
