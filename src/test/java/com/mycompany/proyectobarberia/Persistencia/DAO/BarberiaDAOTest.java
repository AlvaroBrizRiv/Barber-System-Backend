package com.mycompany.proyectobarberia.Persistencia.DAO;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Pruebas del escape de comodines LIKE usado por la búsqueda de barberías. */
class BarberiaDAOTest {

    @Test
    void envuelveYPasaAMinusculas() {
        assertEquals("%providencia%", BarberiaDAO.patronLike("  Providencia "));
    }

    @Test
    void escapaComodinesParaQueSeanLiterales() {
        assertEquals("%100!%%", BarberiaDAO.patronLike("100%"));
        assertEquals("%el!_caballero%", BarberiaDAO.patronLike("el_caballero"));
        assertEquals("%![a-z]%", BarberiaDAO.patronLike("[a-z]"));
    }

    @Test
    void escapaElPropioCaracterDeEscape() {
        assertEquals("%hola!!%", BarberiaDAO.patronLike("hola!"));
    }

    @Test
    void inyeccionSqlQuedaComoTextoLiteral() {
        // Se usa en un PreparedStatement: las comillas no rompen la consulta, solo se buscan literalmente
        assertEquals("%' or 1=1 --%", BarberiaDAO.patronLike("' OR 1=1 --"));
    }
}
