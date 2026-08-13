package com.mycompany.proyectobarberia.Logica;

import com.mycompany.proyectobarberia.Logica.enums.Rol;
import java.time.LocalDateTime;

/**
 * Empleado de una barbería (barbero o administrador de barbería).
 *
 * SEGURIDAD: Un empleado con Rol.BARBERO jamás puede tener los privilegios de
 * Rol.ADMIN_BARBERIA. Esto lo garantiza el SecurityMiddleware en cada request.
 */
public class Empleado extends Persona {

    private int barberiaId;
    private Rol rol;
    private boolean activo;

    // ─── Constructores ────────────────────────────────────────────────

    public Empleado() {
        super();
        this.activo = true;
    }

    /**
     * Constructor para registrar un nuevo empleado.
     * 
     * @param documentoIdentidad Documento de identidad
     * @param tipoDocumento Tipo de documento (ej. RUT)
     * @param nombre Nombre del empleado
     * @param apellido Apellido del empleado
     * @param email Correo electrónico
     * @param telefono Teléfono
     * @param passwordHash Hash de la contraseña
     * @param paisCodigo Código de país
     * @param barberiaId ID de la barbería a la que pertenece
     * @param rol Rol asignado (ej. BARBERO, ADMIN_BARBERIA)
     */
    public Empleado(String documentoIdentidad, String tipoDocumento, String nombre,
                    String apellido, String email, String telefono, String passwordHash,
                    String paisCodigo, int barberiaId, Rol rol) {
        this();
        setDocumentoIdentidad(documentoIdentidad);
        setTipoDocumento(tipoDocumento);
        setNombre(nombre);
        setApellido(apellido);
        setEmail(email);
        setTelefono(telefono);
        setPasswordHash(passwordHash);
        setPaisCodigo(paisCodigo);
        this.barberiaId = barberiaId;
        this.rol = rol;
    }

    /**
     * Constructor completo para carga desde base de datos.
     * 
     * @param id ID único de la persona
     * @param documentoIdentidad Documento de identidad
     * @param tipoDocumento Tipo de documento
     * @param nombre Nombre del empleado
     * @param apellido Apellido del empleado
     * @param email Correo electrónico
     * @param telefono Teléfono
     * @param passwordHash Hash de la contraseña
     * @param fotoUrl URL de la foto de perfil
     * @param idiomaPreferido Idioma de preferencia
     * @param paisCodigo Código de país
     * @param authProvider Proveedor de autenticación
     * @param mfaHabilitado Si tiene MFA activado
     * @param fechaRegistro Fecha de registro en el sistema
     * @param personaActiva Estado de la cuenta a nivel de persona
     * @param barberiaId ID de la barbería a la que pertenece
     * @param rol Rol del empleado
     * @param activo Estado activo del empleado
     */
    public Empleado(int id, String documentoIdentidad, String tipoDocumento,
                    String nombre, String apellido, String email, String telefono,
                    String passwordHash, String fotoUrl, String idiomaPreferido,
                    String paisCodigo, String authProvider, boolean mfaHabilitado,
                    LocalDateTime fechaRegistro, boolean personaActiva,
                    int barberiaId, Rol rol, boolean activo) {
        super(id, documentoIdentidad, tipoDocumento, nombre, apellido, email, telefono,
              passwordHash, fotoUrl, idiomaPreferido, paisCodigo, authProvider,
              mfaHabilitado, fechaRegistro, personaActiva);
        this.barberiaId = barberiaId;
        this.rol = rol;
        this.activo = activo;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getBarberiaId() { return barberiaId; }
    public void setBarberiaId(int barberiaId) { this.barberiaId = barberiaId; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

//    public boolean isActivo() { return activo; }
//    public void setActivo(boolean activo) { this.activo = activo; }

    /**
     * Verifica si este empleado es administrador de su barbería.
     * 
     * @return true si su rol es ADMIN_BARBERIA, false en caso contrario
     */
    public boolean esAdmin() {
        return Rol.ADMIN_BARBERIA.equals(this.rol);
    }
}
