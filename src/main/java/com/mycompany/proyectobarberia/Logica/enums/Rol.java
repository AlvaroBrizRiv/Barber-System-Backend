package com.mycompany.proyectobarberia.Logica.enums;

/**
 * Define los roles de usuario en el sistema.
 *
 * Jerarquía de privilegios (de menor a mayor):
 *   CLIENTE → BARBERO → ADMIN_BARBERIA → SUPERADMIN
 *
 * Reglas de seguridad:
 *   - CLIENTE: puede reservar citas y comprar productos en cualquier barbería.
 *   - BARBERO: puede ver su propia agenda dentro de SU barbería.
 *     No puede modificar datos de la barbería ni ver datos de otros barberos.
 *   - ADMIN_BARBERIA: CRUD completo sobre los datos de SU barbería.
 *     No puede acceder a datos de otras barberías.
 *   - SUPERADMIN: acceso total a la plataforma. Gestiona barberías.
 */
public enum Rol {

    /** Cliente registrado en la plataforma. */
    CLIENTE,

    /** Barbero/empleado de una barbería específica. */
    BARBERO,

    /** Administrador de una barbería específica. */
    ADMIN_BARBERIA,

    /** Superadministrador de la plataforma completa. */
    SUPERADMIN;

    /**
     * Verifica si este rol tiene al menos los privilegios del rol indicado.
     * Útil para validar permisos de forma jerárquica.
     *
     * @param rolMinimo El rol mínimo requerido para una acción.
     * @return {@code true} si este rol tiene igual o mayor jerarquía.
     */
    public boolean tienePrivilegiosDeAlMenos(Rol rolMinimo) {
        return this.ordinal() >= rolMinimo.ordinal();
    }
}
