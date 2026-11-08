package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Revisión de la agenda antes de publicarla (HU-22, Criterios 3 y 4): qué falta y qué se cruza.
 *
 * @param eventoId           ID del evento.
 * @param puedePublicar      true si se puede publicar ahora.
 * @param motivoNoPublicable Explicación cuando no se puede publicar (null si se puede).
 * @param publicada          true si la agenda ya está publicada (algún día publicado).
 * @param fechaPublicacion   Última publicación (null si nunca se publicó).
 * @param totalActividades   Actividades programadas (sin las canceladas ni las por programar).
 * @param dias               Estado de cada día con actividades o publicado antes.
 * @param incompletas        Criterio 4: actividades sin sala u horario.
 * @param conflictos         Cruces entre actividades.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Revisión de la agenda antes de publicarla")
public record ValidacionAgendaResponse(
        Long eventoId,
        boolean puedePublicar,
        String motivoNoPublicable,
        boolean publicada,
        LocalDateTime fechaPublicacion,
        int totalActividades,
        List<DiaAgendaEstadoResponse> dias,
        List<ActividadIncompletaResponse> incompletas,
        List<ConflictoAgendaResponse> conflictos
) {
}
