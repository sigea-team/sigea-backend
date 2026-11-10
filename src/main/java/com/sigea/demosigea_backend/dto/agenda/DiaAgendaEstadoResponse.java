package com.sigea.demosigea_backend.dto.agenda;

import com.sigea.demosigea_backend.model.EstadoAgenda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Estado de la agenda de un día del evento, para el organizador (HU-22).
 *
 * @param fecha            Día.
 * @param actividades      Actividades programadas ese día (sin las canceladas).
 * @param estado           publicada o no_publicada.
 * @param fechaPublicacion Última publicación de ese día (null si nunca se publicó).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Estado de la agenda de un día")
public record DiaAgendaEstadoResponse(
        LocalDate fecha,
        int actividades,
        EstadoAgenda estado,
        LocalDateTime fechaPublicacion
) {
}
