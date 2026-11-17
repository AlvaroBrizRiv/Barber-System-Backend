package com.mycompany.proyectobarberia.Logica;

import java.time.LocalDateTime;

/**
 * Representa una barbería dentro de la plataforma.
 *
 * Es la entidad central del modelo multi-tenant: cada barbería tiene su
 * propio conjunto de empleados, servicios, productos y suscripciones.
 * Un administrador solo puede gestionar los datos de SU propia barbería.
 */
public class Barberia {

    private int id;
    private String nombre;
    private String direccion;
    private String telefono;
    private String email;
    private String logoUrl;
    private String descripcion;
    private boolean activa;
    private String paisCodigo;
    private String monedaCodigo;
    private String zonaHoraria;
    /** Si es true, la página pública de la barbería muestra la sección de suscripciones. */
    private boolean suscripcionesHabilitadas;
    private LocalDateTime fechaCreacion;

    // ─── Constructores ────────────────────────────────────────────────

    public Barberia() {
        this.activa = true;
        this.paisCodigo = "CL";
        this.monedaCodigo = "CLP";
        this.zonaHoraria = "America/Santiago";
        this.fechaCreacion = LocalDateTime.now();
    }

    /**
     * Constructor para registro de nueva barbería.
     * 
     * @param nombre Nombre comercial
     * @param direccion Ubicación física
     * @param telefono Teléfono de contacto
     * @param email Correo electrónico
     * @param descripcion Detalles adicionales
     */
    public Barberia(String nombre, String direccion, String telefono,
                    String email, String descripcion) {
        this();
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.descripcion = descripcion;
    }

    /**
     * Constructor completo para carga desde base de datos.
     */
    public Barberia(int id, String nombre, String direccion, String telefono,
                    String email, String logoUrl, String descripcion,
                    boolean activa, LocalDateTime fechaCreacion) {
        this(id, nombre, direccion, telefono, email, logoUrl, descripcion,
             activa, "CL", "CLP", "America/Santiago", fechaCreacion);
    }

    public Barberia(int id, String nombre, String direccion, String telefono,
                    String email, String logoUrl, String descripcion,
                    boolean activa, String paisCodigo, String monedaCodigo,
                    String zonaHoraria, LocalDateTime fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.direccion = direccion;
        this.telefono = telefono;
        this.email = email;
        this.logoUrl = logoUrl;
        this.descripcion = descripcion;
        this.activa = activa;
        this.paisCodigo = paisCodigo != null ? paisCodigo.trim() : "CL";
        this.monedaCodigo = monedaCodigo != null ? monedaCodigo.trim() : "CLP";
        this.zonaHoraria = zonaHoraria != null ? zonaHoraria : "America/Santiago";
        this.fechaCreacion = fechaCreacion;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public String getPaisCodigo() { return paisCodigo; }
    public void setPaisCodigo(String paisCodigo) { this.paisCodigo = paisCodigo; }

    public String getMonedaCodigo() { return monedaCodigo; }
    public void setMonedaCodigo(String monedaCodigo) { this.monedaCodigo = monedaCodigo; }

    public String getZonaHoraria() { return zonaHoraria; }
    public void setZonaHoraria(String zonaHoraria) { this.zonaHoraria = zonaHoraria; }

    public boolean isSuscripcionesHabilitadas() { return suscripcionesHabilitadas; }
    public void setSuscripcionesHabilitadas(boolean suscripcionesHabilitadas) { this.suscripcionesHabilitadas = suscripcionesHabilitadas; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
