package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Una observación o ajuste requerido a una propuesta (HU-19, Criterio 1). El nivel de detalle es libre.
 *
 * @param descripcion    texto de la observación o del ajuste que debe hacer el autor.
 * @param requiereAjuste {@code true} (por defecto) si es un ajuste requerido que debe resolverse antes de aprobar la
 *                       versión definitiva; {@code false} si es solo un comentario.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Observación o ajuste requerido a una propuesta")
public record ObservacionRequest(
        @Schema(example = "Ampliar la sección de metodología indicando el tamaño de la muestra.")
        @NotBlank(message = "La descripción de la observación es obligatoria.")
        @Size(max = 2000, message = "La observación no puede superar 2000 caracteres.")
        String descripcion,

        @Schema(description = "Si es un ajuste requerido (bloquea la aprobación hasta resolverse). Por defecto true.",
                example = "true")
        Boolean requiereAjuste
) {
}
