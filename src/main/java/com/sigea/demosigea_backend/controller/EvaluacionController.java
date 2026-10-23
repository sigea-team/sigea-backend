package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.evaluacion.AsignacionEvaluacionResponse;
import com.sigea.demosigea_backend.dto.evaluacion.RegistrarEvaluacionRequest;
import com.sigea.demosigea_backend.dto.evaluacion.ResultadoEvaluacionResponse;
import com.sigea.demosigea_backend.service.EvaluacionService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión de evaluaciones de propuestas y el cálculo de
 * puntuación ponderada (HU-17).
 * <p>
 * Expone endpoints para registrar calificaciones por criterio, consultar el estado de
 * una asignación y obtener el resultado final de evaluación de una propuesta.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Evaluaciones",
     description = "Registro de evaluaciones de propuestas y cálculo de puntuación ponderada (HU-17)")
@RestController
@RequestMapping("/api/v1/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    /**
     * Registra la evaluación completa de una propuesta por parte de un evaluador asignado.
     * <p>
     * Valida que todos los criterios de la rúbrica estén calificados, guarda las calificaciones
     * y, si todas las asignaciones de la propuesta están completas, calcula automáticamente la
     * puntuación ponderada.
     * </p>
     *
     * @param request solicitud con el ID de asignación y la lista de calificaciones por criterio.
     * @return {@link AsignacionEvaluacionResponse} con el estado de la asignación y las evaluaciones guardadas.
     */
    @Operation(
            summary = "Registrar evaluación de propuesta",
            description = "Registra las calificaciones por criterio de rúbrica para una asignación de evaluación. "
                    + "Si todas las asignaciones de la propuesta quedan completadas, calcula automáticamente "
                    + "la puntuación ponderada.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evaluación registrada exitosamente."),
            @ApiResponse(responseCode = "400", description = "Criterios sin calificar o datos inválidos (EVALUACION_INCOMPLETA / VALIDACION_FALLIDA)."),
            @ApiResponse(responseCode = "404", description = "Asignación o rúbrica no encontrada (RECURSO_NO_ENCONTRADO)."),
            @ApiResponse(responseCode = "409", description = "La evaluación ya fue enviada y el periodo está cerrado (EVALUACION_CERRADA)."),
            @ApiResponse(responseCode = "401", description = "No autenticado.")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<AsignacionEvaluacionResponse> registrarEvaluacion(
            @Valid @RequestBody RegistrarEvaluacionRequest request) {
        AsignacionEvaluacionResponse response = evaluacionService.registrarEvaluacion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene el detalle de una asignación de evaluación con sus calificaciones por criterio.
     *
     * @param asignacionId ID de la asignación de evaluación.
     * @return {@link AsignacionEvaluacionResponse} con el estado y las calificaciones de la asignación.
     */
    @Operation(
            summary = "Consultar asignación de evaluación",
            description = "Retorna el detalle de una asignación de evaluación, incluyendo las calificaciones "
                    + "por criterio registradas hasta el momento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la asignación obtenido exitosamente."),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada (RECURSO_NO_ENCONTRADO)."),
            @ApiResponse(responseCode = "401", description = "No autenticado.")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/asignaciones/{asignacionId}")
    public ResponseEntity<AsignacionEvaluacionResponse> obtenerAsignacion(
            @PathVariable Long asignacionId) {
        return ResponseEntity.ok(evaluacionService.obtenerAsignacion(asignacionId));
    }

    /**
     * Obtiene el resultado de evaluación ponderada de una propuesta.
     * <p>
     * Si aún hay asignaciones pendientes, devuelve la respuesta con {@code completo=false}
     * y sin puntuación (Criterio 3).
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @return {@link ResultadoEvaluacionResponse} con la puntuación ponderada y clasificación, o indicación de incompletitud.
     */
    @Operation(
            summary = "Consultar resultado de evaluación de propuesta",
            description = "Retorna la puntuación ponderada y clasificación final de la propuesta si todas las "
                    + "evaluaciones han sido completadas. Si hay evaluaciones pendientes, indica que el resultado "
                    + "está incompleto (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado obtenido exitosamente (puede estar incompleto)."),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada (RECURSO_NO_ENCONTRADO)."),
            @ApiResponse(responseCode = "401", description = "No autenticado.")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/propuestas/{propuestaId}/resultado")
    public ResponseEntity<ResultadoEvaluacionResponse> obtenerResultado(
            @PathVariable Long propuestaId) {
        return ResponseEntity.ok(evaluacionService.obtenerResultado(propuestaId));
    }
}
