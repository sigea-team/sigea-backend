package com.sigea.demosigea_backend.dto.encuesta;

import com.sigea.demosigea_backend.model.TipoRespuestaEncuesta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pregunta de la encuesta (HU-25). Las de escala usan el rango indicado (por defecto 1 a 5); las de opción
 * necesitan entre 2 y 20 opciones distintas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Pregunta de la encuesta")
public record PreguntaEncuestaRequest(
        @Schema(description = "Enunciado", example = "¿Qué tan satisfecho quedaste con el contenido?")
        @NotBlank(message = "El texto de la pregunta es obligatorio")
        @Size(max = 1000, message = "La pregunta no puede superar 1000 caracteres")
        String texto,

        @Schema(description = "Tipo de respuesta: escala, texto u opcion", example = "escala")
        @NotNull(message = "El tipo de respuesta es obligatorio")
        TipoRespuestaEncuesta tipoRespuesta,

        @Schema(description = "Si es obligatoria (por defecto true)", example = "true")
        Boolean obligatoria,

        @Schema(description = "Valor mínimo de la escala (0 a 10, por defecto 1)", example = "1")
        @Min(value = 0, message = "La escala mínima no puede ser menor que 0")
        @Max(value = 10, message = "La escala mínima no puede ser mayor que 10")
        Integer escalaMinima,

        @Schema(description = "Valor máximo de la escala (0 a 10, por defecto 5)", example = "5")
        @Min(value = 0, message = "La escala máxima no puede ser menor que 0")
        @Max(value = 10, message = "La escala máxima no puede ser mayor que 10")
        Integer escalaMaxima,

        @Schema(description = "Opciones (solo tipo opcion)", example = "[\"Sí\", \"No\", \"Tal vez\"]")
        @Size(max = 20, message = "Una pregunta no puede tener más de 20 opciones")
        List<String> opciones
) {
}
