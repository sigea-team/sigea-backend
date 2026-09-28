package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.service.RolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la administración integral de roles y asignación de permisos (HU-02).
 * <p>
 * Proporciona operaciones para:
 * <ul>
 *   <li>Consultar el listado de roles y el detalle individual de cada uno con sus permisos y métricas de usuarios.</li>
 *   <li>Crear nuevos roles con asignación de permisos sobre los diferentes módulos (Criterio 1).</li>
 *   <li>Modificar roles y sincronizar permisos con efecto inmediato sobre los usuarios (Criterio 2).</li>
 *   <li>Eliminar roles impidiendo la operación si el rol tiene usuarios activos asignados (Criterio 3).</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see RolService
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Administración de Roles y Permisos", description = "Endpoints de gestión (CRUD) de roles del sistema y asignación de permisos a módulos")
@SecurityRequirement(name = "bearerAuth")
public class RolController {

    /** Servicio de lógica de negocio para roles. */
    private final RolService rolService;

    /**
     * Obtiene el listado completo de roles registrados en el sistema.
     *
     * @return {@link ResponseEntity} con la lista de {@link RolResponse}.
     */
    @Operation(
            summary = "Listar roles",
            description = "Retorna todos los roles configurados en la plataforma con sus permisos asociados y métricas de usuarios asignados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles listados exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RolResponse.class)))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    /**
     * Obtiene el detalle de un rol específico por su identificador único.
     *
     * @param id Identificador numérico del rol.
     * @return {@link ResponseEntity} con la información detallada del rol.
     */
    @Operation(
            summary = "Consultar rol por ID",
            description = "Obtiene los detalles, permisos asignados y número de usuarios de un rol específico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol encontrado",
                    content = @Content(schema = @Schema(implementation = RolResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> obtenerPorId(
            @Parameter(description = "Identificador del rol", example = "1")
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(rolService.obtenerPorId(id));
    }

    /**
     * Crea un nuevo rol en el sistema y le asigna un conjunto de permisos sobre los módulos (Criterio 1).
     *
     * @param request Datos del rol a crear (nombre, descripción, colección de IDs de permisos).
     * @return {@link ResponseEntity} con el rol persistido y código HTTP 201 Created.
     */
    @Operation(
            summary = "Crear nuevo rol",
            description = "Crea un nuevo rol y le asigna un conjunto de permisos funcionales sobre los módulos. Cumple con el Criterio 1 de la HU-02."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rol creado exitosamente",
                    content = @Content(schema = @Schema(implementation = RolResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El nombre del rol ya se encuentra registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLES_CREAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> crearRol(@Valid @RequestBody RolRequest request) {
        RolResponse nuevoRol = rolService.crearRol(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRol);
    }

    /**
     * Modifica los datos de un rol existente y sincroniza sus permisos asignados (Criterio 2).
     *
     * @param id Identificador numérico del rol a actualizar.
     * @param request Nuevos datos y permisos a configurar para el rol.
     * @return {@link ResponseEntity} con el rol actualizado y código HTTP 200 OK.
     */
    @Operation(
            summary = "Modificar rol existente",
            description = "Actualiza los datos y permisos de un rol. El cambio tiene efecto inmediato en todos los usuarios asignados (Criterio 2 de la HU-02)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = RolResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rol o permiso no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre de rol duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> actualizarRol(
            @Parameter(description = "Identificador del rol a modificar", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody RolRequest request
    ) {
        RolResponse rolActualizado = rolService.actualizarRol(id, request);
        return ResponseEntity.ok(rolActualizado);
    }

    /**
     * Elimina un rol del sistema previa comprobación de cuentas activas (Criterio 3).
     *
     * @param id Identificador numérico del rol a eliminar.
     * @return {@link ResponseEntity} con mensaje confirmatorio o error 409 si tiene usuarios activos asignados.
     */
    @Operation(
            summary = "Eliminar rol",
            description = "Elimina un rol del sistema si no posee usuarios activos asignados. Si tiene usuarios activos, rechaza la solicitud e indica reasignarlos primero (Criterio 3 de la HU-02)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rol eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Rol no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Imposible eliminar: el rol posee usuarios activos asignados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_ELIMINAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarRol(
            @Parameter(description = "Identificador del rol a eliminar", example = "1")
            @PathVariable Long id
    ) {
        rolService.eliminarRol(id);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Rol eliminado exitosamente.",
                "rolId", String.valueOf(id)
        ));
    }
}
