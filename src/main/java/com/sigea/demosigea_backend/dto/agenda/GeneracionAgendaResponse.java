package com.sigea.demosigea_backend.dto.agenda;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de la generación automática de la agenda (HU-22, Criterios 1 y 2).
 *
 * @param eventoId      ID del evento.
 * @param aplicada      false: es solo una propuesta (no se guardó nada); true: las asignaciones ya se guardaron.
 * @param totalPorUbicar Actividades que se intentó ubicar.
 * @param asignadas     Actividades ubicadas sin conflictos.
 * @param pendientes    Actividades que quedan pendientes de asignación manual, con el motivo.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Propuesta de agenda generada automáticamente")
public record GeneracionAgendaResponse(
        Long eventoId,
        boolean aplicada,
        int totalPorUbicar,
        List<AsignacionPropuestaResponse> asignadas,
        List<ActividadPendienteResponse> pendientes
) {
}
