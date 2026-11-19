package com.sigea.demosigea_backend.dto.encuesta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Confirmación de que se guardaron las respuestas (HU-25, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Confirmación del envío de la encuesta")
public record EnvioEncuestaResponse(
        @Schema(example = "7") Long encuestaId,
        @Schema(example = "¿Cómo te pareció el taller?") String titulo,
        @Schema(example = "true") boolean anonima,
        @Schema(example = "5") int preguntasRespondidas,
        LocalDateTime fechaRespuesta,
        @Schema(example = "Gracias por responder. Tus respuestas se guardaron de forma anónima.") String mensaje
) {
}
