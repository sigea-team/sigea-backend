package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.comite.MiembroComiteRequest;
import com.sigea.demosigea_backend.dto.comite.MiembroComiteResponse;
import com.sigea.demosigea_backend.service.ComiteOrganizadorService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para el registro de responsables y comité organizador de un evento (HU-06, RF05).
 * <p>
 * El comité es un sub-recurso del evento: {@code /api/v1/eventos/{eventoId}/comite}.
 * Usa los permisos del módulo EVENTOS ya sembrados en el catálogo (changeset 003).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see ComiteOrganizadorService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/comite")
@RequiredArgsConstructor
@Tag(name = "Comité Organizador", description = "Responsables y miembros del comité organizador de un evento (HU-06)")
@SecurityRequirement(name = "bearerAuth")
public class ComiteOrganizadorController {

    private final ComiteOrganizadorService comiteService;

    @Operation(summary = "Listar comité organizador",
            description = "Por defecto devuelve solo los miembros vigentes. Con incluirHistorial=true devuelve también "
                    + "las participaciones retiradas, conservadas como historial (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comité del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MiembroComiteResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<MiembroComiteResponse>> listar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "Incluir miembros retirados (historial)", example = "false")
            @RequestParam(defaultValue = "false") boolean incluirHistorial
    ) {
        return ResponseEntity.ok(comiteService.listar(eventoId, incluirHistorial));
    }

    @Operation(summary = "Agregar responsable o miembro del comité",
            description = "Asocia una persona registrada (por personaId o numeroDocumento) al evento con su rol dentro "
                    + "del comité (Criterio 1). Si la persona ya es miembro vigente del evento → 409 "
                    + "MIEMBRO_COMITE_DUPLICADO (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Miembro agregado",
                    content = @Content(schema = @Schema(implementation = MiembroComiteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o persona no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Miembro duplicado o evento cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<MiembroComiteResponse> agregar(
            @PathVariable Long eventoId,
            @Valid @RequestBody MiembroComiteRequest request
    ) {
        MiembroComiteResponse creado = comiteService.agregar(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{miembroId}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Retirar miembro del comité",
            description = "Retira al miembro de la lista vigente sin borrar el registro: queda con activo=false y "
                    + "fechaRetiro, consultable con incluirHistorial=true (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Miembro retirado",
                    content = @Content(schema = @Schema(implementation = MiembroComiteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o miembro no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El miembro ya estaba retirado o el evento está cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{miembroId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<MiembroComiteResponse> retirar(
            @PathVariable Long eventoId,
            @Parameter(description = "ID de la participación en el comité", example = "5") @PathVariable Long miembroId
    ) {
        return ResponseEntity.ok(comiteService.retirar(eventoId, miembroId));
    }
}
