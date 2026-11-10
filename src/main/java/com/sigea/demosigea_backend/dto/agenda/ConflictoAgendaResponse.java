package com.sigea.demosigea_backend.dto.agenda;

import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cruce entre dos actividades de la agenda que impide publicarla (HU-22, Criterio 3: agenda validada sin
 * conflictos). Cada pareja aparece una sola vez.
 *
 * @param actividadId     ID de una de las actividades.
 * @param actividadNombre Nombre de esa actividad.
 * @param conflicto       Cruce con la otra actividad (tipo, datos de la otra y mensaje), como en HU-21.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Cruce entre dos actividades de la agenda")
public record ConflictoAgendaResponse(
        Long actividadId,
        String actividadNombre,
        ConflictoActividadResponse conflicto
) {
}
