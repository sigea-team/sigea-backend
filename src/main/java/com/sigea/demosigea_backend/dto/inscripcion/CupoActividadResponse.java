package com.sigea.demosigea_backend.dto.inscripcion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Consulta de aforo y cupos disponibles de una actividad (HU-23, Criterio 2, 4 y 5).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Disponibilidad de cupos y aforo de una actividad")
public record CupoActividadResponse(
        @Schema(description = "ID de la actividad", example = "25")
        Long actividadId,

        @Schema(description = "Nombre de la actividad", example = "Taller de Inteligencia Artificial")
        String actividadNombre,

        @Schema(description = "Aforo máximo permitido por la sala asignada", example = "50")
        Integer aforoMaximo,

        @Schema(description = "Inscripciones activas actuales", example = "45")
        long inscritos,

        @Schema(description = "Cupos libres restantes", example = "5")
        long cuposDisponibles,

        @Schema(description = "Indica si el cupo se encuentra agotado (aforo alcanzado)", example = "false")
        boolean cupoAgotado,

        @Schema(description = "Indica si el usuario actual ya se encuentra inscrito", example = "false")
        boolean estaInscrito
) {
}
