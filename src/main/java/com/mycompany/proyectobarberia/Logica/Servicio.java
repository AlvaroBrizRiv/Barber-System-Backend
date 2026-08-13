package com.mycompany.proyectobarberia.Logica;

/**
 * Servicio ofrecido por una barbería.
 *
 * Cada barbería define sus propios servicios con sus precios y duraciones.
 * El cliente puede seleccionar uno o más servicios al agendar una cita.
 *
 * Ejemplos: "Corte Premium", "Barba Clásica + Toalla Húmeda", "Combo Corte & Barba".
 */
public class Servicio {

    private int id;
    private int barberiaId;         // Clave multi-tenant: a qué barbería pertenece
    private String nombre;
    private String descripcion;
    private double precio;          // Precio en CLP (pesos chilenos)
    private int duracionMinutos;    // Duración estimada para bloquear agenda
    private boolean activo;

    // ─── Constructores ────────────────────────────────────────────────

    public Servicio() {
        this.activo = true;
    }

    /**
     * Constructor para crear un nuevo servicio.
     * 
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del servicio
     * @param descripcion Descripción de lo que incluye el servicio
     * @param precio Precio en CLP
     * @param duracionMinutos Tiempo aproximado que demora el servicio
     */
    public Servicio(int barberiaId, String nombre, String descripcion,
                    double precio, int duracionMinutos) {
        this();
        this.barberiaId = barberiaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id Identificador único del servicio
     * @param barberiaId ID de la barbería
     * @param nombre Nombre del servicio
     * @param descripcion Descripción
     * @param precio Precio en CLP
     * @param duracionMinutos Duración en minutos
     * @param activo Estado actual (disponible o no)
     */
    public Servicio(int id, int barberiaId, String nombre, String descripcion,
                    double precio, int duracionMinutos, boolean activo) {
        this.id = id;
        this.barberiaId = barberiaId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
        this.activo = activo;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
