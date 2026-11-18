package com.sigea.demosigea_backend.dto.periodo;

import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Evento dentro de la consulta por periodo académico (HU-27, Criterio 3).
 *
 * @param eventoId         Identificador del evento.
 * @param nombre           Nombre.
 * @param tipoEvento       Nombre del tipo de evento (null si no tiene).
 * @param semestre         Semestre académico.
 * @param fechaInicio      Fecha de inicio.
 * @param fechaFin         Fecha de finalización.
 * @param estado           Estado del evento.
 * @param modalidad        Modalidad del evento.
 * @param inscritos        Personas inscritas al evento.
 * @param totalActividades Actividades listadas (las que cumplen el filtro de tipo de actividad, si lo hay).
 * @param actividades      Actividades del evento.
 */
@Schema(description = "Evento del periodo consultado")
public record EventoPeriodoResponse(
        Long eventoId,
        String nombre,
        String tipoEvento,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoEvento estado,
        ModalidadEvento modalidad,
        long inscritos,
        int totalActividades,
        List<ActividadPeriodoResponse> actividades
) {
}
