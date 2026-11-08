package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.agenda.AgendaRechazadaResponse;
import com.sigea.demosigea_backend.dto.agenda.GeneracionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.PublicacionAgendaResponse;
import com.sigea.demosigea_backend.dto.agenda.ValidacionAgendaResponse;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generación automática y publicación de la agenda del evento (HU-22, RF38 + RF39).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/agenda")
@RequiredArgsConstructor
@Tag(name = "Agenda: generación y publicación", description = "HU-22: generar automáticamente y publicar la agenda")
@SecurityRequirement(name = "bearerAuth")
public class AgendaController {

    private static final String GESTIONAR = "hasAnyAuthority('AGENDA_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";
    private static final String CONSULTAR = "hasAnyAuthority('EVENTOS_VER', 'AGENDA_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    private final AgendaService agendaService;

    @Operation(summary = "Proponer una agenda (sin guardar)",
            description = "Criterios 1 y 2: ubica las actividades por programar en las salas y horarios disponibles "
                    + "sin cruces de horario, sala ni ponente. No guarda nada. Lista las que no se pudieron ubicar "
                    + "y el motivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Propuesta calculada."),
            @ApiResponse(responseCode = "404", description = "Evento inexistente."),
            @ApiResponse(responseCode = "409", description = "El evento está cerrado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/generar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<GeneracionAgendaResponse> proponer(
            @PathVariable Long eventoId,
            @Parameter(description = "También reubica las actividades que ubicó una generación anterior y nadie ajustó")
            @RequestParam(defaultValue = "false") boolean reubicarAutomaticas) {
        return ResponseEntity.ok(agendaService.proponer(eventoId, reubicarAutomaticas));
    }

    @Operation(summary = "Generar y guardar la agenda",
            description = "Criterio 1: calcula la propuesta y la guarda. Las actividades ubicadas quedan programadas "
                    + "(asignacionAutomatica = true) y se pueden ajustar a mano antes de publicar; las demás siguen "
                    + "por programar (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda generada y guardada."),
            @ApiResponse(responseCode = "404", description = "Evento inexistente."),
            @ApiResponse(responseCode = "409", description = "El evento está cerrado.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/generar/aplicar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<GeneracionAgendaResponse> aplicar(
            @PathVariable Long eventoId,
            @RequestParam(defaultValue = "false") boolean reubicarAutomaticas) {
        return ResponseEntity.ok(agendaService.aplicar(eventoId, reubicarAutomaticas));
    }

    @Operation(summary = "Revisar la agenda antes de publicarla",
            description = "Criterios 3 y 4: indica si se puede publicar, qué actividades no tienen sala u horario y "
                    + "qué cruces hay, y el estado de publicación de cada día.")
    @GetMapping("/validacion")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<ValidacionAgendaResponse> validar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(agendaService.validar(eventoId));
    }

    @Operation(summary = "Publicar la agenda",
            description = "Criterios 3 y 4: publica la agenda organizada por día si todas las actividades tienen sala "
                    + "y horario y no hay cruces. Se puede volver a publicar después de cambios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda publicada."),
            @ApiResponse(responseCode = "404", description = "Evento inexistente."),
            @ApiResponse(responseCode = "409", description = "AGENDA_INCOMPLETA, AGENDA_CON_CONFLICTOS o AGENDA_VACIA "
                    + "(o OPERACION_NO_PERMITIDA si el evento no está publicado o está cerrado).",
                    content = @Content(schema = @Schema(implementation = AgendaRechazadaResponse.class)))
    })
    @PostMapping("/publicar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<PublicacionAgendaResponse> publicar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(agendaService.publicar(eventoId));
    }
}
