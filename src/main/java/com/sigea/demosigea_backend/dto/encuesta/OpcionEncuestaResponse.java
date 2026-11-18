package com.sigea.demosigea_backend.dto.encuesta;

import com.sigea.demosigea_backend.model.OpcionPreguntaEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Opción de una pregunta de selección única (HU-25).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Opción de respuesta")
public record OpcionEncuestaResponse(
        @Schema(example = "12") Long opcionId,
        @Schema(example = "Sí") String texto,
        @Schema(example = "1") Integer orden
) {
    public static OpcionEncuestaResponse fromEntity(OpcionPreguntaEncuesta o) {
        return new OpcionEncuestaResponse(o.getId(), o.getTexto(), o.getOrden());
    }
}
