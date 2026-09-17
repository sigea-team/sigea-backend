package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petición para reenviar el enlace de verificación de correo")
public record ReenviarVerificacionRequest(
        @Schema(description = "Correo electrónico o nombre de usuario de la cuenta pendiente de verificación", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "Debe indicar su correo electrónico o nombre de usuario")
        String identificador
) {
}
