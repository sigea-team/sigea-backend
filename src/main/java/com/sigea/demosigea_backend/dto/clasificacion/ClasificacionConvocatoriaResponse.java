package com.sigea.demosigea_backend.dto.clasificacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Propuestas de una convocatoria agrupadas por resultado (HU-18; CU-31, paso 3).
 *
 * @param convocatoriaId   ID de la convocatoria.
 * @param umbralAceptacion Umbral vigente para aprobada.
 * @param umbralAjustes    Umbral vigente para aprobada con ajustes.
 * @param totales          Número de propuestas por grupo: {@code aprobada}, {@code aprobada_con_ajustes},
 *                         {@code rechazada}, {@code sin_clasificar} (calculada pero sin aplicar) y
 *                         {@code evaluacion_incompleta}.
 * @param propuestas       Propuestas ordenadas por grupo y, dentro de cada grupo, por puntuación descendente.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Propuestas de una convocatoria agrupadas por resultado de clasificación")
public record ClasificacionConvocatoriaResponse(
        @Schema(example = "10") Long convocatoriaId,
        @Schema(example = "7.00") BigDecimal umbralAceptacion,
        @Schema(example = "5.00") BigDecimal umbralAjustes,
        Map<String, Long> totales,
        List<PropuestaClasificacionResponse> propuestas
) {
}
