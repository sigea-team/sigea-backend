package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaResponse;
import com.sigea.demosigea_backend.service.LineaTematicaService;
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
import java.util.Map;

/**
 * Controlador REST para la gestión de líneas temáticas de eventos (HU-05).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see LineaTematicaService
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Líneas Temáticas", description = "Definición y asociación de líneas temáticas por evento (HU-05)")
@SecurityRequirement(name = "bearerAuth")
public class LineaTematicaController {

    private final LineaTematicaService lineaTematicaService;

    @Operation(summary = "Listar líneas temáticas de un evento",
            description = "Retorna todas las líneas temáticas asociadas al evento indicado (Criterio 1).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de líneas temáticas del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = LineaTematicaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/lineas-tematicas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<LineaTematicaResponse>> listarPorEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(lineaTematicaService.listarPorEvento(eventoId));
    }

    @Operation(summary = "Obtener línea temática por ID",
            description = "Consulta la información detallada de una línea temática registrada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Línea temática encontrada",
                    content = @Content(schema = @Schema(implementation = LineaTematicaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Línea temática no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/lineas-tematicas/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<LineaTematicaResponse> obtenerPorId(
            @Parameter(description = "ID de la línea temática", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(lineaTematicaService.obtenerPorId(id));
    }

    @Operation(summary = "Registrar línea temática en un evento",
            description = "Registra una línea temática dentro de un evento (Criterio 1). Rechaza si el nombre está vacío (400) o si ya existe en el evento (409) (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Línea temática registrada",
                    content = @Content(schema = @Schema(implementation = LineaTematicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre vacío o datos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre de línea temática ya existente en este evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/lineas-tematicas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<LineaTematicaResponse> crearEnEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Valid @RequestBody LineaTematicaRequest request
    ) {
        LineaTematicaResponse creada = lineaTematicaService.crear(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/lineas-tematicas/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Registrar línea temática",
            description = "Registra una línea temática indicando el eventoId en el cuerpo de la solicitud.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Línea temática registrada",
                    content = @Content(schema = @Schema(implementation = LineaTematicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Nombre vacío o eventoId no especificado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre ya existente en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/lineas-tematicas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<LineaTematicaResponse> crear(@Valid @RequestBody LineaTematicaRequest request) {
        LineaTematicaResponse creada = lineaTematicaService.crear(null, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/lineas-tematicas/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Modificar línea temática",
            description = "Actualiza los datos (nombre o descripción) de una línea temática existente sin afectar registros históricos o propuestas asociadas (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Línea temática actualizada",
                    content = @Content(schema = @Schema(implementation = LineaTematicaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o nombre vacío",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Línea temática no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El nuevo nombre ya existe en otra línea temática del mismo evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/lineas-tematicas/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<LineaTematicaResponse> actualizar(
            @Parameter(description = "ID de la línea temática", example = "1") @PathVariable Long id,
            @Valid @RequestBody LineaTematicaRequest request
    ) {
        return ResponseEntity.ok(lineaTematicaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar línea temática",
            description = "Elimina una línea temática si no tiene propuestas o actividades asociadas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Línea temática eliminada"),
            @ApiResponse(responseCode = "404", description = "Línea temática no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La línea temática está en uso y no puede eliminarse",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/lineas-tematicas/{id}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_ELIMINAR', 'EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(
            @Parameter(description = "ID de la línea temática", example = "1") @PathVariable Long id
    ) {
        lineaTematicaService.eliminar(id);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Línea temática eliminada exitosamente.",
                "id", String.valueOf(id)
        ));
    }
}
