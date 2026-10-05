package com.sigea.demosigea_backend.dto.lineatematica;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación o modificación de una línea temática (HU-05).
 *
 * @param eventoId    ID del evento (opcional si se especifica en la URL).
 * @param nombre      Nombre de la línea temática (máx 120 caracteres).
 * @param descripcion Descripción o alcances de la línea temática (máx 255 caracteres).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Solicitud para registrar o actualizar una línea temática de un evento")
public record LineaTematicaRequest(

        @Schema(description = "ID del evento al que pertenece la línea temática", example = "1")
        Long eventoId,

        @Schema(description = "Nombre del eje temático", example = "Inteligencia Artificial y Aprendizaje Automático")
        @NotBlank(message = "El nombre de la línea temática es obligatorio.")
        @Size(max = 120, message = "El nombre de la línea temática no debe exceder los 120 caracteres.")
        String nombre,

        @Schema(description = "Descripción o áreas disciplinares cubiertas", example = "Investigaciones aplicadas en procesamiento de lenguaje natural, visión por computador y modelos generativos.")
        @Size(max = 255, message = "La descripción no debe exceder los 255 caracteres.")
        String descripcion
) {}
