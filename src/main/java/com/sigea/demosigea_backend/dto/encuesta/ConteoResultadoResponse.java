package com.sigea.demosigea_backend.dto.encuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Cantidad de respuestas para un valor de escala o una opción (HU-25, Criterio 4).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Cantidad de respuestas por valor u opción")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConteoResultadoResponse(
        @Schema(description = "Valor de la escala (solo escala)", example = "4") Integer valor,
        @Schema(description = "Opción (solo opcion)", example = "12") Long opcionId,
        @Schema(example = "4") String etiqueta,
        @Schema(example = "11") long cantidad,
        @Schema(description = "Porcentaje sobre quienes respondieron la pregunta", example = "35.5") BigDecimal porcentaje
) {
}
