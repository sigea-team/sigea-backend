package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agenda publicada del evento, organizada por día y hora (HU-22, Criterio 3).
 *
 * @param eventoId         ID del evento.
 * @param eventoNombre     Nombre del evento.
 * @param fechaPublicacion Última publicación.
 * @param dias             Días publicados, en orden.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Agenda publicada del evento")
public record AgendaPublicaResponse(
        Long eventoId,
        String eventoNombre,
        LocalDateTime fechaPublicacion,
        List<DiaAgendaPublicaResponse> dias
) {
}
