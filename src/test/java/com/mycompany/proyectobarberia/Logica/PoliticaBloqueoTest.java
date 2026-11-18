package com.mycompany.proyectobarberia.Logica;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaBloqueoTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 18, 10, 0, 0);

    /** Simula n fallos seguidos (respetando las esperas). */
    private static PoliticaBloqueo.Resultado fallar(int n) {
        PoliticaBloqueo.Estado e = PoliticaBloqueo.Estado.LIMPIO;
        PoliticaBloqueo.Resultado r = null;
        for (int i = 0; i < n; i++) {
            r = PoliticaBloqueo.registrarFallo(e, T0.plusMinutes(i));
            e = r.nuevoEstado();
        }
        return r;
    }

    @Test
    void primerosCuatroFallosNoSuspenden() {
        for (int i = 1; i <= 4; i++) {
            PoliticaBloqueo.Resultado r = fallar(i);
            assertFalse(r.suspendida());
            assertFalse(r.bloqueoDefinitivo());
            assertNull(r.nuevoEstado().bloqueadoHasta());
            assertEquals(i, r.nuevoEstado().intentosFallidos());
        }
    }

    @Test
    void quintoFalloEspera15Segundos() {
        PoliticaBloqueo.Resultado r = fallar(5);
        assertEquals(Duration.ofSeconds(15), r.espera());
        assertEquals(T0.plusMinutes(4).plusSeconds(15), r.nuevoEstado().bloqueadoHasta());
        assertFalse(r.bloqueoDefinitivo());
    }

    @Test
    void del6al9NoSuspendenYDecimoEspera30() {
        for (int i = 6; i <= 9; i++) assertFalse(fallar(i).suspendida(), "fallo " + i);
        PoliticaBloqueo.Resultado r = fallar(10);
        assertEquals(Duration.ofSeconds(30), r.espera());
        assertFalse(r.bloqueoDefinitivo());
    }

    @Test
    void del11al14NoSuspendenYDecimoquintoBloquea() {
        for (int i = 11; i <= 14; i++) {
            PoliticaBloqueo.Resultado r = fallar(i);
            assertFalse(r.suspendida(), "fallo " + i);
            assertFalse(r.bloqueoDefinitivo(), "fallo " + i);
        }
        PoliticaBloqueo.Resultado r = fallar(15);
        assertTrue(r.bloqueoDefinitivo());
        assertTrue(r.nuevoEstado().bloqueada());
        assertNull(r.espera());
    }

    @Test
    void segundosRestantesRedondeaHaciaArriba() {
        PoliticaBloqueo.Estado e = new PoliticaBloqueo.Estado(5, T0.plusSeconds(15), false);
        assertEquals(15, PoliticaBloqueo.segundosRestantes(e, T0));
        assertEquals(1, PoliticaBloqueo.segundosRestantes(e, T0.plusSeconds(14).plusNanos(1)));
        assertEquals(0, PoliticaBloqueo.segundosRestantes(e, T0.plusSeconds(15)));
        assertEquals(0, PoliticaBloqueo.segundosRestantes(PoliticaBloqueo.Estado.LIMPIO, T0));
    }

    @Test
    void intentosAntesDeSiguienteMedida() {
        assertEquals(5, PoliticaBloqueo.intentosAntesDeSiguienteMedida(PoliticaBloqueo.Estado.LIMPIO));
        assertEquals(2, PoliticaBloqueo.intentosAntesDeSiguienteMedida(new PoliticaBloqueo.Estado(8, null, false)));
    }
}
