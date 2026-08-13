package com.mycompany.proyectobarberia.Logica;

import java.time.LocalDateTime;

/**
 * Clase base abstracta para todos los usuarios del sistema.
 *
 * Diseño multi-país:
 *   - documentoIdentidad: reemplaza a "rut". Soporta RUT (CL), CPF (BR), DNI, etc.
 *   - tipoDocumento: enum que indica el tipo de documento según el país.
 *   - paisCodigo: ISO 3166-1 alpha-2 (CL, BR, AR, etc.)
 *   - idiomaPreferido: código IETF (es, pt-BR)
 *
 * Preparación SSO/MFA (estructura de datos, sin implementación activa):
 *   - authProvider: LOCAL | GOOGLE | GITHUB
 *   - mfaHabilitado: booleano para activar 2FA en el futuro
 *
 * Seguridad PII:
 *   - passwordHash NO se serializa a JSON (sin @JsonProperty en Gson por defecto).
 *   - Se excluye explícitamente en los endpoints que devuelven perfil.
 */
public abstract class Persona {

    private int id;
    private String documentoIdentidad;  // RUT, CPF, DNI, Pasaporte — según país
    private String tipoDocumento;       // RUT | CPF | DNI | PASAPORTE | OTRO
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String passwordHash;        // BCrypt hash — NUNCA texto plano, excluir de respuestas JSON
    private String fotoUrl;             // URL externa de foto de perfil
    private String idiomaPreferido;     // es | pt-BR
    private String paisCodigo;          // ISO 3166-1 alpha-2
    private String authProvider;        // LOCAL | GOOGLE | GITHUB (preparación SSO)
    private boolean mfaHabilitado;      // Preparación MFA
    private LocalDateTime fechaRegistro;
    private boolean activo;

    // ─── Constructores ────────────────────────────────────────────────

    public Persona() {
        this.activo = true;
        this.fechaRegistro = LocalDateTime.now();
        this.tipoDocumento = "RUT";
        this.idiomaPreferido = "es";
        this.paisCodigo = "CL";
        this.authProvider = "LOCAL";
        this.mfaHabilitado = false;
    }

    /** Constructor completo para carga desde base de datos. */
    public Persona(int id, String documentoIdentidad, String tipoDocumento,
                   String nombre, String apellido, String email, String telefono,
                   String passwordHash, String fotoUrl, String idiomaPreferido,
                   String paisCodigo, String authProvider, boolean mfaHabilitado,
                   LocalDateTime fechaRegistro, boolean activo) {
        this.id = id;
        this.documentoIdentidad = documentoIdentidad;
        this.tipoDocumento = tipoDocumento;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.passwordHash = passwordHash;
        this.fotoUrl = fotoUrl;
        this.idiomaPreferido = idiomaPreferido;
        this.paisCodigo = paisCodigo;
        this.authProvider = authProvider;
        this.mfaHabilitado = mfaHabilitado;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }

    // ─── Getters y Setters ────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getIdiomaPreferido() { return idiomaPreferido; }
    public void setIdiomaPreferido(String idiomaPreferido) { this.idiomaPreferido = idiomaPreferido; }

    public String getPaisCodigo() { return paisCodigo; }
    public void setPaisCodigo(String paisCodigo) { this.paisCodigo = paisCodigo; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public boolean isMfaHabilitado() { return mfaHabilitado; }
    public void setMfaHabilitado(boolean mfaHabilitado) { this.mfaHabilitado = mfaHabilitado; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    /** Nombre completo concatenado. */
    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
