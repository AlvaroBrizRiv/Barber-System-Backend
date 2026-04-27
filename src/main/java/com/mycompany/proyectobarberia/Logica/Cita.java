package com.mycompany.proyectobarberia.Logica;

public class Cita {
        
        private int id;
        private Cliente cliente;
        private Empleado empleado;
        private String fecha;
        private String hora;
        private String servicio;

        public Cita() {
        }

        public Cita(int id, Cliente cliente, Empleado empleado, String fecha, String hora, String servicio) {
            this.id = id;
            this.cliente = cliente;
            this.empleado = empleado;
            this.fecha = fecha;
            this.hora = hora;
            this.servicio = servicio;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Cliente getCliente() {
            return cliente;
        }

        public void setCliente(Cliente cliente) {
            this.cliente = cliente;
        }

        public Empleado getEmpleado() {
            return empleado;
        }

        public void setEmpleado(Empleado empleado) {
            this.empleado = empleado;
        }

        public String getFecha() {
            return fecha;
        }

        public void setFecha(String fecha) {
            this.fecha = fecha;
        }

        public String getHora() {
            return hora;
        }

        public void setHora(String hora) {
            this.hora = hora;
        }

        public String getServicio() {
            return servicio;
        }

        public void setServicio(String servicio) {
            this.servicio = servicio;
        }
        
        
        
    }
