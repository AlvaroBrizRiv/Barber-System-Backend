package com.mycompany.proyectobarberia.Servicios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternativa para desarrollo cuando no hay SMTP configurado:
 * escribe el correo en la consola del servidor en lugar de enviarlo.
 *
 * En producción (APP_ENV=production) NO muestra el contenido, porque incluiría
 * el código de recuperación; solo registra un error para que se configure SMTP.
 */
public class ConsolaEmailService implements EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolaEmailService.class);
    private final boolean mostrarContenido;

    /** @param mostrarContenido true en desarrollo; false en producción. */
    public ConsolaEmailService(boolean mostrarContenido) {
        this.mostrarContenido = mostrarContenido;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        if (mostrarContenido) {
            LOG.warn("[CORREO SIMULADO — configure SMTP_* en .env para enviar de verdad]\n"
                    + "Para: {}\nAsunto: {}\n{}", destinatario, asunto, cuerpo);
        } else {
            LOG.error("SMTP no configurado: no se pudo enviar el correo '{}'.", asunto);
        }
    }
}
