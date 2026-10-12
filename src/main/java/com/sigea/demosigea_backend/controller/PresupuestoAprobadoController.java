package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoAprobadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoResumenResponse;
import com.sigea.demosigea_backend.service.PresupuestoAprobadoService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para el registro y consulta del presupuesto formalmente aprobado de un evento (HU-08, RF07).
 * <p>
 * Ruta base: {@code /api/v1/eventos/{eventoId}/presupuesto-aprobado}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see PresupuestoAprobadoService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/presupuesto-aprobado")
@RequiredArgsConstructor
@Tag(name = "Presupuesto Aprobado", description = "Gestión del presupuesto formalmente aprobado para un evento (HU-08)")
@SecurityRequirement(name = "bearerAuth")
public class PresupuestoAprobadoController {

    private final PresupuestoAprobadoService presupuestoService;

    @Operation(
            summary = "Registrar o actualizar presupuesto aprobado",
            description = "Registra el presupuesto formalmente aprobado vinculado al evento con su evidencia documental (Criterio 1). "
                    + "Si ya existía un presupuesto aprobado, conserva la versión previa como histórica y guarda la nueva (Criterio 2). "
                    + "Exige obligatoriamente la evidencia documental antes de guardar (Criterio 3)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Presupuesto aprobado guardado exitosamente como versión oficial",
                    content = @Content(schema = @Schema(implementation = PresupuestoAprobadoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o falta evidencia documental obligatoria",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o usuario no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento no tiene presupuesto preliminar registrado o está cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PresupuestoAprobadoResponse> registrarOActualizar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Valid @RequestBody PresupuestoAprobadoRequest request
    ) {
        PresupuestoAprobadoResponse response = presupuestoService.registrarOActualizar(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri();
        return ResponseEntity.created(ubicacion).body(response);
    }

    @Operation(
            summary = "Consultar versión oficial del presupuesto aprobado",
            description = "Retorna la versión oficial vigente autorizada para el evento, junto con la comparación frente al presupuesto preliminar."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presupuesto oficial vigente",
                    content = @Content(schema = @Schema(implementation = PresupuestoAprobadoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o presupuesto oficial no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PresupuestoAprobadoResponse> obtenerOficial(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(presupuestoService.obtenerOficial(eventoId));
    }

    @Operation(
            summary = "Consultar historial de versiones del presupuesto aprobado",
            description = "Retorna el historial completo de versiones de presupuestos aprobados del evento (versión vigente y versiones históricas conservadas tras actualizaciones) (Criterio 2)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial cronológico de versiones",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = PresupuestoAprobadoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/historial")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<PresupuestoAprobadoResponse>> listarHistorial(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(presupuestoService.listarHistorial(eventoId));
    }

    @Operation(
            summary = "Obtener resumen presupuestal comparativo",
            description = "Retorna el resumen consolidado del evento: presupuesto preliminar proyectado, cantidad de rubros, presupuesto oficial vigente e historial de cambios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resumen presupuestal comparativo",
                    content = @Content(schema = @Schema(implementation = PresupuestoResumenResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/resumen")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PresupuestoResumenResponse> obtenerResumen(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(presupuestoService.obtenerResumen(eventoId));
    }
}
