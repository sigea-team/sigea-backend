package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.historial.HistorialParticipacionResponse;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.service.HistorialParticipacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST del historial de participación de una persona (HU-27, Criterios 1 a 3).
 * <p>
 * Consulta los eventos, actividades y roles (participante, ponente, conferencista, autor) de una persona, con
 * filtros opcionales por semestre, rango de fechas, tipo de evento y tipo de actividad.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see HistorialParticipacionService
 */
@RestController
@RequestMapping("/api/v1/historial-participacion")
@RequiredArgsConstructor
@Tag(name = "Historial de participación", description = "Trayectoria de una persona en los eventos del programa (HU-27)")
@SecurityRequirement(name = "bearerAuth")
public class HistorialParticipacionController {

    private final HistorialParticipacionService historialService;

    @Operation(summary = "Historial de participación de una persona",
            description = "Criterio 1: eventos, actividades y roles en que participó la persona. Criterio 2: si no "
                    + "tiene participación registrada, responde 200 con primeraParticipacion=true y un mensaje. "
                    + "Criterio 3: filtros opcionales por semestre, rango de fechas, tipo de evento y tipo de actividad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de la persona",
                    content = @Content(schema = @Schema(implementation = HistorialParticipacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Semestre con formato inválido o rango de fechas invertido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/personas/{personaId}")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<HistorialParticipacionResponse> consultar(
            @Parameter(description = "ID de la persona", example = "12") @PathVariable Long personaId,
            @Parameter(description = "Semestre académico", example = "2026-1")
            @RequestParam(required = false) String semestre,
            @Parameter(description = "Inicio del rango (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @Parameter(description = "Fin del rango (yyyy-MM-dd)", example = "2026-06-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @Parameter(description = "ID del tipo de evento", example = "2")
            @RequestParam(required = false) Long tipoEventoId,
            @Parameter(description = "Nombre del tipo de actividad", example = "Taller")
            @RequestParam(required = false) String tipoActividad) {
        FiltroPeriodo filtro = new FiltroPeriodo(semestre, fechaDesde, fechaHasta, tipoEventoId, tipoActividad);
        return ResponseEntity.ok(historialService.consultar(personaId, filtro));
    }

    @Operation(summary = "Historial de participación buscando a la persona por documento o correo",
            description = "Igual que la consulta por id, pero localiza a la persona por número de documento "
                    + "(prioritario) o por correo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial de la persona",
                    content = @Content(schema = @Schema(implementation = HistorialParticipacionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falta documento y correo, o filtros inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Persona no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/personas")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<HistorialParticipacionResponse> consultarPorIdentificacion(
            @Parameter(description = "Número de documento", example = "1090123456")
            @RequestParam(required = false) String documento,
            @Parameter(description = "Correo electrónico", example = "persona@ufps.edu.co")
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long tipoEventoId,
            @RequestParam(required = false) String tipoActividad) {
        FiltroPeriodo filtro = new FiltroPeriodo(semestre, fechaDesde, fechaHasta, tipoEventoId, tipoActividad);
        return ResponseEntity.ok(historialService.consultarPorIdentificacion(documento, correo, filtro));
    }
}
