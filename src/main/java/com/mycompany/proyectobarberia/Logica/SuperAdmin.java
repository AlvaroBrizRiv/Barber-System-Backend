package com.mycompany.proyectobarberia.Logica;

import java.time.LocalDateTime;

/**
 * Representa al usuario con rol SUPERADMIN de la plataforma.
 * Mapeado contra la tabla superadmins y la tabla personas.
 */
public class SuperAdmin extends Persona {

    public SuperAdmin(int id, String doc, String tipoDoc, String nombre, String apellido, String email,
                      String tel, String hash, String foto, String idioma, String pais, String auth,
                      boolean mfa, LocalDateTime fecha, boolean activo) {
        super(id, doc, tipoDoc, nombre, apellido, email, tel, hash, foto, idioma, pais, auth, mfa, fecha, activo);
    }
}
