package com.mycompany.proyectobarberia;

import com.mycompany.proyectobarberia.Persistencia.DAO.EmpleadoDAO;
import com.mycompany.proyectobarberia.Logica.Empleado;

public class TestDAO {
    public static void main(String[] args) {
        try {
            EmpleadoDAO dao = new EmpleadoDAO();
            Empleado e = dao.buscarPorEmail("admin@elcaballero.cl");
            if (e != null) {
                System.out.println("Empleado encontrado: " + e.getNombre() + " " + e.getApellido());
            } else {
                System.out.println("Empleado NO encontrado");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
}
