package com.sigea.demosigea_backend.dto.agenda;

import com.sigea.demosigea_backend.service.GeneradorAgenda.MotivoPendiente;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Actividad que la generación no pudo ubicar sin conflictos y queda pendiente de asignación manual (HU-22,
 * Criterio 2).
 *
 * @param actividadId     ID de la actividad.
 * @param nombre          Nombre de la actividad.
 * @param duracionMinutos Duración.
 * @param ponenteId       Ponente (puede ser null).
 * @param ponenteNombre   Nombre del ponente (puede ser null).
 * @param motivo          SIN_SALA_DISPONIBLE, SIN_DISPONIBILIDAD_PONENTE, SIN_FRANJA_COMUN o SIN_ESPACIO_LIBRE.
 * @param mensaje         Explicación para el organizador.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad pendiente de asignación manual")
public record ActividadPendienteResponse(
        Long actividadId,
        String nombre,
        Integer duracionMinutos,
        Long ponenteId,
        String ponenteNombre,
        @Schema(example = "SIN_ESPACIO_LIBRE") MotivoPendiente motivo,
        String mensaje
) {
}
