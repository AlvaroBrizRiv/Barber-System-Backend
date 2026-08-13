package com.mycompany.proyectobarberia.Logica.enums;

/**
 * Estados posibles del ciclo de vida de una Cita.
 *
 * Flujo típico:
 *   PENDIENTE → CONFIRMADA → COMPLETADA
 *            └→ CANCELADA
 *
 * Transiciones permitidas:
 *   - PENDIENTE  → CONFIRMADA (admin/barbero confirma)
 *   - PENDIENTE  → CANCELADA  (cliente o admin cancela)
 *   - CONFIRMADA → COMPLETADA (barbero marca como realizada)
 *   - CONFIRMADA → CANCELADA  (cancelación tardía)
 *
 * Solo una cita en estado COMPLETADA puede recibir una calificación.
 */
public enum EstadoCita {

    /** La cita fue creada pero aún no ha sido confirmada por la barbería. */
    PENDIENTE,

    /** La barbería confirmó la cita. El cliente debe presentarse. */
    CONFIRMADA,

    /** La cita fue cancelada (por el cliente, el barbero o el admin). */
    CANCELADA,

    /** El servicio fue realizado. El cliente puede dejar una calificación. */
    COMPLETADA
}
