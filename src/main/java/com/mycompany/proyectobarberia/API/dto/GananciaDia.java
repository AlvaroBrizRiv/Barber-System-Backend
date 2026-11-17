package com.mycompany.proyectobarberia.API.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Total de ganancias de un día dentro del periodo consultado. */
public record GananciaDia(LocalDate fecha, int citas, BigDecimal total) {
}
