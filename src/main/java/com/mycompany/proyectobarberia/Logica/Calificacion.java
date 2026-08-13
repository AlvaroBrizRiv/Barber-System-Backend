package com.mycompany.proyectobarberia.Logica;

import java.time.LocalDateTime;

/**
 * Calificación dejada por un cliente después de una cita completada.
 *
 * Restricciones de negocio:
 *   - Solo se puede calificar una cita con estado COMPLETADA.
 *   - Cada cita tiene como máximo UNA calificación (restricción UNIQUE en BD).
 *   - Se califica tanto a la barbería como al barbero (por separado).
 *   - Las estrellas van de 1 a 5 (validado en BD y en esta clase).
 */
public class Calificacion {

    private int id;
    private int citaId;         // Referencia a la cita calificada
    private int clienteId;
    private int barberiaId;     // Multi-tenant: facilita consultas por barbería
    private int empleadoId;     // Barbero calificado
    private int estrellasBarberia;  // 1 a 5
    private int estrellasBarbero;   // 1 a 5
    private String comentario;
    private LocalDateTime fecha;

    // ─── Constructores ────────────────────────────────────────────────

    public Calificacion() {
        this.fecha = LocalDateTime.now();
    }

    /**
     * Constructor para registrar una nueva calificación.
     * 
     * @param citaId ID de la cita asociada
     * @param clienteId ID del cliente que califica
     * @param barberiaId ID de la barbería
     * @param empleadoId ID del barbero que atendió
     * @param estrellasBarberia Puntaje para la barbería (1-5)
     * @param estrellasBarbero Puntaje para el barbero (1-5)
     * @param comentario Opinión adicional
     */
    public Calificacion(int citaId, int clienteId, int barberiaId, int empleadoId,
                        int estrellasBarberia, int estrellasBarbero, String comentario) {
        this();
        this.citaId = citaId;
        this.clienteId = clienteId;
        this.barberiaId = barberiaId;
        this.empleadoId = empleadoId;
        setEstrellasBarberia(estrellasBarberia);
        setEstrellasBarbero(estrellasBarbero);
        this.comentario = comentario;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id ID único de la calificación
     * @param citaId ID de la cita asociada
     * @param clienteId ID del cliente
     * @param barberiaId ID de la barbería
     * @param empleadoId ID del barbero
     * @param estrellasBarberia Puntaje (1-5) para la barbería
     * @param estrellasBarbero Puntaje (1-5) para el barbero
     * @param comentario Comentario descriptivo
     * @param fecha Fecha en que se realizó la calificación
     */
    public Calificacion(int id, int citaId, int clienteId, int barberiaId, int empleadoId,
                        int estrellasBarberia, int estrellasBarbero,
                        String comentario, LocalDateTime fecha) {
        this.id = id;
        this.citaId = citaId;
        this.clienteId = clienteId;
        this.barberiaId = barberiaId;
        this.empleadoId = empleadoId;
        this.estrellasBarberia = estrellasBarberia;
        this.estrellasBarbero = estrellasBarbero;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCitaId() { return citaId; }
    public void setCitaId(int citaId) { this.citaId = citaId; }

    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public int getEmpleadoId() { return empleadoId; }
    public void setEmpleadoId(int empleadoId) { this.empleadoId = empleadoId; }

    public int getEstrellasBarberia() { return estrellasBarberia; }

    /**
     * Valida que las estrellas estén en el rango permitido (1-5).
     * @throws IllegalArgumentException si el valor está fuera de rango.
     */
    public void setEstrellasBarberia(int estrellasBarberia) {
        if (estrellasBarberia < 1 || estrellasBarberia > 5) {
            throw new IllegalArgumentException("Las estrellas de la barbería deben estar entre 1 y 5.");
        }
        this.estrellasBarberia = estrellasBarberia;
    }

    public int getEstrellasBarbero() { return estrellasBarbero; }

    /**
     * Valida que las estrellas estén en el rango permitido (1-5).
     * @throws IllegalArgumentException si el valor está fuera de rango.
     */
    public void setEstrellasBarbero(int estrellasBarbero) {
        if (estrellasBarbero < 1 || estrellasBarbero > 5) {
            throw new IllegalArgumentException("Las estrellas del barbero deben estar entre 1 y 5.");
        }
        this.estrellasBarbero = estrellasBarbero;
    }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
