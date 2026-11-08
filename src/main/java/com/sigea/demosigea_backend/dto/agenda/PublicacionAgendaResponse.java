package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de publicar la agenda (HU-22, Criterio 3).
 *
 * @param eventoId         ID del evento.
 * @param fechaPublicacion Momento de la publicación.
 * @param diasPublicados   Días que quedaron visibles para los participantes.
 * @param totalActividades Actividades publicadas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Agenda publicada")
public record PublicacionAgendaResponse(
        Long eventoId,
        LocalDateTime fechaPublicacion,
        List<LocalDate> diasPublicados,
        int totalActividades
) {
}
