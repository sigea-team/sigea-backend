package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.service.ConvocatoriaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de convocatorias académicas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see ConvocatoriaService
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Convocatorias", description = "Gestión de convocatorias públicas para ponencias y trabajos académicos")
@SecurityRequirement(name = "bearerAuth")
public class ConvocatoriaController {

    private final ConvocatoriaService convocatoriaService;

    @Operation(summary = "Listar convocatorias",
            description = "Devuelve el listado de convocatorias, permitiendo filtrar opcionalmente por evento e estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de convocatorias",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConvocatoriaResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Filtro o parámetro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/convocatorias")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<ConvocatoriaResponse>> listar(
            @Parameter(description = "ID del evento para filtrar convocatorias", example = "1")
            @RequestParam(required = false) Long eventoId,
            @Parameter(description = "Estado de la convocatoria", example = "publicada")
            @RequestParam(required = false) EstadoConvocatoria estado
    ) {
        return ResponseEntity.ok(convocatoriaService.listar(eventoId, estado));
    }

    @Operation(summary = "Listar convocatorias de un evento",
            description = "Devuelve todas las convocatorias asociadas a un evento específico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de convocatorias del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConvocatoriaResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/convocatorias")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<ConvocatoriaResponse>> listarPorEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Parameter(description = "Estado de la convocatoria", example = "publicada")
            @RequestParam(required = false) EstadoConvocatoria estado
    ) {
        return ResponseEntity.ok(convocatoriaService.listar(eventoId, estado));
    }

    @Operation(summary = "Consultar convocatoria por ID",
            description = "Devuelve los detalles de una convocatoria específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria encontrada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/convocatorias/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> obtenerPorId(
            @Parameter(description = "ID de la convocatoria", example = "1") @PathVariable Long id
    ) {
        return ResponseEntity.ok(convocatoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Crear convocatoria",
            description = "Crea una nueva convocatoria asociada a un evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convocatoria creada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos requeridos faltantes o rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/convocatorias")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> crear(@Valid @RequestBody ConvocatoriaRequest request) {
        ConvocatoriaResponse creada = convocatoriaService.crear(null, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/convocatorias/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Crear convocatoria para un evento",
            description = "Crea una nueva convocatoria asociada a un evento específico en el path URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convocatoria creada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos requeridos faltantes o rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/convocatorias")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> crearEnEvento(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId,
            @Valid @RequestBody ConvocatoriaRequest request
    ) {
        ConvocatoriaResponse creada = convocatoriaService.crear(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/convocatorias/{id}")
                .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Actualizar convocatoria",
            description = "Modifica los datos de una convocatoria existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria actualizada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o rango de fechas inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/convocatorias/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> actualizar(
            @Parameter(description = "ID de la convocatoria", example = "1") @PathVariable Long id,
            @Valid @RequestBody ConvocatoriaRequest request
    ) {
        return ResponseEntity.ok(convocatoriaService.actualizar(id, request));
    }

    @Operation(summary = "Eliminar convocatoria",
            description = "Elimina una convocatoria de la plataforma.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria eliminada"),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/convocatorias/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(
            @Parameter(description = "ID de la convocatoria", example = "1") @PathVariable Long id
    ) {
        convocatoriaService.eliminar(id);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Convocatoria eliminada exitosamente.",
                "id", String.valueOf(id)
        ));
    }
}
