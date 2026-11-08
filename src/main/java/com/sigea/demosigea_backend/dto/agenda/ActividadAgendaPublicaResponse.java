package com.sigea.demosigea_backend.dto.agenda;

import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

/**
 * Actividad tal como la ven los participantes en la agenda publicada (HU-22, Criterio 3).
 *
 * @param id            ID de la actividad.
 * @param nombre        Nombre.
 * @param tipoActividad Tipo de actividad.
 * @param horaInicio    Hora de inicio.
 * @param horaFin       Hora de fin.
 * @param sala          Nombre de la sala.
 * @param ubicacion     Ubicación de la sala física (null en salas virtuales, para no publicar el enlace).
 * @param ponente       Nombre del ponente (puede ser null).
 * @param modalidad     Modalidad.
 * @param lineaTematica Línea temática (puede ser null).
 * @param simultanea    Si ocurre en paralelo con otras actividades.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad de la agenda publicada")
public record ActividadAgendaPublicaResponse(
        Long id,
        String nombre,
        String tipoActividad,
        LocalTime horaInicio,
        LocalTime horaFin,
        String sala,
        String ubicacion,
        String ponente,
        ModalidadEvento modalidad,
        String lineaTematica,
        boolean simultanea
) {
}
