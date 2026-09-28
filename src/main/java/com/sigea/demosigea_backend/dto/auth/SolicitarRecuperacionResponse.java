package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de Transferencia de Datos (DTO) de respuesta tras solicitar la
 * recuperación de contraseña.
 * <p>
 * El mensaje es <b>siempre el mismo</b>, exista o no una cuenta con ese
 * correo (HU-32, Criterio 4): nunca se debe usar este DTO para confirmar
 * ni descartar la existencia de una cuenta.
 * </p>
 *
 * @param mensaje Mensaje genérico de confirmación.
 *
 * @author SIGEA Development Team
 * @since 2026
 */
@Schema(description = "Confirmación genérica de la solicitud de recuperación de contraseña")
public record SolicitarRecuperacionResponse(
        @Schema(
                description = "Mensaje genérico. No revela si el correo existe en el sistema.",
                example = "Si el correo está registrado, hemos enviado un enlace de recuperación con vigencia limitada."
        )
        String mensaje
) {
}
