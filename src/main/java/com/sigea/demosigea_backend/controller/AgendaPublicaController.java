package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.agenda.AgendaPublicaResponse;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.AgendaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública de la agenda publicada (HU-22, Criterio 3). No requiere autenticación
 * ({@code GET /api/v1/publico/**} está abierto en {@code SecurityConfig}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/publico/eventos/{eventoId}/agenda")
@RequiredArgsConstructor
@Tag(name = "Agenda (público)", description = "Agenda publicada del evento, organizada por día y hora")
public class AgendaPublicaController {

    private final AgendaService agendaService;

    @Operation(summary = "Agenda publicada del evento",
            description = "Días publicados con sus actividades en orden de hora. Responde 404 si el evento no existe, "
                    + "no está publicado o su agenda todavía no se ha publicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda publicada."),
            @ApiResponse(responseCode = "404", description = "Sin agenda publicada.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<AgendaPublicaResponse> obtener(@PathVariable Long eventoId) {
        return ResponseEntity.ok(agendaService.agendaPublica(eventoId));
    }
}
