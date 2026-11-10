package com.sigea.demosigea_backend.dto.rubrica;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * HU-15, Criterio 4: impacto que tendría modificar la rúbrica activa de una convocatoria.
 * Permite al frontend advertir antes de enviar los cambios.
 *
 * @param rubricaId                 ID de la rúbrica activa ({@code null} si la convocatoria no tiene).
 * @param calificacionesRegistradas calificaciones registradas con sus criterios.
 * @param asignacionesAfectadas     asignaciones que tendrían que volver a evaluarse.
 * @param propuestasAfectadas       propuestas afectadas.
 * @param requiereConfirmacion      {@code true} si modificarla exige {@code confirmar=true}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record ImpactoRubricaResponse(
        @Schema(example = "4") Long rubricaId,
        @Schema(example = "12") long calificacionesRegistradas,
        @Schema(example = "4") long asignacionesAfectadas,
        @Schema(example = "3") long propuestasAfectadas,
        @Schema(example = "true") boolean requiereConfirmacion,
        @Schema(description = "Propuestas de la convocatoria con la clasificación aplicada (HU-18)", example = "0")
        long propuestasClasificadas,
        @Schema(description = "true si la rúbrica ya no se puede modificar porque la clasificación empezó",
                example = "false")
        boolean bloqueada
) {
}
