package com.mycompany.proyectobarberia.Logica;

import com.mycompany.proyectobarberia.Logica.enums.TipoSuscripcion;

/**
 * Plan de suscripción ofrecido por una barbería.
 *
 * El administrador de cada barbería puede crear y gestionar sus propios planes.
 * Pueden coexistir múltiples planes para una misma barbería.
 *
 * Modelos posibles:
 *   - POR_CITA: precio fijo por servicio (sin mensualidad).
 *   - MENSUAL: tarifa mensual con acceso incluido.
 *   - AMBAS: la barbería ofrece ambas opciones.
 */
public class Suscripcion {

    private int id;
    private int barberiaId;             // Multi-tenant: a qué barbería pertenece
    private String nombre;              // Ej: "Plan VIP Mensual", "Tarifa Por Cita"
    private TipoSuscripcion tipo;
    private Double precioPorCita;       // Null si tipo = MENSUAL
    private Double precioMensual;       // Null si tipo = POR_CITA
    private String descripcion;
    private boolean activa;

    // ─── Constructores ────────────────────────────────────────────────

    public Suscripcion() {
        this.activa = true;
    }

    /**
     * Constructor para crear una suscripción de tipo POR_CITA.
     * 
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del plan
     * @param precioPorCita Precio por cada cita individual
     * @param descripcion Descripción del plan
     * @return Objeto Suscripcion configurado como POR_CITA
     */
    public static Suscripcion porCita(int barberiaId, String nombre,
                                      double precioPorCita, String descripcion) {
        Suscripcion s = new Suscripcion();
        s.barberiaId = barberiaId;
        s.nombre = nombre;
        s.tipo = TipoSuscripcion.POR_CITA;
        s.precioPorCita = precioPorCita;
        s.descripcion = descripcion;
        return s;
    }

    /**
     * Constructor para crear una suscripción de tipo MENSUAL.
     * 
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del plan
     * @param precioMensual Precio fijo mensual
     * @param descripcion Descripción del plan
     * @return Objeto Suscripcion configurado como MENSUAL
     */
    public static Suscripcion mensual(int barberiaId, String nombre,
                                      double precioMensual, String descripcion) {
        Suscripcion s = new Suscripcion();
        s.barberiaId = barberiaId;
        s.nombre = nombre;
        s.tipo = TipoSuscripcion.MENSUAL;
        s.precioMensual = precioMensual;
        s.descripcion = descripcion;
        return s;
    }

    /**
     * Constructor para crear una suscripción que ofrece ambos modelos.
     * 
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del plan
     * @param precioPorCita Precio por cita
     * @param precioMensual Precio mensual
     * @param descripcion Descripción del plan
     * @return Objeto Suscripcion configurado como AMBAS
     */
    public static Suscripcion ambas(int barberiaId, String nombre,
                                    double precioPorCita, double precioMensual,
                                    String descripcion) {
        Suscripcion s = new Suscripcion();
        s.barberiaId = barberiaId;
        s.nombre = nombre;
        s.tipo = TipoSuscripcion.AMBAS;
        s.precioPorCita = precioPorCita;
        s.precioMensual = precioMensual;
        s.descripcion = descripcion;
        return s;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id ID único de la suscripción
     * @param barberiaId ID de la barbería asociada
     * @param nombre Nombre de la suscripción
     * @param tipo Tipo de modelo de cobro (POR_CITA, MENSUAL, AMBAS)
     * @param precioPorCita Valor opcional del precio por cita
     * @param precioMensual Valor opcional del precio mensual
     * @param descripcion Detalles adicionales
     * @param activa Estado del plan
     */
    public Suscripcion(int id, int barberiaId, String nombre, TipoSuscripcion tipo,
                       Double precioPorCita, Double precioMensual,
                       String descripcion, boolean activa) {
        this.id = id;
        this.barberiaId = barberiaId;
        this.nombre = nombre;
        this.tipo = tipo;
        this.precioPorCita = precioPorCita;
        this.precioMensual = precioMensual;
        this.descripcion = descripcion;
        this.activa = activa;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoSuscripcion getTipo() { return tipo; }
    public void setTipo(TipoSuscripcion tipo) { this.tipo = tipo; }

    public Double getPrecioPorCita() { return precioPorCita; }
    public void setPrecioPorCita(Double precioPorCita) { this.precioPorCita = precioPorCita; }

    public Double getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(Double precioMensual) { this.precioMensual = precioMensual; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
