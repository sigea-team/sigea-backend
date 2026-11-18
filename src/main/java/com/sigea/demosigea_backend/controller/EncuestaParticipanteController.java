package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.EnvioEncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResponderEncuestaRequest;
import com.sigea.demosigea_backend.service.RespuestaEncuestaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import java.util.List;

/**
 * Encuestas de satisfacción del lado del participante (HU-25, RF46, CU-22).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/encuestas")
@RequiredArgsConstructor
@Tag(name = "Encuestas de satisfacción (participante)",
        description = "HU-25: encuestas habilitadas para quien asistió y envío de respuestas")
@SecurityRequirement(name = "bearerAuth")
public class EncuestaParticipanteController {

    private final RespuestaEncuestaService respuestaEncuestaService;

    @Operation(summary = "Mis encuestas",
            description = "Criterio 1: encuestas publicadas de los eventos y actividades a los que asistió el usuario, "
                    + "y las cerradas que ya respondió, con la marca de respondida.")
    @GetMapping("/mias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EncuestaParticipanteResponse>> misEncuestas() {
        return ResponseEntity.ok(respuestaEncuestaService.misEncuestas());
    }

    @Operation(summary = "Formulario de la encuesta", description = "Preguntas de la encuesta para responderla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encuesta con sus preguntas"),
            @ApiResponse(responseCode = "403", description = "ENCUESTA_NO_HABILITADA: no registró asistencia (Criterio 3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada o no publicada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{encuestaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EncuestaParticipanteResponse> formulario(@PathVariable Long encuestaId) {
        return ResponseEntity.ok(respuestaEncuestaService.formulario(encuestaId));
    }

    @Operation(summary = "Responder encuesta",
            description = "Criterio 2: guarda las respuestas, anónimas o identificadas según la configuración. "
                    + "Las preguntas opcionales se pueden omitir.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Respuestas guardadas"),
            @ApiResponse(responseCode = "400", description = "VALIDACION_FALLIDA o RESPUESTAS_INVALIDAS (detalle por "
                    + "pregunta en erroresValidacion con la clave pregunta_<id>)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ENCUESTA_NO_HABILITADA: no registró asistencia (Criterio 3)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada o no publicada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_CERRADA o ENCUESTA_YA_RESPONDIDA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{encuestaId}/respuestas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnvioEncuestaResponse> responder(@PathVariable Long encuestaId,
                                                           @Valid @RequestBody ResponderEncuestaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(respuestaEncuestaService.responder(encuestaId, request));
    }
}
