package com.sigea.demosigea_backend.dto.rol;

import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.model.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) inmutable para la respuesta con los datos de un rol.
 * <p>
 * Incluye información del rol, el listado de permisos asociados y los indicadores de impacto
 * (conteo de usuarios activos y conteo total de usuarios asignados) para soportar las validaciones
 * visuales y de negocio requeridas por la administración de roles (HU-02).
 * </p>
 *
 * @param id Identificador único numérico del rol.
 * @param nombre Nombre único del rol.
 * @param descripcion Descripción funcional del rol.
 * @param usuariosActivosAsignados Cantidad de usuarios con cuenta activa que tienen este rol asignado.
 * @param totalUsuariosAsignados Cantidad global de usuarios (activos, inactivos o bloqueados) asignados.
 * @param permisos Colección ordenada de permisos que componen el perfil.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta detallada con información de un rol, sus permisos y usuarios asignados")
public record RolResponse(
        @Schema(description = "Identificador único del rol", example = "1")
        Long id,

        @Schema(description = "Nombre del rol", example = "EVALUADOR")
        String nombre,

        @Schema(description = "Descripción del rol", example = "Par evaluador de propuestas de investigación")
        String descripcion,

        @Schema(description = "Número de usuarios activos con este rol asignado", example = "3")
        long usuariosActivosAsignados,

        @Schema(description = "Número total de usuarios asignados a este rol", example = "5")
        long totalUsuariosAsignados,

        @Schema(description = "Lista de permisos funcionales asociados al rol")
        List<PermisoResponse> permisos,

        @Schema(description = "Timestamp en milisegundos de la última modificación del rol o sus permisos", example = "1711562400000")
        long ultimaModificacion
) {
    /**
     * Construye un {@link RolResponse} a partir de la entidad JPA {@link Rol} y las métricas de asignación de usuarios.
     *
     * @param rol Entidad JPA del rol.
     * @param usuariosActivos Número de usuarios activos vinculados.
     * @param totalUsuarios Número total de usuarios asignados.
     * @return DTO inmutable formateado con los permisos ordenados por módulo y código.
     */
    public static RolResponse fromEntity(Rol rol, long usuariosActivos, long totalUsuarios) {
        List<PermisoResponse> permisosList = (rol.getPermisos() != null)
                ? rol.getPermisos().stream()
                        .map(PermisoResponse::fromEntity)
                        .sorted(Comparator.comparing(PermisoResponse::modulo)
                                .thenComparing(PermisoResponse::codigo))
                        .toList()
                : List.of();

        long timestampModificacion = (rol.getFechaActualizacion() != null)
                ? rol.getFechaActualizacion().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        return new RolResponse(
                rol.getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                usuariosActivos,
                totalUsuarios,
                permisosList,
                timestampModificacion
        );
    }
}
