package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * Un día de la agenda publicada, con sus actividades en orden de hora (HU-22, Criterio 3).
 *
 * @param fecha       Día.
 * @param actividades Actividades del día, por hora de inicio.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Día de la agenda publicada")
public record DiaAgendaPublicaResponse(
        LocalDate fecha,
        List<ActividadAgendaPublicaResponse> actividades
) {
}
