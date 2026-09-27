package com.sigea.demosigea_backend.dto.rol;

import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.model.Rol;

import java.util.List;

/**
 * DTO para la respuesta con datos resumidos o detallados del rol.
 */
public record RolResponse(
        Long id,
        String nombre,
        String descripcion,
        long usuariosActivosAsignados,
        long totalUsuariosAsignados,
        List<PermisoResponse> permisos
) {
    public static RolResponse fromEntity(Rol rol, long usuariosActivos, long totalUsuarios) {
        List<PermisoResponse> permisosList = (rol.getPermisos() != null)
                ? rol.getPermisos().stream()
                        .map(PermisoResponse::fromEntity)
                        .sorted(java.util.Comparator.comparing(PermisoResponse::modulo)
                                .thenComparing(PermisoResponse::codigo))
                        .toList()
                : List.of();

        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                usuariosActivos,
                totalUsuarios,
                permisosList
        );
    }
}
