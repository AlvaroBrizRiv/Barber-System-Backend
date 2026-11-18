package com.mycompany.proyectobarberia.Servicios;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Crea el EmailService según el .env:
 *   - Con SMTP_HOST definido → SmtpEmailService.
 *   - Sin SMTP_HOST → ConsolaEmailService (muestra el código solo si APP_ENV no es production).
 */
public final class EmailServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(EmailServiceFactory.class);

    private EmailServiceFactory() { }

    public static EmailService desdeEntorno(Dotenv env) {
        String host = env.get("SMTP_HOST");
        boolean produccion = "production".equalsIgnoreCase(env.get("APP_ENV", "development"));
        if (host == null || host.isBlank()) {
            LOG.warn("SMTP_HOST no definido: los correos se mostrarán en consola{}.",
                    produccion ? " (ocultos en producción)" : "");
            return new ConsolaEmailService(!produccion);
        }
        String usuario = env.get("SMTP_USER");
        String remitente = env.get("SMTP_FROM", usuario);
        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException("Define SMTP_FROM (o SMTP_USER) en .env para enviar correos.");
        }
        return new SmtpEmailService(
                host.trim(),
                Integer.parseInt(env.get("SMTP_PORT", "587").trim()),
                usuario,
                env.get("SMTP_PASSWORD"),
                remitente.trim(),
                Boolean.parseBoolean(env.get("SMTP_STARTTLS", "true").trim()),
                Boolean.parseBoolean(env.get("SMTP_SSL", "false").trim()));
    }
}
