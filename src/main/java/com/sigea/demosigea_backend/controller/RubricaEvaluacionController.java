package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.rubrica.ImpactoRubricaResponse;
import com.sigea.demosigea_backend.dto.rubrica.RubricaRequest;
import com.sigea.demosigea_backend.dto.rubrica.RubricaResponse;
import com.sigea.demosigea_backend.service.RubricaEvaluacionService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de la rúbrica de evaluación de una convocatoria (HU-15, RF22).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Rúbrica de evaluación",
        description = "Configuración de la rúbrica (criterios, escalas y pesos) con la que se evalúan las propuestas "
                + "de una convocatoria (HU-15)")
@RestController
@RequestMapping("/api/v1/convocatorias/{convocatoriaId}/rubrica")
@RequiredArgsConstructor
public class RubricaEvaluacionController {

    private final RubricaEvaluacionService rubricaService;

    @Operation(summary = "Rúbrica activa de la convocatoria",
            description = "Criterios con su peso y escala. La usan el organizador y los evaluadores (HU-17).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rúbrica activa."),
            @ApiResponse(responseCode = "404", description = "Convocatoria inexistente o sin rúbrica (RECURSO_NO_ENCONTRADO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_EVALUAR', "
            + "'PROPUESTAS_VER', 'ROLE_ADMIN', 'ADMIN')")
    @GetMapping
    public ResponseEntity<RubricaResponse> obtener(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(rubricaService.obtenerActiva(convocatoriaId));
    }

    @Operation(summary = "Impacto de modificar la rúbrica",
            description = "Calificaciones, asignaciones y propuestas que dependen de la rúbrica activa, para advertir "
                    + "antes de modificarla (Criterio 4).")
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @GetMapping("/impacto")
    public ResponseEntity<ImpactoRubricaResponse> impacto(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(rubricaService.impacto(convocatoriaId));
    }

    @Operation(summary = "Crear o reemplazar la rúbrica",
            description = "Guarda la rúbrica con sus criterios, escalas y pesos; los pesos deben sumar 100% "
                    + "(Criterios 1 y 2). Si la rúbrica activa ya tiene calificaciones se advierte el impacto y se "
                    + "exige confirmar=true; al confirmar, la anterior queda inactiva y las evaluaciones afectadas "
                    + "vuelven a pendiente (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rúbrica creada (o nueva versión tras un reemplazo confirmado)."),
            @ApiResponse(responseCode = "200", description = "Rúbrica sin calificaciones actualizada."),
            @ApiResponse(responseCode = "400", description = "Pesos que no suman 100%, criterios repetidos o escala inválida "
                    + "(RUBRICA_INVALIDA / VALIDACION_FALLIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada (RECURSO_NO_ENCONTRADO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La rúbrica ya tiene calificaciones y no se confirmó (RUBRICA_EN_USO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @PutMapping
    public ResponseEntity<RubricaResponse> configurar(@PathVariable Long convocatoriaId,
                                                      @Valid @RequestBody RubricaRequest request,
                                                      @RequestParam(defaultValue = "false") boolean confirmar) {
        RubricaEvaluacionService.ResultadoConfiguracion resultado =
                rubricaService.configurar(convocatoriaId, request, confirmar);
        HttpStatus status = resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(resultado.rubrica());
    }
}
