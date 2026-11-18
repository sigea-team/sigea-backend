package com.sigea.demosigea_backend.dto.brevo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Payload de petición para el endpoint transactional email de Brevo (POST /v3/smtp/email).
 *
 * @param sender      Remitente del correo.
 * @param to          Lista de destinatarios.
 * @param subject     Asunto del correo.
 * @param htmlContent Contenido en formato HTML.
 * @param attachment  Archivos adjuntos (HU-24: imagen del código QR). No se envía si es {@code null}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BrevoSendEmailRequest(
        BrevoEmailAddress sender,
        List<BrevoEmailAddress> to,
        String subject,
        String htmlContent,
        List<BrevoAttachment> attachment
) {

    /** Correo sin adjuntos. */
    public BrevoSendEmailRequest(BrevoEmailAddress sender, List<BrevoEmailAddress> to, String subject, String htmlContent) {
        this(sender, to, subject, htmlContent, null);
    }
}
