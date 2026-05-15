package com.mycompany.proyectobarberia.Logica;

public abstract class Persona {
    protected int id;
    protected String nombre;
    protected String apellido;
    protected int rut;
    protected String email;
    protected int telefono;

    public Persona() {
    }

    public Persona(int id, String nombre, String apellido, int rut, String email, int telefono) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rut = rut;
        this.email = email;
        this.telefono = telefono;
    }
    public int getId(){
            return id;
    }
    
    public void setId (int id){
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getRut() {
        return rut;
    }

    public void setRut(int rut) {
        this.rut = rut;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTelefono() {
        return telefono;
    }

    public void setTelefono(int telefono) {
        this.telefono = telefono;
    }

    
    
}
