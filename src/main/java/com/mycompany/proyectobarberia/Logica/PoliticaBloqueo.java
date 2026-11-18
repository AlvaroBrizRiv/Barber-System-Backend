package com.mycompany.proyectobarberia.Logica;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Regla de bloqueo progresivo por intentos fallidos de inicio de sesión:
 *
 *   5 fallos  → espera de 15 segundos
 *   10 fallos → espera de 30 segundos
 *   15 fallos → cuenta bloqueada hasta restablecer la contraseña
 *
 * Clase sin dependencias (lógica pura) para poder probarla de forma aislada.
 */
public final class PoliticaBloqueo {

    public static final int INTENTOS_POR_TRAMO = 5;
    public static final Duration ESPERA_PRIMER_TRAMO = Duration.ofSeconds(15);
    public static final Duration ESPERA_SEGUNDO_TRAMO = Duration.ofSeconds(30);
    public static final int INTENTOS_BLOQUEO = 3 * INTENTOS_POR_TRAMO;

    /** Estado de bloqueo de una cuenta. */
    public record Estado(int intentosFallidos, LocalDateTime bloqueadoHasta, boolean bloqueada) {
        public static final Estado LIMPIO = new Estado(0, null, false);
    }

    /** Resultado de registrar un fallo. */
    public record Resultado(Estado nuevoEstado, Duration espera) {
        public boolean bloqueoDefinitivo() { return nuevoEstado.bloqueada(); }
        public boolean suspendida() { return espera != null; }
    }

    private PoliticaBloqueo() { }

    /**
     * Calcula el nuevo estado tras un intento fallido.
     *
     * @param actual Estado antes del intento.
     * @param ahora  Momento del intento.
     * @return Estado nuevo y, si corresponde, la espera aplicada.
     */
    public static Resultado registrarFallo(Estado actual, LocalDateTime ahora) {
        int intentos = actual.intentosFallidos() + 1;
        if (intentos >= INTENTOS_BLOQUEO) {
            return new Resultado(new Estado(intentos, null, true), null);
        }
        Duration espera = null;
        if (intentos == INTENTOS_POR_TRAMO) espera = ESPERA_PRIMER_TRAMO;
        else if (intentos == 2 * INTENTOS_POR_TRAMO) espera = ESPERA_SEGUNDO_TRAMO;
        LocalDateTime hasta = espera != null ? ahora.plus(espera) : null;
        return new Resultado(new Estado(intentos, hasta, false), espera);
    }

    /**
     * Segundos que faltan para poder volver a intentar (0 si no hay espera).
     * Se redondea hacia arriba para no mostrar "0 s" mientras sigue bloqueada.
     */
    public static long segundosRestantes(Estado estado, LocalDateTime ahora) {
        if (estado.bloqueadoHasta() == null || !ahora.isBefore(estado.bloqueadoHasta())) return 0;
        long ms = Duration.between(ahora, estado.bloqueadoHasta()).toMillis();
        return (ms + 999) / 1000;
    }

    /** Intentos que quedan antes de la siguiente espera o del bloqueo definitivo. */
    public static int intentosAntesDeSiguienteMedida(Estado estado) {
        int usados = estado.intentosFallidos() % INTENTOS_POR_TRAMO;
        return INTENTOS_POR_TRAMO - usados;
    }
}
