package com.sigea.demosigea_backend.dto.usuario;

import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Usuario del sistema con los roles que tiene asignados (HU-02: asignación de roles a usuarios).
 *
 * @param usuarioId       ID del usuario.
 * @param nombreCompleto  Nombres y apellidos de la persona.
 * @param correo          Correo electrónico de la cuenta.
 * @param numeroDocumento Número de documento de la persona.
 * @param estado          Estado de la cuenta (activo, bloqueado, inactivo).
 * @param roles           Roles asignados al usuario.
 * @param permisos        Códigos de permiso que el usuario obtiene por sus roles (sin repetir, ordenados).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Usuario con los roles asignados")
public record UsuarioRolesResponse(
        @Schema(example = "7") Long usuarioId,
        @Schema(example = "Ana Pérez") String nombreCompleto,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "activo") String estado,
        List<RolAsignado> roles,
        @Schema(example = "[\"PROPUESTAS_EVALUAR\"]") List<String> permisos
) {

    /**
     * Rol asignado a un usuario.
     *
     * @param id     ID del rol.
     * @param nombre Nombre del rol.
     */
    public record RolAsignado(@Schema(example = "3") Long id, @Schema(example = "EVALUADOR") String nombre) {
    }

    /**
     * Construye la respuesta a partir de la entidad.
     *
     * @param u usuario con persona y roles cargados.
     * @return DTO del usuario con sus roles.
     */
    public static UsuarioRolesResponse fromEntity(Usuario u) {
        Persona p = u.getPersona();
        List<Rol> roles = u.getRoles() == null ? List.of() : u.getRoles().stream()
                .sorted(Comparator.comparing(Rol::getNombre))
                .toList();
        List<String> permisos = roles.stream()
                .filter(r -> r.getPermisos() != null)
                .flatMap(r -> r.getPermisos().stream())
                .map(Permiso::getCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        String nombre = p == null ? null
                : ((p.getNombres() == null ? "" : p.getNombres()) + " "
                + (p.getApellidos() == null ? "" : p.getApellidos())).trim();
        return new UsuarioRolesResponse(
                u.getId(),
                nombre,
                p == null ? null : p.getCorreo(),
                p == null ? null : p.getNumeroDocumento(),
                u.getEstado() == null ? null : u.getEstado().name(),
                roles.stream().map(r -> new RolAsignado(r.getId(), r.getNombre())).toList(),
                permisos
        );
    }
}
