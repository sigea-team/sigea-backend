package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Actividad de un evento dentro de la consulta por periodo (HU-27, Criterio 3).
 *
 * @param actividadId   Identificador de la actividad.
 * @param nombre        Nombre.
 * @param tipoActividad Nombre del tipo de actividad.
 * @param fecha         Fecha (null si está por programar).
 * @param horaInicio    Hora de inicio.
 * @param horaFin       Hora de finalización.
 * @param sala          Nombre de la sala (null si no tiene).
 * @param ponente       Nombre completo del ponente (null si no tiene).
 * @param estado        Estado de la actividad.
 * @param inscritos     Personas inscritas a la actividad.
 */
@Schema(description = "Actividad del evento en el periodo consultado")
public record ActividadPeriodoResponse(
        Long actividadId,
        String nombre,
        String tipoActividad,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala,
        String ponente,
        String estado,
        long inscritos
) {
}
