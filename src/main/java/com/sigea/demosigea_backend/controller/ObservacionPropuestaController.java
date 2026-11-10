package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.observacion.AprobacionRechazadaResponse;
import com.sigea.demosigea_backend.dto.observacion.ObservacionResponse;
import com.sigea.demosigea_backend.dto.observacion.ObservacionesPropuestaResponse;
import com.sigea.demosigea_backend.dto.observacion.PropuestaProgramableResponse;
import com.sigea.demosigea_backend.dto.observacion.RegistrarObservacionesRequest;
import com.sigea.demosigea_backend.dto.observacion.VersionDefinitivaResponse;
import com.sigea.demosigea_backend.service.ObservacionPropuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Observaciones y ajustes requeridos a las propuestas y aprobación de su versión definitiva (HU-19: RF28, RF29).
 * <p>
 * Flujo: el evaluador o el administrador registra observaciones ({@code POST /propuestas/{id}/observaciones}) →
 * el autor sube una nueva versión con el endpoint de HU-14 ({@code POST /propuestas/{id}/versiones}), que queda
 * vinculada a los ajustes pendientes → se dan por resueltos ({@code PATCH .../observaciones/{obsId}/resolver}) → el
 * administrador aprueba la versión definitiva ({@code POST /propuestas/{id}/version-definitiva}) → la propuesta
 * aparece en {@code GET /eventos/{eventoId}/propuestas-programables} para la agenda.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Propuestas - Observaciones y versión definitiva",
        description = "Observaciones y ajustes requeridos, y aprobación de la versión definitiva (HU-19)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ObservacionPropuestaController {

    /** Registrar y resolver: evaluadores (el servicio exige que estén asignados a la propuesta) y administración. */
    private static final String OBSERVAR = "hasAnyAuthority('PROPUESTAS_EVALUAR', 'PROPUESTAS_APROBAR_DEFINITIVA', "
            + "'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    /** Aprobar la versión definitiva: solo administración. */
    private static final String APROBAR = "hasAnyAuthority('PROPUESTAS_APROBAR_DEFINITIVA', 'CONVOCATORIAS_GESTIONAR', "
            + "'ROLE_ADMIN', 'ADMIN')";

    /** Propuestas para la agenda: quien programa la agenda o administra propuestas. */
    private static final String AGENDA = "hasAnyAuthority('AGENDA_GESTIONAR', 'EVENTOS_VER', "
            + "'PROPUESTAS_APROBAR_DEFINITIVA', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    private final ObservacionPropuestaService observacionService;

    @Operation(summary = "Registrar observaciones y ajustes requeridos",
            description = "Guarda una o varias observaciones sobre la versión actual de una propuesta evaluada y "
                    + "clasificada, y notifica a sus autores (Criterio 1). Por defecto cada observación es un ajuste "
                    + "requerido; con requiereAjuste=false queda como comentario y no bloquea la aprobación.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Observaciones registradas"),
            @ApiResponse(responseCode = "400", description = "Sin observaciones o con descripción vacía",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "No es evaluador asignado a la propuesta ni administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Propuesta sin evaluar y clasificar (PROPUESTA_NO_EVALUADA), "
                    + "rechazada (OPERACION_NO_PERMITIDA) o con versión definitiva (PROPUESTA_BLOQUEADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/propuestas/{id}/observaciones")
    @PreAuthorize(OBSERVAR)
    public ResponseEntity<ObservacionesPropuestaResponse> registrar(@PathVariable Long id,
                                                                    @Valid @RequestBody RegistrarObservacionesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(observacionService.registrar(id, request));
    }

    @Operation(summary = "Consultar las observaciones de una propuesta",
            description = "Devuelve las observaciones de todas las versiones (con la versión que las originó y la que "
                    + "las atendió), los ajustes pendientes y si la versión actual se puede aprobar como definitiva. "
                    + "Disponible para el administrador, los evaluadores asignados y los autores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Observaciones de la propuesta"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/propuestas/{id}/observaciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ObservacionesPropuestaResponse> listar(@PathVariable Long id) {
        return ResponseEntity.ok(observacionService.listar(id));
    }

    @Operation(summary = "Dar por resuelta una observación",
            description = "Marca como resuelta una observación. Un ajuste requerido solo se puede resolver cuando el "
                    + "autor subió una versión que lo atiende (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Observación resuelta"),
            @ApiResponse(responseCode = "403", description = "No es evaluador asignado a la propuesta ni administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta u observación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya resuelta, sin versión que la atienda "
                    + "(OPERACION_NO_PERMITIDA) o propuesta con versión definitiva (PROPUESTA_BLOQUEADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/propuestas/{id}/observaciones/{observacionId}/resolver")
    @PreAuthorize(OBSERVAR)
    public ResponseEntity<ObservacionResponse> resolver(@PathVariable Long id, @PathVariable Long observacionId) {
        return ResponseEntity.ok(observacionService.resolver(id, observacionId));
    }

    @Operation(summary = "Aprobar la versión definitiva",
            description = "Aprueba la versión actual de la propuesta como definitiva: la marca, bloquea su edición y la "
                    + "deja disponible para programar en la agenda (Criterio 3). Si quedan ajustes sin resolver, "
                    + "responde 409 con la lista de pendientes (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versión definitiva aprobada"),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ajustes sin resolver (OBSERVACIONES_PENDIENTES) o sin "
                    + "registrar (AJUSTES_SIN_REGISTRAR), con la lista de pendientes",
                    content = @Content(schema = @Schema(implementation = AprobacionRechazadaResponse.class)))
    })
    @PostMapping("/propuestas/{id}/version-definitiva")
    @PreAuthorize(APROBAR)
    public ResponseEntity<VersionDefinitivaResponse> aprobarVersionDefinitiva(@PathVariable Long id) {
        return ResponseEntity.ok(observacionService.aprobarVersionDefinitiva(id));
    }

    @Operation(summary = "Propuestas disponibles para programar en la agenda",
            description = "Propuestas de las convocatorias del evento con versión definitiva aprobada (Criterio 3). "
                    + "Solo estas se pueden vincular a una actividad (HU-21/HU-22). Con soloSinProgramar=true se omiten "
                    + "las que ya tienen una actividad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Propuestas programables"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/propuestas-programables")
    @PreAuthorize(AGENDA)
    public ResponseEntity<List<PropuestaProgramableResponse>> listarProgramables(
            @PathVariable Long eventoId,
            @RequestParam(defaultValue = "false") boolean soloSinProgramar) {
        return ResponseEntity.ok(observacionService.listarProgramables(eventoId, soloSinProgramar));
    }
}
