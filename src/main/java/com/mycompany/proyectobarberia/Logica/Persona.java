package com.mycompany.proyectobarberia.Logica;

public abstract class Persona {
    protected String nombre;
    protected String apellido;
    protected int rut;
    protected String email;
    protected int telefono;

    protected Persona() {
    }

    protected Persona(String nombre, String apellido, int rut, String email, int telefono) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.rut = rut;
        this.email = email;
        this.telefono = telefono;
    }

    protected String getNombre() {
        return nombre;
    }

    protected void setNombre(String nombre) {
        this.nombre = nombre;
    }

    protected String getApellido() {
        return apellido;
    }

    protected void setApellido(String apellido) {
        this.apellido = apellido;
    }

    protected int getRut() {
        return rut;
    }

    protected void setRut(int rut) {
        this.rut = rut;
    }

    protected String getEmail() {
        return email;
    }

    protected void setEmail(String email) {
        this.email = email;
    }

    protected int getTelefono() {
        return telefono;
    }

    protected void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    
    
}
