package com.mycompany.proyectobarberia.Logica;

import java.time.LocalDateTime;

/**
 * Cliente registrado en la plataforma.
 *
 * Los clientes pueden reservar citas en CUALQUIER barbería de la plataforma,
 * a diferencia de los empleados que pertenecen a una barbería específica.
 */
public class Cliente extends Persona {

    private int puntosFidelidad;

    // ─── Constructores ────────────────────────────────────────────────

    public Cliente() {
        super();
        this.puntosFidelidad = 0;
    }

    /**
     * Constructor para registro de nuevo cliente (datos mínimos).
     * 
     * @param documentoIdentidad Documento de identidad del cliente
     * @param tipoDocumento Tipo de documento (e.g. RUT)
     * @param nombre Nombre del cliente
     * @param apellido Apellido del cliente
     * @param email Correo electrónico del cliente
     * @param telefono Teléfono del cliente
     * @param passwordHash Hash de la contraseña
     * @param paisCodigo Código de país del cliente (e.g. CL)
     */
    public Cliente(String documentoIdentidad, String tipoDocumento, String nombre,
                   String apellido, String email, String telefono, String passwordHash,
                   String paisCodigo) {
        this();
        setDocumentoIdentidad(documentoIdentidad);
        setTipoDocumento(tipoDocumento);
        setNombre(nombre);
        setApellido(apellido);
        setEmail(email);
        setTelefono(telefono);
        setPasswordHash(passwordHash);
        setPaisCodigo(paisCodigo);
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id Identificador único del cliente
     * @param documentoIdentidad Documento de identidad del cliente
     * @param tipoDocumento Tipo de documento (e.g. RUT)
     * @param nombre Nombre del cliente
     * @param apellido Apellido del cliente
     * @param email Correo electrónico del cliente
     * @param telefono Teléfono del cliente
     * @param passwordHash Hash de la contraseña
     * @param fotoUrl URL de la foto de perfil
     * @param idiomaPreferido Idioma de preferencia (e.g. es)
     * @param paisCodigo Código de país del cliente (e.g. CL)
     * @param authProvider Proveedor de autenticación (e.g. google, local)
     * @param mfaHabilitado Booleano que indica si tiene autenticación multifactor
     * @param fechaRegistro Fecha y hora de registro del cliente
     * @param activo Indica si el cliente está activo
     * @param puntosFidelidad Puntos acumulados del cliente
     */
    public Cliente(int id, String documentoIdentidad, String tipoDocumento,
                   String nombre, String apellido, String email, String telefono,
                   String passwordHash, String fotoUrl, String idiomaPreferido,
                   String paisCodigo, String authProvider, boolean mfaHabilitado,
                   LocalDateTime fechaRegistro, boolean activo, int puntosFidelidad) {
        super(id, documentoIdentidad, tipoDocumento, nombre, apellido, email, telefono,
              passwordHash, fotoUrl, idiomaPreferido, paisCodigo, authProvider,
              mfaHabilitado, fechaRegistro, activo);
        this.puntosFidelidad = puntosFidelidad;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getPuntosFidelidad() { return puntosFidelidad; }
    public void setPuntosFidelidad(int puntosFidelidad) { this.puntosFidelidad = puntosFidelidad; }

    /**
     * Agrega puntos de fidelidad al cliente.
     * Se llama típicamente al completar una cita.
     * 
     * @param puntos Cantidad de puntos a agregar
     */
    public void agregarPuntos(int puntos) {
        // Añade los puntos solo si son un valor positivo
        if (puntos > 0) this.puntosFidelidad += puntos;
    }
}
