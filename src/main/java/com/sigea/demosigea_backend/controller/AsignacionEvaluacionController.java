package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asignacion.AsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.AsignarEvaluadoresRequest;
import com.sigea.demosigea_backend.dto.asignacion.EvaluadorDisponibleResponse;
import com.sigea.demosigea_backend.dto.asignacion.HistorialAsignacionResponse;
import com.sigea.demosigea_backend.dto.asignacion.ReasignarEvaluadorRequest;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.AsignacionEvaluacionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para asignar propuestas a los evaluadores del comité (HU-16, RF24).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Asignación de evaluadores",
        description = "Asignación y reasignación de propuestas a evaluadores del comité, con control de conflicto "
                + "de interés e historial (HU-16)")
@RestController
@RequestMapping("/api/v1/asignaciones")
@RequiredArgsConstructor
public class AsignacionEvaluacionController {

    private final AsignacionEvaluacionService asignacionService;

    @Operation(summary = "Asignar propuesta a evaluadores",
            description = "Asigna la propuesta a uno o más evaluadores del comité de su convocatoria y notifica a "
                    + "cada uno (Criterio 1). Si un evaluador es autor de la propuesta se impide la asignación; si "
                    + "comparte afiliación con un autor se exige confirmarConflicto=true (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asignaciones creadas."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o evaluador de otra convocatoria (VALIDACION_FALLIDA / SOLICITUD_INVALIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta o evaluador no encontrado (RECURSO_NO_ENCONTRADO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflicto de interés (CONFLICTO_INTERES), advertencia sin confirmar "
                    + "(CONFIRMACION_REQUERIDA), asignación repetida (ASIGNACION_DUPLICADA) o propuesta no asignable (OPERACION_NO_PERMITIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('PROPUESTAS_ASIGNAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @PostMapping("/propuestas/{propuestaId}")
    public ResponseEntity<List<AsignacionResponse>> asignar(@PathVariable Long propuestaId,
                                                            @Valid @RequestBody AsignarEvaluadoresRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionService.asignar(propuestaId, request));
    }

    @Operation(summary = "Reasignar a otro evaluador",
            description = "Cambia el evaluador de una asignación que aún no tiene calificaciones. La asignación "
                    + "vuelve a pendiente y el movimiento queda en el historial (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asignación actualizada."),
            @ApiResponse(responseCode = "400", description = "Mismo evaluador o evaluador de otra convocatoria (SOLICITUD_INVALIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Asignación o evaluador no encontrado (RECURSO_NO_ENCONTRADO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Evaluación ya iniciada, conflicto de interés o asignación repetida.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('PROPUESTAS_ASIGNAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @PutMapping("/{asignacionId}/reasignar")
    public ResponseEntity<AsignacionResponse> reasignar(@PathVariable Long asignacionId,
                                                        @Valid @RequestBody ReasignarEvaluadorRequest request) {
        return ResponseEntity.ok(asignacionService.reasignar(asignacionId, request));
    }

    @Operation(summary = "Asignaciones de una propuesta",
            description = "Evaluadores que tienen asignada actualmente la propuesta.")
    @PreAuthorize("hasAnyAuthority('PROPUESTAS_ASIGNAR', 'CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_VER', 'ROLE_ADMIN', 'ADMIN')")
    @GetMapping("/propuestas/{propuestaId}")
    public ResponseEntity<List<AsignacionResponse>> listarPorPropuesta(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(asignacionService.listarPorPropuesta(propuestaId));
    }

    @Operation(summary = "Historial de asignaciones de una propuesta",
            description = "Asignaciones y reasignaciones de la propuesta, de la más reciente a la más antigua (Criterio 3).")
    @PreAuthorize("hasAnyAuthority('PROPUESTAS_ASIGNAR', 'CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_VER', 'ROLE_ADMIN', 'ADMIN')")
    @GetMapping("/propuestas/{propuestaId}/historial")
    public ResponseEntity<List<HistorialAsignacionResponse>> historial(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(asignacionService.historialPorPropuesta(propuestaId));
    }

    @Operation(summary = "Evaluadores disponibles para una propuesta",
            description = "Evaluadores del comité de la convocatoria con su carga de trabajo, si ya tienen la "
                    + "propuesta y el conflicto de interés que tendrían (NINGUNO, ADVERTENCIA, BLOQUEANTE).")
    @PreAuthorize("hasAnyAuthority('PROPUESTAS_ASIGNAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @GetMapping("/propuestas/{propuestaId}/evaluadores-disponibles")
    public ResponseEntity<List<EvaluadorDisponibleResponse>> evaluadoresDisponibles(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(asignacionService.evaluadoresDisponibles(propuestaId));
    }

    @Operation(summary = "Mi bandeja de evaluaciones",
            description = "Propuestas asignadas al evaluador autenticado (Criterio 1).")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mis-asignaciones")
    public ResponseEntity<List<AsignacionResponse>> miBandeja() {
        return ResponseEntity.ok(asignacionService.miBandeja());
    }
}
