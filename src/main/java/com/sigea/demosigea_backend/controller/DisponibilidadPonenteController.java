package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.sala.ActualizacionDisponibilidadPonenteResponse;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadHorariaRequest;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadPonenteResponse;
import com.sigea.demosigea_backend.service.SalaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la disponibilidad horaria de ponentes en un evento — HU-Salas, Criterios 2 y 5.
 * <p>
 * Sub-recurso de evento y ponente: {@code /api/v1/eventos/{eventoId}/ponentes/{personaId}/disponibilidad}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see SalaService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/ponentes/{personaId}/disponibilidad")
@RequiredArgsConstructor
@Tag(name = "Disponibilidad de Ponentes",
        description = "Gestión de franjas horarias de disponibilidad de ponentes en el evento (HU-Salas, Criterios 2 y 5)")
@SecurityRequirement(name = "bearerAuth")
public class DisponibilidadPonenteController {

    private final SalaService salaService;

    @Operation(summary = "Consultar disponibilidad horaria de un ponente",
            description = "Devuelve las franjas de disponibilidad registradas para el ponente en el evento, "
                    + "ordenadas cronológicamente (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franjas de disponibilidad del ponente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DisponibilidadPonenteResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento o ponente no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<DisponibilidadPonenteResponse>> obtener(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "ID de la persona (ponente)", example = "5") @PathVariable Long personaId
    ) {
        return ResponseEntity.ok(salaService.obtenerDisponibilidadPonente(eventoId, personaId));
    }

    @Operation(summary = "Actualizar disponibilidad horaria de un ponente",
            description = "Reemplaza TODAS las franjas de disponibilidad del ponente en el evento por las recibidas "
                    + "(Criterio 2). Tras guardar, verifica si alguna actividad ya programada con ese ponente en el "
                    + "evento queda fuera de la nueva disponibilidad y devuelve la lista de conflictos (Criterio 5). "
                    + "La respuesta es siempre 200 OK; los conflictos son una advertencia, no un bloqueo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada. El campo 'conflictos' puede "
                    + "contener actividades en conflicto (advertencia informativa).",
                    content = @Content(schema = @Schema(implementation = ActualizacionDisponibilidadPonenteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Franjas con datos inválidos (hora_fin ≤ hora_inicio)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o ponente no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ActualizacionDisponibilidadPonenteResponse> actualizar(
            @PathVariable Long eventoId,
            @PathVariable Long personaId,
            @Valid @RequestBody List<@Valid DisponibilidadHorariaRequest> franjas
    ) {
        return ResponseEntity.ok(salaService.actualizarDisponibilidadPonente(eventoId, personaId, franjas));
    }
}
