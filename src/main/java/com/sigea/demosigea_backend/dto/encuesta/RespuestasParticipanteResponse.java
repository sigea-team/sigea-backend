package com.sigea.demosigea_backend.dto.encuesta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuestas de un participante en una encuesta identificada (HU-25, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuestas de un participante (solo encuestas identificadas)")
public record RespuestasParticipanteResponse(
        @Schema(example = "10") Long personaId,
        @Schema(example = "María Pérez") String participanteNombre,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "maria.perez@ufps.edu.co") String correo,
        LocalDateTime fechaRespuesta,
        List<Detalle> respuestas
) {
    /** Respuesta a una pregunta. */
    @Schema(name = "RespuestaParticipanteDetalle", description = "Respuesta a una pregunta")
    public record Detalle(
            @Schema(example = "40") Long preguntaId,
            @Schema(example = "1") Integer orden,
            @Schema(example = "¿Qué tan satisfecho quedaste con el contenido?") String pregunta,
            @Schema(example = "4") String respuesta
    ) {
    }
}
