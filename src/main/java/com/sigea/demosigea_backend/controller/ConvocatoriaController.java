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
 * Controlador REST para la gestión de convocatorias asociadas a eventos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see ConvocatoriaService
 */
@RestController
@RequestMapping("/api/v1/convocatorias")
@RequiredArgsConstructor
@Tag(name = "Convocatorias", description = "Gestión de convocatorias para recepción de propuestas académicas")
@SecurityRequirement(name = "bearerAuth")
public class ConvocatoriaController {

    private final ConvocatoriaService convocatoriaService;

    @Operation(summary = "Listar convocatorias",
            description = "Devuelve el listado de convocatorias, permitiendo filtrar por ID de evento y por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de convocatorias",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConvocatoriaResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetro inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<ConvocatoriaResponse>> listar(
            @Parameter(description = "ID del evento para filtrar", example = "1")
            @RequestParam(required = false) Long eventoId,
            @Parameter(description = "Estado de la convocatoria (borrador, publicada, cerrada)", example = "borrador")
            @RequestParam(required = false) EstadoConvocatoria estado
    ) {
        return ResponseEntity.ok(convocatoriaService.listar(eventoId, estado));
    }

    @Operation(summary = "Consultar convocatoria por ID",
            description = "Obtiene los detalles de una convocatoria específica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria encontrada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.obtenerPorId(id));
    }

    @Operation(summary = "Crear convocatoria",
            description = "Registra una convocatoria asociada a un evento con sus fechas de apertura y cierre. Queda en estado 'borrador' (Criterio 1). "
                    + "La fecha de cierre debe ser posterior a la de apertura (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Convocatoria creada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos obligatorios incompletos o fechas inválidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> crear(@Valid @RequestBody ConvocatoriaRequest request) {
        ConvocatoriaResponse creada = convocatoriaService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
        .buildAndExpand(creada.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Actualizar convocatoria",
            description = "Actualiza el contenido o fechas de una convocatoria en borrador sin publicar (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria actualizada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos o fechas inválidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria o evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ConvocatoriaRequest request) {
        return ResponseEntity.ok(convocatoriaService.actualizar(id, request));
    }

    @Operation(summary = "Publicar convocatoria",
            description = "Cambia el estado de una convocatoria de 'borrador' a 'publicada', permitiendo la recepción de propuestas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria publicada exitosamente",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "400", description = "La convocatoria no está en estado borrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{id}/publicar")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConvocatoriaResponse> publicar(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.publicar(id));
    }

    @Operation(summary = "Eliminar convocatoria",
            description = "Elimina una convocatoria existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria eliminada"),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        convocatoriaService.eliminar(id);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Convocatoria eliminada exitosamente.",
                "convocatoriaId", String.valueOf(id)
        ));
    }

    @Operation(summary = "Validar disponibilidad de envío de propuestas",
            description = "Valida si la convocatoria permite el envío de propuestas. Si la fecha de cierre ya transcurrió o no está abierta, responde 400 (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Periodo de recepción abierto"),
            @ApiResponse(responseCode = "400", description = "Fecha de cierre cumplida o convocatoria no publicada (CONVOCATORIA_CERRADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/validar-envio")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'PROPUESTAS_VER', 'EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> validarEnvio(@PathVariable Long id) {
        convocatoriaService.validarRecepcionPropuestas(id);
        return ResponseEntity.ok(Map.of(
                "valido", true,
                "mensaje", "La convocatoria se encuentra abierta y disponible para el envío de propuestas."
        ));
    }
}
