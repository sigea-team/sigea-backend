package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.presupuesto.HistorialRubroResponse;
import com.sigea.demosigea_backend.dto.presupuesto.PresupuestoPreliminarResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroOperacionResponse;
import com.sigea.demosigea_backend.dto.presupuesto.RubroRequest;
import com.sigea.demosigea_backend.service.PresupuestoPreliminarService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST del presupuesto preliminar de un evento (HU-07, RF06).
 * <p>
 * El presupuesto es un sub-recurso del evento: {@code /api/v1/eventos/{eventoId}/presupuesto}.
 * Usa los permisos {@code PRESUPUESTO_VER} y {@code PRESUPUESTO_GESTIONAR} (changeset hu07-004),
 * que se asignan a los roles desde la administración de roles (HU-02).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see PresupuestoPreliminarService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/presupuesto")
@RequiredArgsConstructor
@Tag(name = "Presupuesto Preliminar", description = "Rubros y total del presupuesto preliminar de un evento (HU-07)")
@SecurityRequirement(name = "bearerAuth")
public class PresupuestoPreliminarController {

    private final PresupuestoPreliminarService presupuestoService;

    @Operation(summary = "Consultar presupuesto preliminar",
            description = "Devuelve los rubros vigentes del evento con su subtotal y el total del presupuesto "
                    + "preliminar (Criterio 1). Con incluirEliminados=true lista también los rubros eliminados, "
                    + "que no suman al total.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presupuesto del evento",
                    content = @Content(schema = @Schema(implementation = PresupuestoPreliminarResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<PresupuestoPreliminarResponse> consultar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "Incluir rubros eliminados", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirEliminados
    ) {
        return ResponseEntity.ok(presupuestoService.consultar(eventoId, incluirEliminados));
    }

    @Operation(summary = "Agregar rubro",
            description = "Registra un rubro con su valor estimado y devuelve el total actualizado (Criterio 1). "
                    + "Nombre vacío, valor vacío o negativo → 400 VALIDACION_FALLIDA con el error por campo "
                    + "(Criterio 3). El valor 0 se acepta. Nombre repetido → 409 RUBRO_DUPLICADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rubro creado",
                    content = @Content(schema = @Schema(implementation = RubroOperacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rubro duplicado o presupuesto no modificable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/rubros")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RubroOperacionResponse> agregar(
            @PathVariable Long eventoId,
            @Valid @RequestBody RubroRequest request
    ) {
        RubroOperacionResponse creado = presupuestoService.agregar(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{rubroId}")
                .buildAndExpand(creado.rubro().id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Editar rubro",
            description = "Actualiza nombre, cantidad o valor de un rubro vigente, recalcula el total y deja el "
                    + "cambio en el historial con los valores anteriores y nuevos (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rubro actualizado",
                    content = @Content(schema = @Schema(implementation = RubroOperacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o rubro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rubro duplicado, eliminado o presupuesto no modificable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/rubros/{rubroId}")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RubroOperacionResponse> actualizar(
            @PathVariable Long eventoId,
            @Parameter(description = "ID del rubro", example = "7") @PathVariable Long rubroId,
            @Valid @RequestBody RubroRequest request
    ) {
        return ResponseEntity.ok(presupuestoService.actualizar(eventoId, rubroId, request));
    }

    @Operation(summary = "Eliminar rubro",
            description = "Retira el rubro del presupuesto vigente sin borrar el registro (activo=false), "
                    + "recalcula el total y deja el cambio en el historial (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rubro eliminado",
                    content = @Content(schema = @Schema(implementation = RubroOperacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Motivo demasiado largo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o rubro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rubro ya eliminado o presupuesto no modificable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/rubros/{rubroId}")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RubroOperacionResponse> eliminar(
            @PathVariable Long eventoId,
            @PathVariable Long rubroId,
            @Parameter(description = "Motivo de la eliminación (opcional, máx. 255)")
            @RequestParam(required = false) String motivo
    ) {
        return ResponseEntity.ok(presupuestoService.eliminar(eventoId, rubroId, motivo));
    }

    @Operation(summary = "Historial del presupuesto preliminar",
            description = "Todas las creaciones, ediciones y eliminaciones de rubros del evento, de la más reciente "
                    + "a la más antigua (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistorialRubroResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/historial")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<HistorialRubroResponse>> historialPresupuesto(@PathVariable Long eventoId) {
        return ResponseEntity.ok(presupuestoService.historialPresupuesto(eventoId));
    }

    @Operation(summary = "Historial de un rubro",
            description = "Modificaciones de un rubro (vigente o eliminado), de la más reciente a la más antigua.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del rubro",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistorialRubroResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento o rubro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/rubros/{rubroId}/historial")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<HistorialRubroResponse>> historialRubro(
            @PathVariable Long eventoId,
            @PathVariable Long rubroId
    ) {
        return ResponseEntity.ok(presupuestoService.historialRubro(eventoId, rubroId));
    }
}
