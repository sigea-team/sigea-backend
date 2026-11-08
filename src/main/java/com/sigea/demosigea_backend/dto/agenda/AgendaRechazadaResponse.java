package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo de la respuesta 409 cuando no se publica la agenda (HU-22, Criterios 3 y 4).
 *
 * @param timestamp   Fecha y hora de la respuesta.
 * @param status      Código HTTP (409).
 * @param error       Nombre del error HTTP.
 * @param message     Explicación.
 * @param codigo      AGENDA_INCOMPLETA (actividades sin sala u horario), AGENDA_CON_CONFLICTOS o AGENDA_VACIA.
 * @param path        URI de la petición.
 * @param incompletas Actividades sin sala u horario.
 * @param conflictos  Cruces entre actividades.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Agenda no publicada por estar incompleta o con conflictos")
public record AgendaRechazadaResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        @Schema(example = "AGENDA_INCOMPLETA") String codigo,
        String path,
        List<ActividadIncompletaResponse> incompletas,
        List<ConflictoAgendaResponse> conflictos
) {
}
