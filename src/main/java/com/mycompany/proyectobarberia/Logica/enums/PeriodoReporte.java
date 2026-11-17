package com.mycompany.proyectobarberia.Logica.enums;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Periodo de agregación para los reportes de ganancias.
 *
 *   DIA    → solo la fecha de referencia.
 *   SEMANA → lunes a domingo de la semana que contiene la fecha (ISO-8601).
 *   MES    → del día 1 al último día del mes de la fecha.
 */
public enum PeriodoReporte {

    DIA,
    SEMANA,
    MES;

    /** Rango cerrado [desde, hasta] de fechas incluidas en el periodo. */
    public record Rango(LocalDate desde, LocalDate hasta) { }

    /**
     * Calcula el rango de fechas del periodo que contiene la fecha de referencia.
     *
     * @param referencia Fecha dentro del periodo (no nula).
     * @return Rango con fechas inclusivas.
     */
    public Rango rango(LocalDate referencia) {
        return switch (this) {
            case DIA -> new Rango(referencia, referencia);
            case SEMANA -> new Rango(
                    referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    referencia.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case MES -> new Rango(
                    referencia.withDayOfMonth(1),
                    referencia.with(TemporalAdjusters.lastDayOfMonth()));
        };
    }

    /**
     * Convierte el texto del query param en un periodo (insensible a mayúsculas).
     *
     * @param valor Texto recibido ("dia", "SEMANA", ...). Null o vacío → DIA.
     * @return El periodo correspondiente.
     * @throws IllegalArgumentException si el valor no es un periodo válido.
     */
    public static PeriodoReporte desde(String valor) {
        if (valor == null || valor.isBlank()) return DIA;
        return PeriodoReporte.valueOf(valor.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
