package com.sigea.demosigea_backend.dto.brevo;

/**
 * Representa una dirección de correo y nombre asociado para la API de Brevo.
 *
 * @param email Dirección de correo electrónico.
 * @param name  Nombre asociado (opcional).
 */
public record BrevoEmailAddress(
        String email,
        String name
) {}
