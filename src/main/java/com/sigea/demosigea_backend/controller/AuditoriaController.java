package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auditoria.AuditoriaResponse;
import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.dto.auditoria.PaginaResponse;
import com.sigea.demosigea_backend.dto.auditoria.TipoOperacionResponse;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.exception.RegistroAuditoriaInmutableException;
import com.sigea.demosigea_backend.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST del log de auditoría de operaciones críticas (HU-03, RF56).
 * <p>
 * Solo expone consultas (Criterio 2). Cualquier intento de modificar o eliminar registros
 * responde 405 METHOD NOT ALLOWED (Criterio 3). El registro de las operaciones no se hace
 * desde aquí sino automáticamente desde los servicios de negocio (Criterio 1).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see AuditoriaService
 */
@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Consulta del log de auditoría de operaciones críticas (solo lectura)")
@SecurityRequirement(name = "bearerAuth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @Operation(
            summary = "Consultar el log de auditoría con filtros",
            description = "Devuelve los registros ordenados del más reciente al más antiguo. Todos los filtros son "
                    + "opcionales y se combinan (AND). Cumple el Criterio 2 de la HU-03."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos (rango de fechas, tipo de operación, paginación)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin permiso AUDITORIA_VER",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('AUDITORIA_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PaginaResponse<AuditoriaResponse>> buscar(
            @Parameter(description = "ID del usuario que ejecutó la operación", example = "3")
            @RequestParam(required = false) Long usuarioId,
            @Parameter(description = "Parte del correo del usuario", example = "admin@")
            @RequestParam(required = false) String correo,
            @Parameter(description = "Tipo de operación (ver /tipos-operacion)", example = "ROL_ACTUALIZADO")
            @RequestParam(required = false) String accion,
            @Parameter(description = "Recurso afectado", example = "roles")
            @RequestParam(required = false) String entidad,
            @Parameter(description = "Fecha inicial inclusiva (yyyy-MM-dd)", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Fecha final inclusiva (yyyy-MM-dd)", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Número de página (desde 0)", example = "0")
            @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Registros por página (1 a 100)", example = "20")
            @RequestParam(defaultValue = "20") int tamano
    ) {
        FiltroAuditoria filtro = new FiltroAuditoria(usuarioId, correo, accion, entidad, desde, hasta);
        return ResponseEntity.ok(auditoriaService.buscar(filtro, pagina, tamano));
    }

    @Operation(summary = "Consultar un registro de auditoría por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro encontrado"),
            @ApiResponse(responseCode = "404", description = "Registro no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('AUDITORIA_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<AuditoriaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(auditoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Catálogo de tipos de operación auditables",
            description = "Lista para poblar el selector de filtro por tipo de operación en el frontend.")
    @GetMapping("/tipos-operacion")
    @PreAuthorize("hasAnyAuthority('AUDITORIA_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TipoOperacionResponse>> tiposOperacion() {
        return ResponseEntity.ok(auditoriaService.listarTiposOperacion());
    }

    /**
     * Criterio 3: rechaza explícitamente cualquier intento de modificar o eliminar el log,
     * sin importar el rol del usuario (ni siquiera el administrador puede hacerlo).
     */
    @Operation(summary = "Modificar o eliminar registros (NO PERMITIDO)",
            description = "Siempre responde 405. Los registros de auditoría son inmutables (Criterio 3 de la HU-03).")
    @ApiResponse(responseCode = "405", description = "Operación no permitida",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @RequestMapping(value = {"", "/{id}"}, method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE})
    public ResponseEntity<Void> modificacionNoPermitida() {
        throw new RegistroAuditoriaInmutableException();
    }
}
