package com.sigea.demosigea_backend.dto.propuesta;

import com.sigea.demosigea_backend.model.RolAutor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para modificar el rol u orden de autoría de una persona vinculada a una propuesta.
 *
 * @param rol   Nuevo rol del autor (obligatorio).
 * @param orden Nuevo orden de autoría (opcional).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para actualizar el rol u orden de un autor en la propuesta")
public record ActualizarAutorRequest(

        @Schema(description = "Nuevo rol del autor en la propuesta", example = "ponente")
        @NotNull(message = "El rol es obligatorio")
        RolAutor rol,

        @Schema(description = "Nuevo orden de autoría", example = "2")
        Integer orden
) {
}
