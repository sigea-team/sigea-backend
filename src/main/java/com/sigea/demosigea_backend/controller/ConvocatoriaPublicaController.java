package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.service.ConvocatoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta pública de convocatorias (HU-11, Criterio 1).
 * <p>
 * No requiere autenticación: expone únicamente las convocatorias en estado {@code publicada}, que son
 * las visibles para autores y usuarios externos. Las convocatorias en borrador o cerradas no aparecen.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see ConvocatoriaService#listarPublicadas(Long)
 */
@RestController
@RequestMapping("/api/v1/publico/convocatorias")
@RequiredArgsConstructor
@Tag(name = "Convocatorias (público)", description = "Consulta pública de convocatorias publicadas, sin autenticación")
public class ConvocatoriaPublicaController {

    private final ConvocatoriaService convocatoriaService;

    @Operation(summary = "Listar convocatorias publicadas",
            description = "Devuelve las convocatorias publicadas, visibles para usuarios externos. Permite filtrar por evento.")
    @ApiResponse(responseCode = "200", description = "Listado de convocatorias publicadas",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = ConvocatoriaResponse.class))))
    @GetMapping
    public ResponseEntity<List<ConvocatoriaResponse>> listarPublicadas(
            @Parameter(description = "ID del evento para filtrar", example = "1")
            @RequestParam(required = false) Long eventoId
    ) {
        return ResponseEntity.ok(convocatoriaService.listarPublicadas(eventoId));
    }

    @Operation(summary = "Consultar convocatoria publicada",
            description = "Obtiene el detalle de una convocatoria publicada. Si no existe o no está publicada responde 404.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Convocatoria publicada",
                    content = @Content(schema = @Schema(implementation = ConvocatoriaResponse.class))),
            @ApiResponse(responseCode = "404", description = "No existe o no está publicada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConvocatoriaResponse> obtenerPublicada(@PathVariable Long id) {
        return ResponseEntity.ok(convocatoriaService.obtenerPublicada(id));
    }
}
