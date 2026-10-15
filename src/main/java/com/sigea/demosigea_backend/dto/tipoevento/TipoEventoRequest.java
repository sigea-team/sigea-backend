package com.sigea.demosigea_backend.dto.tipoevento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para crear o actualizar un tipo de evento del catálogo.
 *
 * @param nombre      Nombre del tipo (obligatorio, máx. 50, único sin distinguir mayúsculas).
 * @param descripcion Descripción del tipo (opcional, máx. 255).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de un tipo de evento del catálogo")
public record TipoEventoRequest(
        @Schema(description = "Nombre del tipo de evento", example = "Congreso")
        @NotBlank(message = "El nombre del tipo de evento es obligatorio.")
        @Size(max = 50, message = "El nombre del tipo de evento no puede superar los 50 caracteres.")
        String nombre,

        @Schema(description = "Descripción del tipo de evento", example = "Evento de varios días con conferencias y ponencias evaluadas")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion
) {
}
