package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionCompletadaResponse;
import com.sigea.demosigea_backend.dto.conferencista.InvitacionPublicaResponse;
import com.sigea.demosigea_backend.service.InvitacionConferencistaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enlace de invitación que recibe el conferencista por correo (HU-12). No requiere sesión: el token
 * del enlace identifica la invitación. Permitido en {@code SecurityConfig} para GET y POST.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/publico/invitaciones-conferencista/{token}")
@RequiredArgsConstructor
@Tag(name = "Conferencistas (público)", description = "Conferencistas de un evento publicado, sin iniciar sesión (HU-12)")
public class InvitacionConferencistaPublicaController {

    private final InvitacionConferencistaService invitacionService;

    @Operation(summary = "Consultar invitación",
            description = "Evento, nombre y correo de la invitación y, si la persona ya existe en SIGEA, sus datos para "
                    + "precargar el formulario. 410 si venció, ya se usó, fue cancelada o el evento ya no recibe conferencistas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitación vigente",
                    content = @Content(schema = @Schema(implementation = InvitacionPublicaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Enlace no válido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "Invitación vencida, usada, cancelada o evento cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<InvitacionPublicaResponse> consultar(@PathVariable String token) {
        return ResponseEntity.ok(invitacionService.consultar(token));
    }

    @Operation(summary = "Completar registro desde la invitación",
            description = "El conferencista registra documento, nombres, teléfono, tema y trayectoria (Criterios 1 y 2) "
                    + "y queda vinculado al evento. Documento de otra persona → 409 DATOS_PERSONA_DUPLICADOS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registro completado",
                    content = @Content(schema = @Schema(implementation = InvitacionCompletadaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Enlace no válido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Documento registrado con otro correo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "Invitación vencida, usada, cancelada o evento cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<InvitacionCompletadaResponse> completar(
            @PathVariable String token,
            @Valid @RequestBody CompletarInvitacionRequest request) {
        return ResponseEntity.ok(invitacionService.completar(token, request));
    }
}
