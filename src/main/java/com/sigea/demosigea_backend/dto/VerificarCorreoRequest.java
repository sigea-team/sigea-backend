package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para validar el token de verificación de correo")
public record VerificarCorreoRequest(
        @Schema(description = "Token de verificación recibido por correo electrónico", example = "4c522da4-7d52-4467-bc18-2ad16a690d79")
        @NotBlank(message = "El token de verificación es obligatorio")
        String token
) {
}
