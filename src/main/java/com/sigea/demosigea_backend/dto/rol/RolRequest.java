package com.sigea.demosigea_backend.dto.rol;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Objeto de transferencia de datos (DTO) inmutable para la creación y edición de roles en el sistema SIGEA.
 * <p>
 * Contiene los datos requeridos por la historia de usuario HU-02 (Criterios 1 y 2) para definir el nombre,
 * la descripción del rol y la colección de identificadores de permisos que le serán asignados.
 * </p>
 *
 * @param nombre Nombre único del perfil o rol (ej. "COORDINADOR_ACADEMICO", "EVALUADOR"). Máximo 50 caracteres.
 * @param descripcion Explicación sobre las responsabilidades o facultades del rol. Máximo 255 caracteres.
 * @param permisosIds Conjunto de IDs correspondientes a los permisos atómicos a asociar con el rol.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para la creación o actualización de un rol y sus permisos")
public record RolRequest(
        @Schema(description = "Nombre único del rol", example = "COORDINADOR_ACADEMICO")
        @NotBlank(message = "El nombre del rol es obligatorio.")
        @Size(max = 50, message = "El nombre del rol no puede superar los 50 caracteres.")
        String nombre,

        @Schema(description = "Descripción de facultades asignadas", example = "Encargado de la supervisión de salas y conferencistas")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion,

        @Schema(description = "Conjunto de IDs de permisos que tendrá el rol", example = "[1, 2, 5]")
        Set<Long> permisosIds
) {
}
