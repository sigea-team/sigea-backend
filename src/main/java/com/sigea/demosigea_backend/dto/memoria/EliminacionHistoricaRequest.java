package com.sigea.demosigea_backend.dto.memoria;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Eliminación con autorización especial de un evento cerrado formalmente (HU-28, Criterio 3).
 *
 * @param nombreEvento nombre exacto del evento, como confirmación.
 * @param motivo       motivo de la eliminación; queda en la auditoría.
 */
@Schema(description = "Eliminación con autorización especial de un evento de la memoria histórica (HU-28)")
public record EliminacionHistoricaRequest(
        @Schema(description = "Nombre exacto del evento, como confirmación", example = "Congreso de Ingeniería 2025")
        @NotBlank(message = "Escriba el nombre del evento para confirmar la eliminación")
        String nombreEvento,

        @Schema(description = "Motivo de la eliminación (mínimo 20 caracteres)",
                example = "Evento de prueba creado por error durante la capacitación")
        @NotBlank(message = "El motivo es obligatorio")
        @Size(min = 20, max = 1000, message = "El motivo debe tener entre 20 y 1000 caracteres")
        String motivo
) {
}
