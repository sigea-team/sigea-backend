package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO que describe un conflicto de disponibilidad detectado al actualizar la disponibilidad
 * horaria de una sala o un ponente — HU-Salas, Criterio 5.
 * <p>
 * Un conflicto ocurre cuando existe una actividad programada cuya fecha y hora NO quedan
 * cubiertas por ninguna franja de disponibilidad tras la actualización.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Conflicto detectado entre una actividad programada y la nueva disponibilidad")
public record ConflictoDisponibilidadResponse(
        @Schema(description = "ID de la actividad en conflicto", example = "42") Long actividadId,
        @Schema(description = "Nombre de la actividad", example = "Taller de Inteligencia Artificial") String nombreActividad,
        @Schema(description = "Fecha de la actividad", example = "2026-11-15") LocalDate fecha,
        @Schema(description = "Hora de inicio de la actividad", example = "10:00") LocalTime horaInicio,
        @Schema(description = "Hora de fin de la actividad", example = "11:00") LocalTime horaFin,
        @Schema(description = "Descripción del conflicto", example = "La actividad queda fuera de la disponibilidad horaria registrada") String descripcionConflicto
) {
}
