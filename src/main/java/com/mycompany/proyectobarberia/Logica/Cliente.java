package com.mycompany.proyectobarberia.Logica;

public class Cliente extends Persona {
    
    private int puntoFidelidad;

    public Cliente() {
    }
    
    public Cliente(int puntoFidelidad, String nombre, String apellido, int rut, String email, int telefono) {
        super(nombre, apellido, rut, email, telefono);
        this.puntoFidelidad = puntoFidelidad;
    }
    
    public int getPuntoFidelidad() {
        return puntoFidelidad;
    }
    
    public void setPuntoFidelidad(int puntoFidelidad) {
        this.puntoFidelidad = puntoFidelidad;
    }
}
