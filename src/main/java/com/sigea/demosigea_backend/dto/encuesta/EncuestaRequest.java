package com.sigea.demosigea_backend.dto.encuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Configuración de una encuesta de satisfacción (HU-25, Criterio 1). Al editar, las preguntas enviadas reemplazan
 * a las anteriores.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Configuración de una encuesta de satisfacción")
public record EncuestaRequest(
        @Schema(description = "Título visible para los participantes", example = "¿Cómo te pareció el taller?")
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @Schema(description = "Instrucciones o presentación", example = "Tus respuestas nos ayudan a mejorar.")
        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String descripcion,

        @Schema(description = "Actividad evaluada; vacío para la encuesta general del evento", example = "25")
        Long actividadId,

        @Schema(description = "Si es true, las respuestas se guardan sin identificar al participante", example = "true")
        Boolean anonima,

        @Schema(description = "Fecha y hora límite para responder (opcional)", example = "2026-11-30T23:59:00")
        LocalDateTime fechaCierre,

        @Schema(description = "Preguntas en el orden en que se muestran")
        @Size(max = 50, message = "La encuesta no puede tener más de 50 preguntas")
        List<@Valid PreguntaEncuestaRequest> preguntas
) {
}
