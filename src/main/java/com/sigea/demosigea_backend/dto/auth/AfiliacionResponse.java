package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO que representa una opción de afiliación institucional en el catálogo del sistema.
 *
 * @param id Identificador de la afiliación en la base de datos.
 * @param nombreAfiliacion Nombre o descripción de la afiliación institucional.
 */
@Schema(description = "Respuesta con los datos de una afiliación institucional del catálogo")
public record AfiliacionResponse(
        @Schema(description = "ID de la afiliación institucional", example = "1")
        Long id,

        @Schema(description = "Nombre o descripción de la afiliación", example = "Estudiante UFPS")
        String nombreAfiliacion
) {
}
