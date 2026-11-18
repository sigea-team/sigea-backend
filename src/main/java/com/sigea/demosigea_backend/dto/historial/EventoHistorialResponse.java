package com.sigea.demosigea_backend.dto.historial;

import com.sigea.demosigea_backend.model.EstadoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Evento del historial de una persona con los roles, actividades y propuestas en que participó (HU-27, Criterio 1).
 *
 * @param eventoId         Identificador del evento.
 * @param nombre           Nombre del evento.
 * @param tipoEvento       Nombre del tipo de evento (null si no tiene).
 * @param semestre         Semestre académico del evento.
 * @param fechaInicio      Fecha de inicio.
 * @param fechaFin         Fecha de finalización.
 * @param estado           Estado del evento.
 * @param roles            Roles de la persona en el evento.
 * @param temaConferencia  Tema registrado como conferencista o ponente invitado (null si no aplica).
 * @param actividades      Actividades en las que participó.
 * @param propuestas       Propuestas postuladas en el evento.
 */
@Schema(description = "Evento en el que participó la persona")
public record EventoHistorialResponse(
        Long eventoId,
        String nombre,
        String tipoEvento,
        String semestre,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoEvento estado,
        Set<RolParticipacion> roles,
        String temaConferencia,
        List<ActividadHistorialResponse> actividades,
        List<PropuestaHistorialResponse> propuestas
) {
}
