package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.service.PermisoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la consulta y catalogación de permisos del sistema SIGEA.
 * <p>
 * Provee acceso seguro al catálogo de permisos funcionales organizados por módulos,
 * facilitando la interfaz de creación y modificación de roles (HU-02).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see PermisoService
 */
@RestController
@RequestMapping("/api/v1/permisos")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Permisos", description = "Endpoints para la consulta y catalogación de permisos atómicos del sistema por módulos")
@SecurityRequirement(name = "bearerAuth")
public class PermisoController {

    /** Servicio de consulta de permisos. */
    private final PermisoService permisoService;

    /**
     * Consulta el catálogo de permisos funcionales del sistema.
     * <p>
     * Puede retornarse de forma lineal (lista plana ordenada) o agrupada por el nombre del módulo
     * enviando el parámetro de consulta {@code agrupado=true}.
     * </p>
     *
     * @param agrupado Si es {@code true}, retorna un diccionario de permisos indexado por módulo; de lo contrario, una lista plana.
     * @return {@link ResponseEntity} conteniendo la colección de permisos o el mapa por módulos.
     */
    @Operation(
            summary = "Consultar catálogo de permisos",
            description = "Retorna la lista de permisos funcionales del sistema para asignación sobre roles. Permite agrupación por módulo funcional."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo de permisos obtenido con éxito",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PermisoResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token JWT faltante o no válido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No posee los privilegios suficientes para consultar permisos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLES_CREAR', 'ROLES_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> listarPermisos(
            @Parameter(description = "Indica si se deben agrupar los permisos por módulo funcional", example = "false")
            @RequestParam(name = "agrupado", defaultValue = "false") boolean agrupado
    ) {
        if (agrupado) {
            Map<String, List<PermisoResponse>> agrupados = permisoService.listarAgrupadosPorModulo();
            return ResponseEntity.ok(agrupados);
        }
        List<PermisoResponse> permisos = permisoService.listarTodos();
        return ResponseEntity.ok(permisos);
    }
}
