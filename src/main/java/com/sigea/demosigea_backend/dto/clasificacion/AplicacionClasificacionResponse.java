package com.sigea.demosigea_backend.dto.clasificacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de aplicar la clasificación a todas las propuestas de una convocatoria (HU-18, Criterios 1 y 3).
 *
 * @param convocatoriaId ID de la convocatoria.
 * @param clasificadas   Propuestas clasificadas (o que ya tenían aplicada su clasificación).
 * @param omitidas       Propuestas que no se clasificaron porque su evaluación no está completa.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de aplicar la clasificación a las propuestas de una convocatoria")
public record AplicacionClasificacionResponse(
        @Schema(example = "10") Long convocatoriaId,
        List<PropuestaClasificacionResponse> clasificadas,
        List<PropuestaOmitidaResponse> omitidas
) {

    /**
     * Propuesta que no se pudo clasificar.
     *
     * @param propuestaId ID de la propuesta.
     * @param titulo      Título de la propuesta.
     * @param motivo      Por qué no se clasificó.
     */
    @Schema(description = "Propuesta que no se pudo clasificar")
    public record PropuestaOmitidaResponse(
            @Schema(example = "41") Long propuestaId,
            @Schema(example = "Robótica educativa") String titulo,
            @Schema(example = "Tiene 1 evaluación pendiente de 2 asignadas.") String motivo
    ) {
    }
}
