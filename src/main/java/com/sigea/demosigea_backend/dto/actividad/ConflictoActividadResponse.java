package com.sigea.demosigea_backend.dto.actividad;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Cruce de una actividad con otra ya programada (HU-21, Criterio 2). Los conflictos impiden guardar.
 *
 * @param tipo            HORARIO (se traslapan y no están marcadas como simultáneas), SALA (comparten sala) o
 *                        PONENTE (el mismo ponente en dos actividades a la vez).
 * @param actividadId     ID de la actividad con la que se cruza.
 * @param actividadNombre Nombre de esa actividad.
 * @param fecha           Fecha de esa actividad.
 * @param horaInicio      Hora de inicio de esa actividad.
 * @param horaFin         Hora de fin de esa actividad.
 * @param salaNombre      Sala de esa actividad.
 * @param mensaje         Explicación del conflicto.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Cruce con otra actividad de la agenda")
public record ConflictoActividadResponse(
        @Schema(example = "SALA") TipoConflicto tipo,
        @Schema(example = "42") Long actividadId,
        @Schema(example = "Conferencia inaugural") String actividadNombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        @Schema(example = "Auditorio principal") String salaNombre,
        String mensaje
) {

    /** Tipos de cruce que detecta la programación. */
    public enum TipoConflicto {
        /** Se traslapan en el tiempo y no están marcadas ambas como simultáneas. */
        HORARIO,
        /** Comparten la sala a la misma hora. */
        SALA,
        /** El mismo ponente tiene dos actividades a la misma hora. */
        PONENTE
    }
}
