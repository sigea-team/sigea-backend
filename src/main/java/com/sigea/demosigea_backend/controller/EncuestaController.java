package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaRequest;
import com.sigea.demosigea_backend.dto.encuesta.EncuestaResponse;
import com.sigea.demosigea_backend.dto.encuesta.RespuestasParticipanteResponse;
import com.sigea.demosigea_backend.dto.encuesta.ResultadosEncuestaResponse;
import com.sigea.demosigea_backend.service.EncuestaService;
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
 * Configuración, publicación y resultados de las encuestas de satisfacción de un evento (HU-25, RF46).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/encuestas")
@RequiredArgsConstructor
@Tag(name = "Encuestas de satisfacción (organizador)",
        description = "HU-25: configurar, publicar y cerrar encuestas del evento o de una actividad y consultar resultados")
@SecurityRequirement(name = "bearerAuth")
public class EncuestaController {

    static final String PERMISO_GESTION = "hasAnyAuthority('ENCUESTAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";
    static final String PERMISO_RESULTADOS =
            "hasAnyAuthority('ENCUESTAS_GESTIONAR', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')";

    private final EncuestaService encuestaService;

    @Operation(summary = "Crear encuesta",
            description = "Crea la encuesta en borrador, para el evento en general (sin actividadId) o para una "
                    + "actividad. Define si las respuestas serán anónimas o identificadas (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encuesta creada en borrador"),
            @ApiResponse(responseCode = "400", description = "VALIDACION_FALLIDA o ENCUESTA_CONFIGURACION_INVALIDA "
                    + "(detalle por campo en erroresValidacion)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento o actividad no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<EncuestaResponse> crear(@PathVariable Long eventoId,
                                                  @Valid @RequestBody EncuestaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(encuestaService.crear(eventoId, request));
    }

    @Operation(summary = "Listar encuestas del evento",
            description = "Encuestas del evento con sus preguntas, habilitados (asistentes) y respuestas recibidas.")
    @GetMapping
    @PreAuthorize(PERMISO_RESULTADOS)
    public ResponseEntity<List<EncuestaResponse>> listar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(encuestaService.listar(eventoId));
    }

    @Operation(summary = "Consultar encuesta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encuesta"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{encuestaId}")
    @PreAuthorize(PERMISO_RESULTADOS)
    public ResponseEntity<EncuestaResponse> obtener(@PathVariable Long eventoId, @PathVariable Long encuestaId) {
        return ResponseEntity.ok(encuestaService.obtener(eventoId, encuestaId));
    }

    @Operation(summary = "Modificar encuesta en borrador",
            description = "Reemplaza la configuración y todas las preguntas. Solo en borrador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encuesta modificada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION_FALLIDA o ENCUESTA_CONFIGURACION_INVALIDA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_NO_EDITABLE (ya publicada o cerrada)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{encuestaId}")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<EncuestaResponse> actualizar(@PathVariable Long eventoId, @PathVariable Long encuestaId,
                                                       @Valid @RequestBody EncuestaRequest request) {
        return ResponseEntity.ok(encuestaService.actualizar(eventoId, encuestaId, request));
    }

    @Operation(summary = "Eliminar encuesta", description = "Solo si todavía no tiene respuestas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Encuesta eliminada"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_NO_EDITABLE (tiene respuestas)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{encuestaId}")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<Void> eliminar(@PathVariable Long eventoId, @PathVariable Long encuestaId) {
        encuestaService.eliminar(eventoId, encuestaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Publicar encuesta",
            description = "Criterio 1: la habilita para los participantes que asistieron al evento o a la actividad y "
                    + "les avisa en la campana. Después ya no se puede modificar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encuesta publicada"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_ESTADO_INVALIDO, ENCUESTA_INCOMPLETA (sin "
                    + "preguntas o fecha de cierre vencida), EVENTO_NO_DISPONIBLE o ACTIVIDAD_NO_DISPONIBLE",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{encuestaId}/publicar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<EncuestaResponse> publicar(@PathVariable Long eventoId, @PathVariable Long encuestaId) {
        return ResponseEntity.ok(encuestaService.publicar(eventoId, encuestaId));
    }

    @Operation(summary = "Cerrar encuesta", description = "Deja de recibir respuestas; los resultados se conservan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encuesta cerrada"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_ESTADO_INVALIDO (no está publicada)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{encuestaId}/cerrar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<EncuestaResponse> cerrar(@PathVariable Long eventoId, @PathVariable Long encuestaId) {
        return ResponseEntity.ok(encuestaService.cerrar(eventoId, encuestaId));
    }

    @Operation(summary = "Resultados consolidados",
            description = "Criterio 4: por pregunta, promedio y distribución de la escala, conteo por opción y "
                    + "respuestas de texto; además, habilitados, respuestas y tasa de respuesta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados consolidados"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{encuestaId}/resultados")
    @PreAuthorize(PERMISO_RESULTADOS)
    public ResponseEntity<ResultadosEncuestaResponse> resultados(@PathVariable Long eventoId,
                                                                 @PathVariable Long encuestaId) {
        return ResponseEntity.ok(encuestaService.resultados(eventoId, encuestaId));
    }

    @Operation(summary = "Respuestas por participante",
            description = "Solo encuestas identificadas (Criterio 2): quién respondió y qué respondió.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Respuestas por participante"),
            @ApiResponse(responseCode = "404", description = "Encuesta no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ENCUESTA_ANONIMA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{encuestaId}/respuestas")
    @PreAuthorize(PERMISO_RESULTADOS)
    public ResponseEntity<List<RespuestasParticipanteResponse>> respuestasPorParticipante(
            @PathVariable Long eventoId, @PathVariable Long encuestaId) {
        return ResponseEntity.ok(encuestaService.respuestasPorParticipante(eventoId, encuestaId));
    }
}
