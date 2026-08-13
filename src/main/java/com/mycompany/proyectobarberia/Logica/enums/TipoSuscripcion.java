package com.mycompany.proyectobarberia.Logica.enums;

/**
 * Tipo de modelo de suscripción que ofrece una barbería.
 *
 * Las barberías pueden elegir uno o ambos modelos.
 *
 * Modelos:
 *   POR_CITA  → El cliente paga un precio fijo por cada cita (sin mensualidad).
 *   MENSUAL   → El cliente paga una tarifa mensual y puede agendar sin costo adicional.
 *   AMBAS     → La barbería ofrece ambas opciones; el cliente elige al suscribirse.
 */
public enum TipoSuscripcion {

    /**
     * Pago por servicio individual.
     * El campo {@code precioPorCita} de Suscripcion debe estar definido.
     */
    POR_CITA,

    /**
     * Suscripción mensual con acceso ilimitado (o limitado según plan).
     * El campo {@code precioMensual} de Suscripcion debe estar definido.
     */
    MENSUAL,

    /**
     * La barbería ofrece ambos modelos simultáneamente.
     * Ambos campos {@code precioPorCita} y {@code precioMensual} deben estar definidos.
     */
    AMBAS
}
