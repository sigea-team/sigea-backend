package com.sigea.demosigea_backend.dto.indicador;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Grupo de indicadores del dashboard (participación, asistencia, actividades, propuestas, satisfacción,
 * presupuesto) e indicación de si tiene datos registrados (HU-29, Criterio 2).
 *
 * @param codigo   código del grupo.
 * @param nombre   nombre visible.
 * @param hayDatos {@code false} si el evento no tiene información registrada para este grupo.
 * @param mensaje  aviso de ausencia de datos; {@code null} si hay datos.
 */
@Schema(description = "Grupo de indicadores del dashboard")
public record GrupoIndicadoresResponse(
        @Schema(example = "ASISTENCIA") String codigo,
        @Schema(example = "Asistencia") String nombre,
        @Schema(example = "true") boolean hayDatos,
        @Schema(nullable = true) String mensaje
) {
}
