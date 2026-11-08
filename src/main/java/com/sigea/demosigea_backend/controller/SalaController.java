package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.sala.ActualizacionDisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadHorariaRequest;
import com.sigea.demosigea_backend.dto.sala.DisponibilidadSalaResponse;
import com.sigea.demosigea_backend.dto.sala.SalaRequest;
import com.sigea.demosigea_backend.dto.sala.SalaResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controlador REST para la gestión de salas y su disponibilidad horaria — HU-Salas.
 * <p>
 * Sub-recurso del evento: {@code /api/v1/eventos/{eventoId}/salas}. Cubre los
 * Criterios 1, 3, 4 y 5 de la historia de usuario.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see SalaService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/salas")
@RequiredArgsConstructor
@Tag(name = "Salas", description = "Gestión de salas y disponibilidad horaria del evento (HU-Salas)")
@SecurityRequirement(name = "bearerAuth")
public class SalaController {

    private final SalaService salaService;

    @Operation(summary = "Listar salas del evento",
            description = "Devuelve todas las salas del evento ordenadas por nombre, con sus franjas de disponibilidad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de salas",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SalaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<SalaResponse>> listar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(salaService.listar(eventoId));
    }

    @Operation(summary = "Consultar una sala",
            description = "Obtiene los datos de una sala específica del evento con sus disponibilidades.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala encontrada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> obtener(
            @PathVariable Long eventoId,
            @PathVariable Long salaId
    ) {
        return ResponseEntity.ok(salaService.obtener(eventoId, salaId));
    }

    @Operation(summary = "Crear sala",
            description = "Registra una nueva sala con nombre, ubicación, tipo (fisica/virtual), aforo máximo y "
                    + "disponibilidad horaria opcional (Criterio 1). "
                    + "Rechaza nombre duplicado en el evento (409 SALA_DUPLICADA, Criterio 3) y "
                    + "aforo ≤ 0 (400 VALIDACION_FALLIDA, Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sala creada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (aforo ≤ 0, campos faltantes)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre de sala duplicado en el evento (SALA_DUPLICADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> crear(
            @PathVariable Long eventoId,
            @Valid @RequestBody SalaRequest request
    ) {
        SalaResponse creada = salaService.crear(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Actualizar sala",
            description = "Modifica los datos de una sala. "
                    + "Si el nuevo nombre ya existe en otra sala del evento → 409 SALA_DUPLICADA (Criterio 3). "
                    + "Si el nuevo aforo es menor que las inscripciones activas existentes → 409 AFORO_INVALIDO (Criterio 4). "
                    + "Si se incluye disponibilidades, las reemplaza en bloque.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala actualizada",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado (SALA_DUPLICADA) o aforo inválido (AFORO_INVALIDO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> actualizar(
            @PathVariable Long eventoId,
            @PathVariable Long salaId,
            @Valid @RequestBody SalaRequest request
    ) {
        return ResponseEntity.ok(salaService.actualizar(eventoId, salaId, request));
    }

    @Operation(summary = "Eliminar sala",
            description = "Elimina la sala si no tiene actividades programadas. "
                    + "Si tiene actividades → 409 OPERACION_NO_PERMITIDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sala eliminada"),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La sala tiene actividades programadas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long eventoId,
            @PathVariable Long salaId
    ) {
        salaService.eliminar(eventoId, salaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar disponibilidad horaria de la sala",
            description = "Devuelve todas las franjas de disponibilidad registradas para la sala.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Franjas de disponibilidad",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = DisponibilidadSalaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{salaId}/disponibilidad")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<DisponibilidadSalaResponse>> obtenerDisponibilidad(
            @PathVariable Long eventoId,
            @PathVariable Long salaId
    ) {
        return ResponseEntity.ok(salaService.obtenerDisponibilidadSala(eventoId, salaId));
    }

    @Operation(summary = "Actualizar disponibilidad horaria de la sala",
            description = "Reemplaza TODAS las franjas de disponibilidad de la sala por las recibidas. "
                    + "Tras guardar, verifica si alguna actividad ya programada en la sala queda fuera de la nueva "
                    + "disponibilidad y devuelve la lista de conflictos (Criterio 5). "
                    + "La respuesta es siempre 200 OK; los conflictos son una advertencia, no un bloqueo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidad actualizada. El campo 'conflictos' puede contener actividades en conflicto.",
                    content = @Content(schema = @Schema(implementation = ActualizacionDisponibilidadSalaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Franjas con datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{salaId}/disponibilidad")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ActualizacionDisponibilidadSalaResponse> actualizarDisponibilidad(
            @PathVariable Long eventoId,
            @PathVariable Long salaId,
            @Valid @RequestBody List<@Valid DisponibilidadHorariaRequest> franjas
    ) {
        return ResponseEntity.ok(salaService.actualizarDisponibilidadSala(eventoId, salaId, franjas));
    }

    @Operation(summary = "Vincular sala existente al evento",
            description = "Asocia una sala institucional del catálogo al evento, permitiendo reutilizarla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sala vinculada exitosamente",
                    content = @Content(schema = @Schema(implementation = SalaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/vincular/{salaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<SalaResponse> vincular(
            @PathVariable Long eventoId,
            @PathVariable Long salaId
    ) {
        return ResponseEntity.ok(salaService.vincularSalaAEvento(eventoId, salaId));
    }

    @Operation(summary = "Desvincular sala del evento",
            description = "Remueve la asociación de la sala con este evento sin eliminar la sala del catálogo general.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sala desvinculada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento o sala no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La sala tiene actividades programadas en este evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{salaId}/desvincular")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> desvincular(
            @PathVariable Long eventoId,
            @PathVariable Long salaId
    ) {
        salaService.desvincularSalaDeEvento(eventoId, salaId);
        return ResponseEntity.noContent().build();
    }
}
