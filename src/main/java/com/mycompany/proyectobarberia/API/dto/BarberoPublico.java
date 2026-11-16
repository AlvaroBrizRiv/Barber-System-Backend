package com.mycompany.proyectobarberia.API.dto;

/**
 * DTO para la exposición pública de información de barberos.
 * Excluye explícitamente passwordHash, documentoIdentidad, email, teléfono
 * y datos personales para proteger la privacidad según OWASP y CID.
 */
public record BarberoPublico(int id, String nombre, String apellido, String fotoUrl) {
}
