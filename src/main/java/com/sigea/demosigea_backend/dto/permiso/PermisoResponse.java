package com.sigea.demosigea_backend.dto.permiso;

import com.sigea.demosigea_backend.model.Permiso;

/**
 * DTO inmutable que representa un permiso en las respuestas de la API.
 */
public record PermisoResponse(
        Long id,
        String codigo,
        String modulo,
        String descripcion
) {
    public static PermisoResponse fromEntity(Permiso permiso) {
        return new PermisoResponse(
                permiso.getId(),
                permiso.getCodigo(),
                permiso.getModulo(),
                permiso.getDescripcion()
        );
    }
}
