package com.sigea.demosigea_backend.dto.brevo;

/**
 * Archivo adjunto de un correo transaccional de Brevo (campo {@code attachment}).
 *
 * @param content contenido del archivo en Base64.
 * @param name    nombre del archivo con su extensión (Brevo deduce el tipo por la extensión).
 */
public record BrevoAttachment(
        String content,
        String name
) {}
