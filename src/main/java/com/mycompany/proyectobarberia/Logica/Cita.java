package com.mycompany.proyectobarberia.Logica;

import com.mycompany.proyectobarberia.Logica.enums.EstadoCita;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cita/reserva de un cliente con un barbero en una barbería.
 *
 * CORRECCIONES respecto a la versión anterior:
 *   - fecha: cambiado de String → LocalDate  (correcto para fechas)
 *   - hora:  cambiado de String → LocalTime  (correcto para horas)
 *   - Agregado: barberiaId  — clave multi-tenant principal.
 *   - Agregado: estado      — enum EstadoCita (PENDIENTE, CONFIRMADA, etc.)
 *   - Agregado: precioTotal — precio acordado al confirmar la cita.
 *   - Agregado: servicios   — lista de servicios incluidos en la cita.
 *   - Agregado: notas       — observaciones adicionales del cliente.
 *   - Agregado: fechaCreacion — para trazabilidad y auditoría.
 */
public class Cita {

    private int id;
    private int barberiaId;                 // Multi-tenant: a qué barbería pertenece
    private Cliente cliente;
    private Empleado empleado;
    private LocalDate fecha;                // Fecha del servicio
    private LocalTime hora;                 // Hora del servicio
    private EstadoCita estado;             // Estado del ciclo de vida
    private double precioTotal;            // Precio calculado al confirmar
    private String notas;                  // Notas del cliente
    private List<Servicio> servicios;      // Servicios incluidos en la cita
    private LocalDateTime fechaCreacion;   // Cuando se agendó

    // ─── Constructores ────────────────────────────────────────────────

    public Cita() {
        this.estado = EstadoCita.PENDIENTE;
        this.servicios = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Constructor para crear una nueva cita inicial (sin ID generado aún).
     * 
     * @param barberiaId ID de la barbería donde se reserva
     * @param cliente Cliente que agenda la cita
     * @param empleado Barbero que brindará el servicio
     * @param fecha Fecha programada para la cita
     * @param hora Hora programada para la cita
     * @param notas Notas u observaciones adicionales proporcionadas por el cliente
     */
    public Cita(int barberiaId, Cliente cliente, Empleado empleado,
                LocalDate fecha, LocalTime hora, String notas) {
        this();
        this.barberiaId = barberiaId;
        this.cliente = cliente;
        this.empleado = empleado;
        this.fecha = fecha;
        this.hora = hora;
        this.notas = notas;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id ID único de la cita
     * @param barberiaId ID de la barbería a la que pertenece
     * @param cliente Objeto Cliente asociado
     * @param empleado Objeto Empleado asignado (Barbero)
     * @param fecha Fecha del servicio
     * @param hora Hora del servicio
     * @param estado Estado actual de la cita (ej. PENDIENTE, COMPLETADA)
     * @param precioTotal Precio total calculado de los servicios
     * @param notas Observaciones registradas
     * @param fechaCreacion Fecha y hora de cuando se creó la cita
     */
    public Cita(int id, int barberiaId, Cliente cliente, Empleado empleado,
                LocalDate fecha, LocalTime hora, EstadoCita estado,
                double precioTotal, String notas, LocalDateTime fechaCreacion) {
        this.id = id;
        this.barberiaId = barberiaId;
        this.cliente = cliente;
        this.empleado = empleado;
        this.fecha = fecha;
        this.hora = hora;
        this.estado = estado;
        this.precioTotal = precioTotal;
        this.notas = notas;
        this.servicios = new ArrayList<>();
        this.fechaCreacion = fechaCreacion;
    }

    // ─── Métodos de negocio ───────────────────────────────────────────

    /**
     * Agrega un servicio a la cita y acumula el precio.
     * @param servicio El servicio a agregar.
     */
    public void agregarServicio(Servicio servicio) {
        this.servicios.add(servicio);
        this.precioTotal += servicio.getPrecio();
    }

    /**
     * Verifica si la cita puede ser calificada.
     * Solo las citas COMPLETADAS admiten calificación.
     * 
     * @return true si el estado es COMPLETADA, false en caso contrario
     */
    public boolean puedeCalificarse() {
        return EstadoCita.COMPLETADA.equals(this.estado);
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public EstadoCita getEstado() { return estado; }
    public void setEstado(EstadoCita estado) { this.estado = estado; }

    public double getPrecioTotal() { return precioTotal; }
    public void setPrecioTotal(double precioTotal) { this.precioTotal = precioTotal; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public List<Servicio> getServicios() { return servicios; }
    public void setServicios(List<Servicio> servicios) { this.servicios = servicios; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
