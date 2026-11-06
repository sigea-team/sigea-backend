package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.comiteevaluador.ActualizarEvaluadorRequest;
import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorRequest;
import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorResponse;
import com.sigea.demosigea_backend.service.ComiteEvaluadorService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST del comité evaluador de una convocatoria (HU-15, RF23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Comité evaluador",
        description = "Registro de los evaluadores de una convocatoria con su área de experticia (HU-15)")
@RestController
@RequestMapping("/api/v1/convocatorias/{convocatoriaId}/comite-evaluador")
@RequiredArgsConstructor
public class ComiteEvaluadorController {

    private final ComiteEvaluadorService comiteService;

    @Operation(summary = "Evaluadores del comité",
            description = "Evaluadores registrados en la convocatoria, con su área de experticia, si tienen cuenta y "
                    + "cuántas propuestas tienen asignadas.")
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_ASIGNAR', "
            + "'ROLE_ADMIN', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<EvaluadorResponse>> listar(@PathVariable Long convocatoriaId) {
        return ResponseEntity.ok(comiteService.listar(convocatoriaId));
    }

    @Operation(summary = "Registrar evaluador",
            description = "Registra al evaluador por su correo con su área de experticia y lo deja disponible para "
                    + "asignación (Criterio 3). Si el correo no está registrado se crea la persona con los datos "
                    + "enviados. Un correo ya registrado en el comité se rechaza (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evaluador registrado."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o faltan los datos de la persona nueva "
                    + "(VALIDACION_FALLIDA / SOLICITUD_INVALIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria o afiliación no encontrada (RECURSO_NO_ENCONTRADO).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Correo ya registrado en el comité (EVALUADOR_DUPLICADO) o documento "
                    + "de otra persona (DATOS_PERSONA_DUPLICADOS).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @PostMapping
    public ResponseEntity<EvaluadorResponse> registrar(@PathVariable Long convocatoriaId,
                                                       @Valid @RequestBody EvaluadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comiteService.registrar(convocatoriaId, request));
    }

    @Operation(summary = "Actualizar área de experticia")
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @PutMapping("/{comiteEvaluadorId}")
    public ResponseEntity<EvaluadorResponse> actualizar(@PathVariable Long convocatoriaId,
                                                        @PathVariable Long comiteEvaluadorId,
                                                        @Valid @RequestBody ActualizarEvaluadorRequest request) {
        return ResponseEntity.ok(comiteService.actualizar(convocatoriaId, comiteEvaluadorId, request));
    }

    @Operation(summary = "Retirar evaluador",
            description = "Retira al evaluador del comité. Si tiene propuestas asignadas se rechaza (OPERACION_NO_PERMITIDA).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evaluador retirado."),
            @ApiResponse(responseCode = "404", description = "Evaluador no encontrado en la convocatoria.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Tiene propuestas asignadas (OPERACION_NO_PERMITIDA).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('EVALUACION_CONFIGURAR', 'CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    @DeleteMapping("/{comiteEvaluadorId}")
    public ResponseEntity<Void> retirar(@PathVariable Long convocatoriaId, @PathVariable Long comiteEvaluadorId) {
        comiteService.retirar(convocatoriaId, comiteEvaluadorId);
        return ResponseEntity.noContent().build();
    }
}
