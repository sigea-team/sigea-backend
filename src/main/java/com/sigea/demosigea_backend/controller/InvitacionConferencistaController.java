package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConflictoConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitarConferencistaRequest;
import com.sigea.demosigea_backend.service.InvitacionConferencistaService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Invitaciones por correo a conferencistas nuevos de un evento (HU-12).
 * El administrador solo indica nombre y correo; el invitado registra sus datos desde el enlace
 * ({@link InvitacionConferencistaPublicaController}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/conferencistas/invitaciones")
@RequiredArgsConstructor
@Tag(name = "Conferencistas", description = "Registro de conferencistas y administración de su trayectoria (HU-12)")
@SecurityRequirement(name = "bearerAuth")
public class InvitacionConferencistaController {

    private final InvitacionConferencistaService invitacionService;

    @Operation(summary = "Listar invitaciones pendientes del evento",
            description = "Invitaciones enviadas que el conferencista aún no ha completado: 'pendiente' (enlace vigente) "
                    + "o 'vencida' (debe reenviarse). Incluye el enlace para copiarlo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitaciones pendientes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = InvitacionResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<InvitacionResponse>> listar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(invitacionService.listarPendientes(eventoId));
    }

    @Operation(summary = "Invitar a un conferencista nuevo",
            description = "Solo nombre y correo. Se envía un correo con un enlace vigente 3 días para que el conferencista "
                    + "registre sus datos y trayectoria. Si el correo ya es de un conferencista registrado → 409 "
                    + "CONFERENCISTA_EXISTENTE con la persona en 'coincidencia' para vincularla (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitación enviada",
                    content = @Content(schema = @Schema(implementation = InvitacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre o correo inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya registrado, ya en el evento, invitación vigente o evento cerrado",
                    content = @Content(schema = @Schema(implementation = ConflictoConferencistaResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<InvitacionResponse> invitar(
            @PathVariable Long eventoId,
            @Valid @RequestBody InvitarConferencistaRequest request) {
        InvitacionResponse invitacion = invitacionService.invitar(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{invitacionId}")
                .buildAndExpand(invitacion.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(invitacion);
    }

    @Operation(summary = "Reenviar invitación",
            description = "Genera un enlace nuevo (el anterior deja de servir) con 3 días de vigencia y lo envía otra vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitación reenviada",
                    content = @Content(schema = @Schema(implementation = InvitacionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La invitación ya no está pendiente o el evento está cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{invitacionId}/reenviar")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<InvitacionResponse> reenviar(
            @PathVariable Long eventoId,
            @Parameter(description = "ID de la invitación", example = "4") @PathVariable Long invitacionId) {
        return ResponseEntity.ok(invitacionService.reenviar(eventoId, invitacionId));
    }

    @Operation(summary = "Cancelar invitación", description = "El enlace deja de servir.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invitación cancelada"),
            @ApiResponse(responseCode = "404", description = "Invitación no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La invitación ya no está pendiente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{invitacionId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable Long eventoId, @PathVariable Long invitacionId) {
        invitacionService.cancelar(eventoId, invitacionId);
        return ResponseEntity.noContent().build();
    }
}
