package com.sigea.demosigea_backend.dto.encuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado consolidado de una pregunta (HU-25, Criterio 4).
 *
 * @param totalRespuestas personas que respondieron esta pregunta.
 * @param omitidas        personas que enviaron la encuesta sin responder esta pregunta (solo opcionales).
 * @param promedio        promedio de la escala (null si nadie la respondió).
 * @param distribucion    cantidad por valor de la escala (todos los valores del rango) o por opción (todas).
 * @param respuestasTexto respuestas de texto libre.
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado consolidado de una pregunta")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultadoPreguntaResponse(
        @Schema(example = "40") Long preguntaId,
        @Schema(example = "1") Integer orden,
        @Schema(example = "¿Qué tan satisfecho quedaste con el contenido?") String texto,
        @Schema(example = "escala") TipoRespuestaEncuesta tipoRespuesta,
        @Schema(example = "true") boolean obligatoria,
        @Schema(example = "31") long totalRespuestas,
        @Schema(example = "0") long omitidas,
        @Schema(description = "Solo escala", example = "1") Integer escalaMinima,
        @Schema(description = "Solo escala", example = "5") Integer escalaMaxima,
        @Schema(description = "Solo escala", example = "4.23") BigDecimal promedio,
        @Schema(description = "Escala y opción") List<ConteoResultadoResponse> distribucion,
        @Schema(description = "Solo texto") List<RespuestaTextoResponse> respuestasTexto
) {
}
