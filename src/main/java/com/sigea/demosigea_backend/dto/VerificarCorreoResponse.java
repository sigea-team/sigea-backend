package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de la verificación de correo electrónico")
public record VerificarCorreoResponse(
        @Schema(description = "Mensaje informativo del resultado", example = "Su correo electrónico ha sido verificado con éxito. Ya puede iniciar sesión.")
        String mensaje,

        @Schema(description = "Indica si la verificación fue exitosa", example = "true")
        boolean verificado
) {
}
