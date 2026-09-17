package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmación de reenvío de correo de verificación")
public record ReenviarVerificacionResponse(
        @Schema(description = "Mensaje descriptivo", example = "Se ha enviado un nuevo enlace de verificación a su correo electrónico.")
        String mensaje,

        @Schema(description = "Correo destinatario del enlace", example = "carlos.gomez@universidad.edu.co")
        String correo
) {
}
