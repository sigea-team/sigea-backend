package com.sigea.demosigea_backend.dto.sala;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO de respuesta al actualizar la disponibilidad horaria de un ponente — HU-Salas, Criterio 5.
 * <p>
 * Incluye las nuevas franjas guardadas y la lista de conflictos detectados (actividades
 * programadas con ese ponente que quedan fuera de la disponibilidad). Si no hay conflictos,
 * la lista está vacía. Es una advertencia informativa, no un bloqueo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de actualizar la disponibilidad de un ponente, incluyendo posibles conflictos")
public record ActualizacionDisponibilidadPonenteResponse(
        @Schema(description = "ID del ponente (persona)", example = "7") Long personaId,
        @Schema(description = "ID del evento", example = "1") Long eventoId,
        @Schema(description = "Franjas de disponibilidad guardadas") List<DisponibilidadPonenteResponse> disponibilidades,
        @Schema(description = "Actividades en conflicto con la nueva disponibilidad") List<ConflictoDisponibilidadResponse> conflictos,
        @Schema(description = "Indica si se detectaron conflictos", example = "false") boolean tieneConflictos
) {
}
