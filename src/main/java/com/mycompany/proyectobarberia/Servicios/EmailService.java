package com.mycompany.proyectobarberia.Servicios;

/**
 * Abstracción del envío de correos (principio de inversión de dependencias):
 * los controladores dependen de esta interfaz, no de SMTP, lo que permite
 * cambiar de proveedor o usar un doble en las pruebas.
 */
public interface EmailService {

    /**
     * Envía un correo de texto plano.
     *
     * @param destinatario Dirección de correo del destinatario.
     * @param asunto       Asunto del mensaje.
     * @param cuerpo       Texto del mensaje.
     * @throws Exception si el proveedor rechaza o no puede enviar el correo.
     */
    void enviar(String destinatario, String asunto, String cuerpo) throws Exception;
}
