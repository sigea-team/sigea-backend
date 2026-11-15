package com.sigea.demosigea_backend.dto.inscripcion;

import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.InscripcionActividad;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Respuesta con los datos de una inscripción a una actividad de la agenda (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Detalle de la inscripción a una actividad de la agenda")
public record InscripcionActividadResponse(
        @Schema(description = "ID de la inscripción a la actividad", example = "10")
        Long inscripcionActividadId,

        @Schema(description = "ID de la inscripción al evento", example = "1")
        Long inscripcionId,

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

        @Schema(description = "Nombre de la sala asignada", example = "Auditorio Central")
        String salaNombre,

        @Schema(description = "Fecha de registro de la inscripción a la actividad", example = "2026-11-15T10:30:00")
        LocalDateTime fechaInscripcion
) {
    public static InscripcionActividadResponse fromEntity(InscripcionActividad ia) {
        Actividad a = ia.getActividad();
        String salaNombre = (a != null && a.getSala() != null) ? a.getSala().getNombre() : null;

        return new InscripcionActividadResponse(
                ia.getId(),
                ia.getInscripcion() != null ? ia.getInscripcion().getId() : null,
                a != null ? a.getId() : null,
                a != null ? a.getNombre() : null,
                a != null ? a.getFecha() : null,
                a != null ? a.getHoraInicio() : null,
                a != null ? a.getHoraFin() : null,
                salaNombre,
                ia.getFechaInscripcion()
        );
    }
}
