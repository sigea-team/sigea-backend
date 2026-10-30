package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Autorización explícita para modificar una propuesta bloqueada (HU-14, Criterio 5).
 *
 * @param motivo Justificación de la autorización (queda en la auditoría).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Autorización explícita para modificar una propuesta bloqueada")
public record AutorizarEdicionRequest(

        @Schema(description = "Motivo de la autorización", example = "Corrección de un error de forma solicitada por el comité")
        @NotBlank(message = "El motivo de la autorización es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
        String motivo
) {
}
