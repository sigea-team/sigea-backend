package com.sigea.demosigea_backend.dto.brevo;

/**
 * Respuesta devuelta por la API de Brevo al enviar un correo transaccional exitosamente.
 *
 * @param messageId Identificador único asignado al mensaje enviado.
 */
public record BrevoSendEmailResponse(
        String messageId
) {}
