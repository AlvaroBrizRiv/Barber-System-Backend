package com.mycompany.proyectobarberia.Servicios;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Envío de correos por SMTP (Gmail, Outlook, SendGrid, Brevo, etc.).
 * Se configura con las variables SMTP_* del archivo .env.
 */
public class SmtpEmailService implements EmailService {

    private final Session session;
    private final String remitente;

    /**
     * @param host      Servidor SMTP (ej. smtp.gmail.com).
     * @param puerto    Puerto (587 con STARTTLS, 465 con SSL).
     * @param usuario   Usuario SMTP (puede ser null si el servidor no exige autenticación).
     * @param password  Contraseña o "contraseña de aplicación".
     * @param remitente Dirección que aparece como remitente.
     * @param starttls  true para STARTTLS (puerto 587).
     * @param ssl       true para SSL implícito (puerto 465).
     */
    public SmtpEmailService(String host, int puerto, String usuario, String password,
                            String remitente, boolean starttls, boolean ssl) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(puerto));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.starttls.required", String.valueOf(starttls));
        props.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        props.put("mail.smtp.ssl.checkserveridentity", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        boolean conAuth = usuario != null && !usuario.isBlank();
        props.put("mail.smtp.auth", String.valueOf(conAuth));
        this.session = conAuth
                ? Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(usuario, password);
                    }
                })
                : Session.getInstance(props);
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) throws Exception {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(remitente, "Barber System", StandardCharsets.UTF_8.name()));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario, true));
        msg.setSubject(asunto, StandardCharsets.UTF_8.name());
        msg.setText(cuerpo, StandardCharsets.UTF_8.name());
        Transport.send(msg);
    }
}
