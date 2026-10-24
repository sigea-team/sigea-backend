package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistasPublicosEventoResponse;
import com.sigea.demosigea_backend.service.ConferencistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Vista pública de los conferencistas de un evento publicado (HU-12, Criterio 4).
 * <p>
 * No requiere sesión ({@code GET /api/v1/publico/**} es público en {@code SecurityConfig}).
 * Lee la trayectoria vigente de cada conferencista en cada consulta, por lo que una actualización
 * hecha por el organizador se ve de inmediato. No expone documento, correo ni teléfono.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/publico/eventos/{eventoId}/conferencistas")
@RequiredArgsConstructor
@Tag(name = "Conferencistas (público)", description = "Conferencistas de un evento publicado, sin iniciar sesión (HU-12)")
public class ConferencistaPublicoController {

    private final ConferencistaService conferencistaService;

    @Operation(summary = "Conferencistas de un evento publicado",
            description = "Solo para eventos habilitados, en ejecución o cerrados. Un evento en configuración responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento y conferencistas con su trayectoria",
                    content = @Content(schema = @Schema(implementation = ConferencistasPublicosEventoResponse.class))),
            @ApiResponse(responseCode = "404", description = "El evento no existe o no está publicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ConferencistasPublicosEventoResponse> listar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId) {
        return ResponseEntity.ok(conferencistaService.listarPublico(eventoId));
    }
}
