package com.sigea.demosigea_backend.dto.clasificacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Umbrales de clasificación vigentes de una convocatoria (HU-18).
 *
 * @param convocatoriaId        ID de la convocatoria.
 * @param umbralAceptacion      puntuación mínima para aprobada.
 * @param umbralAjustes         puntuación mínima para aprobada con ajustes.
 * @param propuestasPorReaplicar propuestas ya clasificadas automáticamente cuya clasificación cambia con los nuevos
 *                              umbrales; quedan igual hasta volver a aplicar la clasificación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Umbrales de clasificación vigentes de una convocatoria")
public record UmbralesClasificacionResponse(
        @Schema(example = "10") Long convocatoriaId,
        @Schema(example = "7.00") BigDecimal umbralAceptacion,
        @Schema(example = "5.00") BigDecimal umbralAjustes,
        @Schema(example = "0") long propuestasPorReaplicar
) {
}
