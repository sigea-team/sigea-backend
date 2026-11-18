package com.sigea.demosigea_backend.dto.asistencia;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Asistencias registradas en una actividad, con el total de inscritos para el control del organizador (HU-24).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Asistencias registradas en una actividad")
public record AsistenciasActividadResponse(
        @Schema(description = "ID de la actividad", example = "25")
        Long actividadId,

        @Schema(description = "Nombre de la actividad", example = "Taller de Inteligencia Artificial")
        String actividadNombre,

        @Schema(description = "Fecha de la actividad", example = "2026-11-20")
        LocalDate fecha,

        @Schema(description = "Hora de inicio", example = "14:00:00")
        LocalTime horaInicio,

        @Schema(description = "Hora de finalización", example = "16:00:00")
        LocalTime horaFin,

        @Schema(description = "Participantes inscritos en la actividad", example = "30")
        long inscritos,

        @Schema(description = "Asistencias registradas", example = "24")
        long asistentes,

        @Schema(description = "Asistencias en orden de registro")
        List<AsistenciaResponse> asistencias
) {}
