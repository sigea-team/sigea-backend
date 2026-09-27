package com.sigea.demosigea_backend.dto.rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * DTO para la creación y actualización de roles.
 */
public record RolRequest(
        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max = 50, message = "El nombre del rol no puede superar los 50 caracteres.")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion,

        Set<Long> permisosIds
) {
}
