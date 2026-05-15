package com.mycompany.proyectobarberia.Logica;

public class Producto {
    int id, precio, cantidad;
    String producto;

    public Producto() {
    }

    public Producto(int id, int precio, int cantidad, String producto) {
        this.id = id;
        this.precio = precio;
        this.cantidad = cantidad;
        this.producto = producto;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getProducto() {
        return producto;
    }

    public void setProducto(String producto) {
        this.producto = producto;
    }
    
    
    
}
