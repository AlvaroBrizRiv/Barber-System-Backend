package com.mycompany.proyectobarberia.API.dto;

import java.math.BigDecimal;

/** Ganancias de un barbero en un periodo (solo citas COMPLETADA). */
public record GananciaBarbero(int empleadoId, String nombre, String apellido, int citas, BigDecimal total) {
}
