package com.sigea.demosigea_backend.dto.permiso;

import com.sigea.demosigea_backend.model.Permiso;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Objeto de transferencia de datos (DTO) inmutable que encapsula la información de un permiso del sistema.
 * <p>
 * Empleado para serializar los permisos tanto en el catálogo general de permisos como en la lista
 * de permisos concedidos a un rol determinado.
 * </p>
 *
 * @param id Identificador numérico único del permiso en la base de datos.
 * @param codigo Código identificador único en mayúsculas (ej. "ROLES_VER", "EVENTOS_CREAR").
 * @param modulo Nombre del módulo del sistema al que corresponde el permiso.
 * @param descripcion Descripción en lenguaje natural sobre la acción habilitada.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Representación detallada de un permiso atómico por módulo")
public record PermisoResponse(
        @Schema(description = "Identificador único del permiso", example = "1")
        Long id,

        @Schema(description = "Código único del permiso", example = "ROLES_CREAR")
        String codigo,

        @Schema(description = "Módulo funcional asociado", example = "ROLES")
        String modulo,

        @Schema(description = "Descripción de la acción que autoriza", example = "Crear nuevos roles y asignar permisos")
        String descripcion
) {
    /**
     * Mapea una entidad JPA {@link Permiso} hacia su representación DTO {@link PermisoResponse}.
     *
     * @param permiso Entidad JPA a transformar.
     * @return DTO inmutable con los atributos del permiso.
     */
    public static PermisoResponse fromEntity(Permiso permiso) {
        return new PermisoResponse(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getModulo(),
                permiso.getDescripcion()
        );
    }
}
