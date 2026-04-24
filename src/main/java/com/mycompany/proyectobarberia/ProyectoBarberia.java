package com.mycompany.proyectobarberia;
import com.mycompany.proyectobarberia.Logica.Cliente;
import com.mycompany.proyectobarberia.Logica.Empleado;

public class ProyectoBarberia {

    public static void main(String[] args) {
        
        // Crear un cliente de prueba
        Cliente c1 = new Cliente(100,"Alvaro", "Brizuela", 12345678, "alvaro@email.com", 912345678);

         // Imprimir un dato para verificar la herencia
         System.out.println("Cliente creado: " + c1.getNombre() + " " + c1.getApellido() + " " + c1.getRut());
        System.out.println("Puntos: " + c1.getPuntoFidelidad());
        
        Empleado e1 = new Empleado("Senior", "Juan", "Pinilla", 234567892, "juan@email.com", 98765345);
        System.out.println("Empleado creado: " + e1.getNombre() + " " + e1.getApellido());
        System.out.println("Cargo: " + e1.getRango());
    }
}
