package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.service.IndicadorEventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Indicadores y dashboard de un evento (HU-29).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Indicadores del evento", description = "HU-29: indicadores de participación y dashboard de un evento")
@SecurityRequirement(name = "bearerAuth")
public class IndicadorEventoController {

    /** Administrador/organizador: quien ve reportes o edita eventos. */
    static final String PERMISO_INDICADORES =
            "hasAnyAuthority('REPORTES_VER', 'EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')";

    private final IndicadorEventoService indicadorEventoService;

    @Operation(summary = "Generar los indicadores y el dashboard del evento",
            description = "Calcula en este momento los indicadores del evento (inscripciones, asistencia, actividades, "
                    + "propuestas, satisfacción y presupuesto) para la tabla y las series para las gráficas "
                    + "(Criterio 1). Lo que no tiene datos suficientes llega con disponible=false o hayDatos=false "
                    + "y su motivo, sin error (Criterio 2). Nada se guarda: cada llamada refleja la información más "
                    + "reciente, así que refrescar el dashboard es volver a llamar (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/indicadores")
    @PreAuthorize(PERMISO_INDICADORES)
    public ResponseEntity<DashboardIndicadoresResponse> generar(@PathVariable Long eventoId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(indicadorEventoService.generar(eventoId));
    }
}
