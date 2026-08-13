package com.mycompany.proyectobarberia.Logica.enums;

/**
 * Categorías de productos de aseo masculino disponibles en la tienda.
 *
 * Corresponde al campo ENUM en la tabla {@code productos} de MySQL.
 */
public enum CategoriaProducto {

    SHAMPOO("Shampoo"),
    BALSAMO("Bálsamo / Acondicionador"),
    CREMA_BARBEAR("Crema de Afeitar"),
    CREMA_MODELADORA("Crema Modeladora de Cabello"),
    ACEITE_BARBA("Aceite para Barba"),
    CERA_CABELLO("Cera para Cabello"),
    LOCION("Loción / Aftershave"),
    OTRO("Otro");

    private final String nombreMostrar;

    CategoriaProducto(String nombreMostrar) {
        this.nombreMostrar = nombreMostrar;
    }

    /**
     * Nombre legible para mostrar en la interfaz.
     * @return Nombre descriptivo de la categoría en español.
     */
    public String getNombreMostrar() {
        return nombreMostrar;
    }
}
