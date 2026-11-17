package com.mycompany.proyectobarberia.API.dto;

import com.mycompany.proyectobarberia.Logica.enums.PeriodoReporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Respuesta del reporte de ganancias.
 *
 * @param periodo     DIA, SEMANA o MES.
 * @param desde       Primer día incluido.
 * @param hasta       Último día incluido.
 * @param total       Suma de todas las citas completadas.
 * @param totalCitas  Número de citas completadas.
 * @param porBarbero  Desglose por barbero (vacío o con un elemento en la vista del barbero).
 * @param serie       Un elemento por cada día del rango (con ceros si no hubo citas).
 */
public record ReporteGanancias(PeriodoReporte periodo, LocalDate desde, LocalDate hasta,
                               BigDecimal total, int totalCitas,
                               List<GananciaBarbero> porBarbero, List<GananciaDia> serie) {

    /**
     * Construye el reporte calculando los totales y rellenando los días sin citas.
     *
     * @param periodo    Periodo solicitado.
     * @param rango      Rango de fechas del periodo.
     * @param porBarbero Desglose por barbero devuelto por la base de datos.
     * @param diasConDatos Días que tuvieron citas completadas.
     * @return Reporte completo y consistente.
     */
    public static ReporteGanancias construir(PeriodoReporte periodo, PeriodoReporte.Rango rango,
                                             List<GananciaBarbero> porBarbero, List<GananciaDia> diasConDatos) {
        Map<LocalDate, GananciaDia> porFecha = new LinkedHashMap<>();
        for (LocalDate d = rango.desde(); !d.isAfter(rango.hasta()); d = d.plusDays(1)) {
            porFecha.put(d, new GananciaDia(d, 0, BigDecimal.ZERO));
        }
        BigDecimal total = BigDecimal.ZERO;
        int citas = 0;
        for (GananciaDia g : diasConDatos) {
            if (porFecha.containsKey(g.fecha())) {
                BigDecimal t = g.total() != null ? g.total() : BigDecimal.ZERO;
                porFecha.put(g.fecha(), new GananciaDia(g.fecha(), g.citas(), t));
                total = total.add(t);
                citas += g.citas();
            }
        }
        return new ReporteGanancias(periodo, rango.desde(), rango.hasta(), total, citas,
                List.copyOf(porBarbero), new ArrayList<>(porFecha.values()));
    }
}
