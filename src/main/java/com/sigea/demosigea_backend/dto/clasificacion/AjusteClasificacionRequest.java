package com.sigea.demosigea_backend.dto.clasificacion;

import com.sigea.demosigea_backend.model.ClasificacionResultado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajuste manual de la clasificación de una propuesta (HU-18, Criterio 2).
 *
 * @param clasificacion nueva clasificación: {@code aprobada}, {@code aprobada_con_ajustes} o {@code rechazada}.
 * @param motivo        justificación del ajuste; queda en el historial.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Ajuste manual de la clasificación de una propuesta")
public record AjusteClasificacionRequest(
        @Schema(example = "aprobada_con_ajustes", allowableValues = {"aprobada", "aprobada_con_ajustes", "rechazada"})
        @NotNull(message = "La clasificación es obligatoria.")
        ClasificacionResultado clasificacion,

        @Schema(example = "El comité acordó solicitar ajustes de forma antes de aceptarla.")
        @NotBlank(message = "El motivo del ajuste es obligatorio.")
        @Size(min = 10, max = 1000, message = "El motivo debe tener entre 10 y 1000 caracteres.")
        String motivo
) {
}
