package com.sigea.demosigea_backend.dto.memoria;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Proceso pendiente del evento al momento del cierre formal (CU-32, flujo alterno 1.1).
 *
 * @param codigo      tipo de pendiente.
 * @param cantidad    elementos pendientes.
 * @param descripcion explicación para el usuario.
 */
@Schema(description = "Proceso pendiente del evento al cierre formal")
public record PendienteCierreResponse(
        @Schema(example = "EVALUACIONES_SIN_FINALIZAR") String codigo,
        @Schema(example = "3") long cantidad,
        @Schema(example = "Asignaciones de evaluación pendientes o en proceso") String descripcion
) {
}
