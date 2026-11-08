package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO que representa una franja horaria ocupada por un evento o actividad en una sala — HU-Salas.
 * Permite que los horarios asignados dejen de figurar como disponibles para futuras asignaciones.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Franja horaria ocupada por un evento o actividad en la sala")
public record OcupacionSalaResponse(
        @Schema(description = "ID de la actividad", example = "10")
        Long actividadId,

        @Schema(description = "Nombre de la actividad", example = "Conferencia Inaugural")
        String actividadNombre,

        @Schema(description = "ID del evento", example = "1")
        Long eventoId,

        @Schema(description = "Nombre del evento", example = "Seminario de IA 2026")
        String eventoNombre,

        @Schema(description = "Fecha de ocupación", example = "2026-11-15")
        LocalDate fecha,

        @Schema(description = "Hora de inicio", example = "08:00:00")
        LocalTime horaInicio,

        @Schema(description = "Hora de fin", example = "12:00:00")
        LocalTime horaFin
) {
}
