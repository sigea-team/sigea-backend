package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.presupuesto.EjecucionPresupuestalResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoRequest;
import com.sigea.demosigea_backend.dto.presupuesto.GastoEjecutadoResponse;
import com.sigea.demosigea_backend.dto.presupuesto.GastoRegistradoResponse;
import com.sigea.demosigea_backend.service.EjecucionPresupuestalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST de los gastos ejecutados y la ejecución presupuestal de un evento
 * (HU-09, RF11 + RF12).
 * <p>
 * Ruta base: {@code /api/v1/eventos/{eventoId}/ejecucion-presupuestal}. Usa los permisos del módulo
 * PRESUPUESTO (changeset hu07-004): {@code PRESUPUESTO_VER} para consultar y
 * {@code PRESUPUESTO_GESTIONAR} para registrar gastos.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see EjecucionPresupuestalService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/ejecucion-presupuestal")
@RequiredArgsConstructor
@Tag(name = "Ejecución Presupuestal", description = "Gastos ejecutados por rubro y porcentaje de ejecución del evento (HU-09)")
@SecurityRequirement(name = "bearerAuth")
public class EjecucionPresupuestalController {

    private final EjecucionPresupuestalService ejecucionService;

    @Operation(summary = "Consultar ejecución presupuestal",
            description = "Devuelve el presupuesto aprobado, el total ejecutado, el saldo y el porcentaje de ejecución "
                    + "del evento, con el acumulado, disponible y porcentaje de cada rubro vigente (Criterio 1). "
                    + "Sin gastos devuelve 0 % sin error (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ejecución presupuestal del evento",
                    content = @Content(schema = @Schema(implementation = EjecucionPresupuestalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<EjecucionPresupuestalResponse> consultar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(ejecucionService.consultarEjecucion(eventoId));
    }

    @Operation(summary = "Registrar gasto ejecutado",
            description = "Registra un gasto contra un rubro vigente y devuelve el acumulado del rubro y el porcentaje de "
                    + "ejecución recalculados (Criterio 1). Requiere presupuesto aprobado. Un gasto que supera el "
                    + "disponible del rubro se registra igual y la respuesta lo marca con excedioDisponible=true (Criterio 2). "
                    + "Sin valor o con valor negativo → 400 VALIDACION_FALLIDA (Criterio 3). El valor 0 y la falta de soporte se aceptan.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Gasto registrado",
                    content = @Content(schema = @Schema(implementation = GastoRegistradoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falta el valor, la fecha o el rubro",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o rubro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Sin presupuesto aprobado, rubro eliminado o evento cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/gastos")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<GastoRegistradoResponse> registrarGasto(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Valid @RequestBody GastoEjecutadoRequest request
    ) {
        GastoRegistradoResponse registrado = ejecucionService.registrarGasto(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{gastoId}")
                .buildAndExpand(registrado.gasto().id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(registrado);
    }

    @Operation(summary = "Listar gastos ejecutados",
            description = "Gastos del evento (o de un rubro con rubroId), del más reciente al más antiguo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gastos registrados",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GastoEjecutadoResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento o rubro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/gastos")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<GastoEjecutadoResponse>> listarGastos(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "Filtrar por rubro", example = "7") @RequestParam(required = false) Long rubroId
    ) {
        return ResponseEntity.ok(ejecucionService.listarGastos(eventoId, rubroId));
    }

    @Operation(summary = "Consultar gasto ejecutado", description = "Detalle de un gasto del evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gasto",
                    content = @Content(schema = @Schema(implementation = GastoEjecutadoResponse.class))),
            @ApiResponse(responseCode = "404", description = "Gasto no encontrado en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/gastos/{gastoId}")
    @PreAuthorize("hasAnyAuthority('PRESUPUESTO_VER', 'PRESUPUESTO_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<GastoEjecutadoResponse> obtenerGasto(
            @PathVariable Long eventoId,
            @Parameter(description = "ID del gasto", example = "15") @PathVariable Long gastoId
    ) {
        return ResponseEntity.ok(ejecucionService.obtenerGasto(eventoId, gastoId));
    }
}
