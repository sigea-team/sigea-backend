package com.sigea.demosigea_backend.dto.parametro;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear o actualizar una línea temática del evento (HU-05, Criterio 1).
 *
 * @param nombre      Nombre de la línea (obligatorio, máx. 120, único por evento).
 * @param descripcion Alcance o características de la línea (opcional, máx. 255).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de una línea temática del evento")
public record LineaTematicaRequest(
        @Schema(description = "Nombre de la línea temática", example = "Inteligencia artificial y ciencia de datos")
        @NotBlank(message = "El nombre de la línea temática es obligatorio.")
        @Size(max = 120, message = "El nombre de la línea temática no puede superar los 120 caracteres.")
        String nombre,

        @Schema(description = "Alcance de la línea temática", example = "Aprendizaje automático, analítica y visualización de datos")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion
) {
}
