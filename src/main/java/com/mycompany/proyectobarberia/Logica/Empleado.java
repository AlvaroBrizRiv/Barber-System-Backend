package com.mycompany.proyectobarberia.Logica;

public class Empleado extends Persona {
    
    private String rango;

    public Empleado() {
    }

    public Empleado(String rango, String nombre, String apellido, int rut, String email, int telefono) {
        super(nombre, apellido, rut, email, telefono);
        this.rango = rango;
    }

    public String getRango() {
        return rango;
    }

    public void setRango(String rango) {
        this.rango = rango;
    }
        
}
