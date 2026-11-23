package com.sigea.demosigea_backend.dto.historial;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Actividad en la que participó la persona dentro de un evento (HU-27, Criterio 1).
 *
 * @param actividadId   Identificador de la actividad.
 * @param nombre        Nombre de la actividad.
 * @param tipoActividad Nombre del tipo de actividad (catálogo del evento).
 * @param fecha         Fecha de realización (null si aún está por programar).
 * @param horaInicio    Hora de inicio (null si aún está por programar).
 * @param horaFin       Hora de finalización (null si aún está por programar).
 * @param estado        Estado de la actividad.
 * @param rol           Rol de la persona en la actividad (PARTICIPANTE o PONENTE).
 */
@Schema(description = "Actividad en la que participó la persona")
public record ActividadHistorialResponse(
        Long actividadId,
        String nombre,
        String tipoActividad,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String estado,
        RolParticipacion rol
) {
}
