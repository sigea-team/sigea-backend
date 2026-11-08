package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ubicación que la generación automática propone (o aplicó) para una actividad (HU-22, Criterio 1).
 *
 * @param actividadId   ID de la actividad.
 * @param nombre        Nombre de la actividad.
 * @param fecha         Fecha propuesta.
 * @param horaInicio    Hora de inicio propuesta.
 * @param horaFin       Hora de fin propuesta.
 * @param salaId        Sala propuesta.
 * @param salaNombre    Nombre de la sala.
 * @param ponenteId     Ponente (puede ser null).
 * @param ponenteNombre Nombre del ponente (puede ser null).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Ubicación propuesta para una actividad")
public record AsignacionPropuestaResponse(
        Long actividadId,
        String nombre,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long salaId,
        String salaNombre,
        Long ponenteId,
        String ponenteNombre
) {
}
