package com.sigea.demosigea_backend.dto.parametro;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear o actualizar un tipo de actividad del evento (HU-05, Criterio 1).
 *
 * @param nombre      Nombre del tipo (obligatorio, máx. 80, único por evento).
 * @param descripcion Características del tipo de actividad (opcional, máx. 255).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de un tipo de actividad del evento")
public record TipoActividadRequest(
        @Schema(description = "Nombre del tipo de actividad", example = "Taller")
        @NotBlank(message = "El nombre del tipo de actividad es obligatorio.")
        @Size(max = 80, message = "El nombre del tipo de actividad no puede superar los 80 caracteres.")
        String nombre,

        @Schema(description = "Características del tipo de actividad", example = "Sesión práctica de 2 a 4 horas con cupo limitado")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion
) {
}
