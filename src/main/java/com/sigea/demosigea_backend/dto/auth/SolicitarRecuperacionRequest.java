package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) que captura la solicitud de un usuario
 * para iniciar el flujo de recuperación de contraseña (HU-32).
 * <p>
 * Solo requiere el correo electrónico de la cuenta. El sistema responde siempre
 * con el mismo mensaje genérico exista o no ese correo (Criterio 4), por lo que
 * este DTO no necesita transportar más información.
 * </p>
 *
 * @param correo Correo electrónico de la cuenta que solicita recuperar su contraseña.
 *
 * @author SIGEA Development Team
 * @since 2026
 */
@Schema(description = "Petición para iniciar la recuperación de contraseña")
public record SolicitarRecuperacionRequest(
        @Schema(description = "Correo electrónico de la cuenta", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato de correo electrónico no es válido")
        String correo
) {
}
