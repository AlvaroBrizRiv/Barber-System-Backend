package com.mycompany.proyectobarberia;

import com.mycompany.proyectobarberia.Logica.Cita;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;

public class ProyectoBarberia {

    public static void main(String[] args) {

        // 1. Crear los objetos necesarios
        Cliente cli = new Cliente(100, "Alvaro", "Brizuela", 12345678, "alvaro@email.com", 912345678);
        Empleado emp = new Empleado("Senior", "Juan", "Pinilla", 23456789, "juan@email.com", 987654321);

        // 2. Crear la cita relacionando al cliente y al empleado
        Cita cita1 = new Cita(1, cli, emp, "2026-05-15", "10:30", "Corte de Cabello");

        // 3. Imprimir los datos para verificar
        System.out.println("--- Nueva Cita Registrada ---");
        System.out.println("Cliente: " + cita1.getCliente().getNombre());
        System.out.println("Atendido por: " + cita1.getEmpleado().getNombre());
        System.out.println("Servicio: " + cita1.getServicio());
        System.out.println("Hora: " + cita1.getHora());
    }
}
