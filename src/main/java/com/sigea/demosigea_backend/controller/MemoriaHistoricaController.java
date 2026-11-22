package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.memoria.CierreConPendientesResponse;
import com.sigea.demosigea_backend.dto.memoria.CierreFormalRequest;
import com.sigea.demosigea_backend.dto.memoria.EliminacionHistoricaRequest;
import com.sigea.demosigea_backend.dto.memoria.EstadoCierreResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResumenResponse;
import com.sigea.demosigea_backend.service.MemoriaHistoricaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Memoria histórica de los eventos (HU-28, RF55, CU-32): cierre formal, consulta y eliminación con autorización
 * especial.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Memoria histórica",
        description = "HU-28: cierre formal del evento, consulta de la memoria histórica y eliminación con autorización especial")
@SecurityRequirement(name = "bearerAuth")
public class MemoriaHistoricaController {

    static final String PERMISO_CIERRE = "hasAnyAuthority('MEMORIA_HISTORICA_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";
    static final String PERMISO_CONSULTA = "hasAnyAuthority('MEMORIA_HISTORICA_GESTIONAR', 'EVENTOS_VER', "
            + "'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')";
    /** Criterio 3: autorización especial. No basta con ser administrador: hay que tener este permiso. */
    static final String PERMISO_ELIMINACION = "hasAuthority('MEMORIA_HISTORICA_ELIMINAR')";

    private final MemoriaHistoricaService memoriaHistoricaService;

    @Operation(summary = "Revisar el cierre formal",
            description = "Indica si el evento ya está en la memoria histórica, si se puede cerrar formalmente y qué "
                    + "procesos quedan pendientes (evaluaciones sin finalizar, certificados sin generar...).")
    @GetMapping("/eventos/{eventoId}/memoria-historica/cierre")
    @PreAuthorize(PERMISO_CIERRE)
    public ResponseEntity<EstadoCierreResponse> revisarCierre(@PathVariable Long eventoId) {
        return ResponseEntity.ok(memoriaHistoricaService.revisarCierre(eventoId));
    }

    @Operation(summary = "Cerrar formalmente el evento",
            description = "Consolida la información del evento finalizado en la memoria histórica (Criterio 1). Desde "
                    + "ese momento la información del evento es solo consulta. Si hay procesos pendientes se responde "
                    + "409 CIERRE_CON_PENDIENTES; para cerrar de todas formas se envía confirmarPendientes=true.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Memoria histórica consolidada"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "CIERRE_CON_PENDIENTES (con la lista de pendientes), "
                    + "EVENTO_NO_FINALIZADO o MEMORIA_YA_CONSOLIDADA",
                    content = @Content(schema = @Schema(implementation = CierreConPendientesResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/memoria-historica")
    @PreAuthorize(PERMISO_CIERRE)
    public ResponseEntity<MemoriaHistoricaResponse> cerrarFormalmente(
            @PathVariable Long eventoId, @Valid @RequestBody(required = false) CierreFormalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memoriaHistoricaService.cerrarFormalmente(eventoId, request));
    }

    @Operation(summary = "Consultar la memoria histórica de un evento",
            description = "Información consolidada al cierre formal, solo consulta (Criterio 2).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Memoria histórica del evento"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado o MEMORIA_NO_ENCONTRADA (sin cierre formal)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/memoria-historica")
    @PreAuthorize(PERMISO_CONSULTA)
    public ResponseEntity<MemoriaHistoricaResponse> obtener(@PathVariable Long eventoId) {
        return ResponseEntity.ok(memoriaHistoricaService.obtener(eventoId));
    }

    @Operation(summary = "Listar la memoria histórica",
            description = "Eventos cerrados formalmente, del más reciente al más antiguo, con sus indicadores "
                    + "principales. Filtros opcionales: semestre, familia de ediciones (cualquier evento de la "
                    + "familia) y parte del nombre.")
    @GetMapping("/memoria-historica")
    @PreAuthorize(PERMISO_CONSULTA)
    public ResponseEntity<List<MemoriaHistoricaResumenResponse>> listar(
            @Parameter(description = "Semestre, por ejemplo 2026-1") @RequestParam(required = false) String semestre,
            @Parameter(description = "ID de cualquier evento de la familia de ediciones")
            @RequestParam(required = false) Long familiaEventoId,
            @Parameter(description = "Parte del nombre del evento") @RequestParam(required = false) String texto) {
        return ResponseEntity.ok(memoriaHistoricaService.listar(semestre, familiaEventoId, texto));
    }

    @Operation(summary = "Eliminar un evento de la memoria histórica (autorización especial)",
            description = "Criterio 3: elimina un evento cerrado formalmente con toda su información. Exige el permiso "
                    + "MEMORIA_HISTORICA_ELIMINAR (no lo tiene ningún rol por defecto), el nombre exacto del evento y "
                    + "un motivo; todo queda en la auditoría. Las demás eliminaciones de información histórica se "
                    + "rechazan con 409 EVENTO_ARCHIVADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento eliminado"),
            @ApiResponse(responseCode = "400", description = "VALIDACION_FALLIDA o CONFIRMACION_INVALIDA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin el permiso MEMORIA_HISTORICA_ELIMINAR"),
            @ApiResponse(responseCode = "409", description = "MEMORIA_NO_ENCONTRADA o ELIMINACION_NO_PERMITIDA",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/memoria-historica/eliminacion")
    @PreAuthorize(PERMISO_ELIMINACION)
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long eventoId,
                                                        @Valid @RequestBody EliminacionHistoricaRequest request) {
        memoriaHistoricaService.eliminarConAutorizacionEspecial(eventoId, request);
        return ResponseEntity.ok(Map.of("mensaje",
                "El evento y su memoria histórica fueron eliminados. La operación quedó registrada en la auditoría."));
    }
}
