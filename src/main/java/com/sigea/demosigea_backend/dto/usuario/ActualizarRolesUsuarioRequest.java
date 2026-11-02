package com.sigea.demosigea_backend.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Roles que tendrá un usuario después de la operación; reemplaza los que tenía (HU-02).
 *
 * @param rolesIds IDs de los roles a asignar. Debe incluir al menos uno.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Roles que tendrá el usuario (reemplaza los actuales)")
public record ActualizarRolesUsuarioRequest(
        @Schema(description = "IDs de los roles a asignar", example = "[1, 3]")
        @NotNull(message = "La lista de roles es obligatoria.")
        @Size(min = 1, message = "El usuario debe tener al menos un rol.")
        Set<@NotNull Long> rolesIds
) {
}
