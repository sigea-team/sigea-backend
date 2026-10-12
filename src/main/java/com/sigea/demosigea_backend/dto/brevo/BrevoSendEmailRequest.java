package com.sigea.demosigea_backend.dto.brevo;

import java.util.List;

/**
 * Payload de petición para el endpoint transactional email de Brevo (POST /v3/smtp/email).
 *
 * @param sender      Remitente del correo.
 * @param to          Lista de destinatarios.
 * @param subject     Asunto del correo.
 * @param htmlContent Contenido en formato HTML.
 */
public record BrevoSendEmailRequest(
        BrevoEmailAddress sender,
        List<BrevoEmailAddress> to,
        String subject,
        String htmlContent
) {}
