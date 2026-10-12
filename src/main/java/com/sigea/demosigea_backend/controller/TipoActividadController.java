package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.parametro.TipoActividadRequest;
import com.sigea.demosigea_backend.dto.parametro.TipoActividadResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.service.TipoActividadService;
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
 * Controlador REST del catálogo de tipos de actividad de un evento (HU-05).
 * <p>
 * Sub-recurso del evento: {@code /api/v1/eventos/{eventoId}/tipos-actividad}. Usa los permisos del módulo
 * EVENTOS ya sembrados en el catálogo (changeset 003).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see TipoActividadService
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/tipos-actividad")
@RequiredArgsConstructor
@Tag(name = "Tipos de Actividad", description = "Catálogo de tipos de actividad del evento (HU-05)")
@SecurityRequirement(name = "bearerAuth")
public class TipoActividadController {

    private final TipoActividadService tipoService;

    @Operation(summary = "Listar tipos de actividad del evento", description = "Devuelve el catálogo ordenado por nombre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catálogo del evento",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TipoActividadResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<TipoActividadResponse>> listar(
            @Parameter(description = "ID del evento", example = "1") @PathVariable Long eventoId
    ) {
        return ResponseEntity.ok(tipoService.listar(eventoId));
    }

    @Operation(summary = "Consultar un elemento del catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elemento encontrado",
                    content = @Content(schema = @Schema(implementation = TipoActividadResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o elemento no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{tipoId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoActividadResponse> obtener(@PathVariable Long eventoId, @PathVariable Long tipoId) {
        return ResponseEntity.ok(tipoService.obtener(eventoId, tipoId));
    }

    @Operation(summary = "Consultar el uso (impacto de eliminar)",
            description = "Indica cuántas actividades y propuestas usan el elemento y si puede eliminarse, "
                    + "para que el frontend advierta del impacto antes de borrar (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uso del elemento",
                    content = @Content(schema = @Schema(implementation = UsoParametroResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o elemento no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{tipoId}/uso")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<UsoParametroResponse> consultarUso(@PathVariable Long eventoId, @PathVariable Long tipoId) {
        return ResponseEntity.ok(tipoService.consultarUso(eventoId, tipoId));
    }

    @Operation(summary = "Crear elemento del catálogo",
            description = "Registra un nuevo elemento con nombre y características (Criterio 1). Si el nombre ya existe "
                    + "en el evento, sin distinguir mayúsculas ni espacios → 409 PARAMETRO_DUPLICADO (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Elemento creado",
                    content = @Content(schema = @Schema(implementation = TipoActividadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o evento en ejecución/cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoActividadResponse> crear(
            @PathVariable Long eventoId,
            @Valid @RequestBody TipoActividadRequest request
    ) {
        TipoActividadResponse creado = tipoService.crear(eventoId, request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Actualizar elemento del catálogo",
            description = "Modifica nombre y características. El nombre nuevo no puede repetir el de otro elemento del evento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Elemento actualizado",
                    content = @Content(schema = @Schema(implementation = TipoActividadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Datos incompletos o inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento o elemento no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Nombre duplicado o evento en ejecución/cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{tipoId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<TipoActividadResponse> actualizar(
            @PathVariable Long eventoId,
            @PathVariable Long tipoId,
            @Valid @RequestBody TipoActividadRequest request
    ) {
        return ResponseEntity.ok(tipoService.actualizar(eventoId, tipoId, request));
    }

    @Operation(summary = "Eliminar elemento del catálogo",
            description = "Elimina el elemento si no está en uso. Si lo usan actividades o propuestas → 409 "
                    + "PARAMETRO_EN_USO con el impacto en el mensaje (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Elemento eliminado"),
            @ApiResponse(responseCode = "404", description = "Evento o elemento no encontrados",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Elemento en uso o evento en ejecución/cerrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{tipoId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long eventoId, @PathVariable Long tipoId) {
        tipoService.eliminar(eventoId, tipoId);
        return ResponseEntity.noContent().build();
    }
}
