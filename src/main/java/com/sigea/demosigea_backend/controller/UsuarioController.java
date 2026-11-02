package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.usuario.ActualizarRolesUsuarioRequest;
import com.sigea.demosigea_backend.dto.usuario.UsuarioRolesResponse;
import com.sigea.demosigea_backend.service.UsuarioRolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de usuarios y asignación de sus roles por parte del administrador (HU-02).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Consulta de usuarios y asignación de roles")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioRolService usuarioRolService;

    @Operation(summary = "Listar usuarios con sus roles",
            description = "Lista los usuarios del sistema con sus roles y permisos. Filtra por nombre, correo o documento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuarios obtenidos."),
            @ApiResponse(responseCode = "403", description = "Sin permiso para consultar usuarios.")
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USUARIOS_VER', 'USUARIOS_GESTIONAR_ROLES', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<UsuarioRolesResponse>> listar(
            @Parameter(description = "Texto a buscar en nombre, correo o documento")
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(usuarioRolService.listarUsuarios(busqueda));
    }

    @Operation(summary = "Asignar roles a un usuario",
            description = "Reemplaza los roles del usuario. El cambio aplica de inmediato en el backend; "
                    + "el usuario debe volver a iniciar sesión para ver las nuevas opciones en la interfaz.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles actualizados."),
            @ApiResponse(responseCode = "400", description = "Lista de roles vacía o inválida."),
            @ApiResponse(responseCode = "404", description = "Usuario o rol inexistente."),
            @ApiResponse(responseCode = "409", description = "El administrador intentó quitarse a sí mismo la gestión de roles."),
            @ApiResponse(responseCode = "403", description = "Sin permiso para gestionar roles de usuarios.")
    })
    @PutMapping("/{usuarioId}/roles")
    @PreAuthorize("hasAnyAuthority('USUARIOS_GESTIONAR_ROLES', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UsuarioRolesResponse> actualizarRoles(
            @PathVariable Long usuarioId,
            @Valid @RequestBody ActualizarRolesUsuarioRequest request) {
        return ResponseEntity.ok(usuarioRolService.actualizarRoles(usuarioId, request.rolesIds()));
    }
}
