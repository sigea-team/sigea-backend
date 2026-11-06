package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.clasificacion.AjusteClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.AplicacionClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.ClasificacionConvocatoriaResponse;
import com.sigea.demosigea_backend.dto.clasificacion.HistorialClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.PropuestaClasificacionResponse;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionRequest;
import com.sigea.demosigea_backend.dto.clasificacion.UmbralesClasificacionResponse;
import com.sigea.demosigea_backend.service.ClasificacionPropuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * Controlador REST para clasificar las propuestas evaluadas según su resultado (HU-18, RF27).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Clasificación de propuestas",
     description = "Clasificación de propuestas evaluadas como aceptada, aceptada con modificaciones o rechazada (HU-18)")
@RestController
@RequestMapping("/api/v1/clasificaciones")
@RequiredArgsConstructor
public class ClasificacionPropuestaController {

    private static final String GESTIONAR =
            "hasAnyAuthority('PROPUESTAS_CLASIFICAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";
    private static final String CONSULTAR =
            "hasAnyAuthority('PROPUESTAS_CLASIFICAR', 'CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_VER', 'ROLE_ADMIN', 'ADMIN')";

    private final ClasificacionPropuestaService clasificacionService;

    @Operation(summary = "Listar propuestas por resultado",
            description = "Propuestas de la convocatoria agrupadas por clasificación, con los umbrales vigentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado obtenido."),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(CONSULTAR)
    @GetMapping("/convocatorias/{convocatoriaId}")
    public ResponseEntity<ClasificacionConvocatoriaResponse> listarPorConvocatoria(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(clasificacionService.listarPorConvocatoria(convocatoriaId));
    }

    @Operation(summary = "Consultar umbrales de clasificación")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Umbrales vigentes."),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(CONSULTAR)
    @GetMapping("/convocatorias/{convocatoriaId}/umbrales")
    public ResponseEntity<UmbralesClasificacionResponse> obtenerUmbrales(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(clasificacionService.obtenerUmbrales(convocatoriaId));
    }

    @Operation(summary = "Definir umbrales de clasificación (Criterio 1)",
            description = "Puntuación mínima (0 - 10) para aprobada y para aprobada con ajustes. Las propuestas ya "
                    + "clasificadas no cambian hasta volver a aplicar la clasificación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Umbrales guardados."),
            @ApiResponse(responseCode = "400", description = "Umbrales inválidos (VALIDACION_FALLIDA / SOLICITUD_INVALIDA)."),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(GESTIONAR)
    @PutMapping("/convocatorias/{convocatoriaId}/umbrales")
    public ResponseEntity<UmbralesClasificacionResponse> configurarUmbrales(
            @PathVariable Long convocatoriaId, @Valid @RequestBody UmbralesClasificacionRequest request) {
        return ResponseEntity.ok(clasificacionService.configurarUmbrales(convocatoriaId, request));
    }

    @Operation(summary = "Clasificar todas las propuestas evaluadas de la convocatoria (Criterios 1 y 3)",
            description = "Aplica los umbrales a cada propuesta con evaluación completa. Las demás se devuelven en "
                    + "'omitidas' con el motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clasificación aplicada."),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(GESTIONAR)
    @PostMapping("/convocatorias/{convocatoriaId}/aplicar")
    public ResponseEntity<AplicacionClasificacionResponse> aplicarConvocatoria(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(clasificacionService.aplicarConvocatoria(convocatoriaId));
    }

    @Operation(summary = "Consultar la clasificación de una propuesta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situación de clasificación."),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(CONSULTAR)
    @GetMapping("/propuestas/{propuestaId}")
    public ResponseEntity<PropuestaClasificacionResponse> obtenerPorPropuesta(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(clasificacionService.obtenerPorPropuesta(propuestaId));
    }

    @Operation(summary = "Clasificar una propuesta según los umbrales (Criterios 1 y 3)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clasificación aplicada."),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada (RECURSO_NO_ENCONTRADO)."),
            @ApiResponse(responseCode = "409", description = "Evaluación incompleta (PROPUESTA_NO_EVALUADA).")
    })
    @PreAuthorize(GESTIONAR)
    @PostMapping("/propuestas/{propuestaId}/aplicar")
    public ResponseEntity<PropuestaClasificacionResponse> aplicar(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(clasificacionService.aplicar(propuestaId));
    }

    @Operation(summary = "Ajustar manualmente la clasificación (Criterio 2)",
            description = "Exige un motivo, que queda en el historial de clasificaciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clasificación ajustada."),
            @ApiResponse(responseCode = "400", description = "Sin motivo o clasificación inválida (VALIDACION_FALLIDA / SOLICITUD_INVALIDA)."),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada (RECURSO_NO_ENCONTRADO)."),
            @ApiResponse(responseCode = "409", description = "Evaluación incompleta (PROPUESTA_NO_EVALUADA).")
    })
    @PreAuthorize(GESTIONAR)
    @PutMapping("/propuestas/{propuestaId}")
    public ResponseEntity<PropuestaClasificacionResponse> ajustar(
            @PathVariable Long propuestaId, @Valid @RequestBody AjusteClasificacionRequest request) {
        return ResponseEntity.ok(clasificacionService.ajustar(propuestaId, request));
    }

    @Operation(summary = "Historial de clasificaciones de una propuesta (Criterio 2)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial, del más reciente al más antiguo."),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada (RECURSO_NO_ENCONTRADO).")
    })
    @PreAuthorize(CONSULTAR)
    @GetMapping("/propuestas/{propuestaId}/historial")
    public ResponseEntity<List<HistorialClasificacionResponse>> historial(@PathVariable Long propuestaId) {
        return ResponseEntity.ok(clasificacionService.historial(propuestaId));
    }
}
