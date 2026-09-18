package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Objeto de Transferencia de Datos (DTO) para la captura del token
 * necesario en el proceso de confirmación de correo electrónico en SIGEA.
 * <p>
 * Contiene el código de verificación único generado durante el registro
 * o reenvío para validar la identidad de la cuenta.
 * </p>
 *
 * @param token Código único de validación (UUID/hash) enviado al correo del usuario. No puede estar en blanco.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Petición para validar el token de verificación de correo")
public record VerificarCorreoRequest(
        @Schema(description = "Token de verificación recibido por correo electrónico", example = "4c522da4-7d52-4467-bc18-2ad16a690d79")
        @NotBlank(message = "El token de verificación es obligatorio")
        String token
) {
}