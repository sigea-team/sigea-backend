package com.sigea.demosigea_backend.dto.encuesta;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sigea.demosigea_backend.model.PreguntaEncuesta;
import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Pregunta de la encuesta con su configuración (HU-25).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Pregunta de la encuesta")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreguntaEncuestaResponse(
        @Schema(example = "40") Long preguntaId,
        @Schema(example = "1") Integer orden,
        @Schema(example = "¿Qué tan satisfecho quedaste con el contenido?") String texto,
        @Schema(example = "escala") TipoRespuestaEncuesta tipoRespuesta,
        @Schema(example = "true") boolean obligatoria,
        @Schema(description = "Solo escala", example = "1") Integer escalaMinima,
        @Schema(description = "Solo escala", example = "5") Integer escalaMaxima,
        @Schema(description = "Solo opcion") List<OpcionEncuestaResponse> opciones
) {
    public static PreguntaEncuestaResponse fromEntity(PreguntaEncuesta p) {
        return new PreguntaEncuestaResponse(
                p.getId(),
                p.getOrden(),
                p.getTexto(),
                p.getTipoRespuesta(),
                p.isObligatoria(),
                p.getEscalaMinima(),
                p.getEscalaMaxima(),
                p.getTipoRespuesta() == TipoRespuestaEncuesta.opcion
                        ? p.getOpciones().stream().map(OpcionEncuestaResponse::fromEntity).toList()
                        : null);
    }
}
