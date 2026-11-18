package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.periodo.ComparacionSemestresResponse;
import com.sigea.demosigea_backend.dto.periodo.ConsultaPeriodoResponse;
import com.sigea.demosigea_backend.dto.periodo.FiltroPeriodo;
import com.sigea.demosigea_backend.dto.periodo.IndicadoresPeriodoResponse;
import com.sigea.demosigea_backend.service.PeriodoAcademicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST de consultas por periodo académico (HU-27, Criterios 3, 4 y 5): eventos e indicadores
 * filtrados por semestre, rango de fechas, tipo de evento o tipo de actividad, y comparación entre semestres.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see PeriodoAcademicoService
 */
@RestController
@RequestMapping("/api/v1/periodos-academicos")
@RequiredArgsConstructor
@Tag(name = "Periodos académicos", description = "Eventos e indicadores por semestre y comparación entre semestres (HU-27)")
@SecurityRequirement(name = "bearerAuth")
public class PeriodoAcademicoController {

    private final PeriodoAcademicoService periodoService;

    @Operation(summary = "Semestres con eventos registrados",
            description = "Lista para poblar los filtros, del semestre más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Semestres",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping("/semestres")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<String>> listarSemestres() {
        return ResponseEntity.ok(periodoService.listarSemestres());
    }

    @Operation(summary = "Eventos por periodo",
            description = "Criterio 3: eventos y actividades que cumplen los filtros. Criterio 4: si no hay eventos, "
                    + "responde 200 con hayInformacion=false y un mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eventos del periodo",
                    content = @Content(schema = @Schema(implementation = ConsultaPeriodoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Semestre con formato inválido o rango invertido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ConsultaPeriodoResponse> consultarEventos(
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
        return ResponseEntity.ok(periodoService.consultarEventos(
                new FiltroPeriodo(semestre, fechaDesde, fechaHasta, tipoEventoId, tipoActividad)));
    }

    @Operation(summary = "Indicadores por periodo",
            description = "Criterio 3: indicadores (eventos, actividades, inscripciones, participantes, "
                    + "conferencistas, ponentes y propuestas) de los eventos que cumplen los filtros. Criterio 4: si "
                    + "no hay eventos, responde 200 con hayInformacion=false y un mensaje.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores del periodo",
                    content = @Content(schema = @Schema(implementation = IndicadoresPeriodoResponse.class))),
            @ApiResponse(responseCode = "400", description = "Semestre con formato inválido o rango invertido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/indicadores")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<IndicadoresPeriodoResponse> calcularIndicadores(
            @RequestParam(required = false) String semestre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long tipoEventoId,
            @RequestParam(required = false) String tipoActividad) {
        return ResponseEntity.ok(periodoService.calcularIndicadores(
                new FiltroPeriodo(semestre, fechaDesde, fechaHasta, tipoEventoId, tipoActividad)));
    }

    @Operation(summary = "Comparar indicadores de dos semestres",
            description = "Criterio 5: indicadores de ambos semestres y la variación de cada uno (diferencia, "
                    + "porcentaje y tendencia). Los filtros de tipo de evento y de actividad aplican a ambos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comparación",
                    content = @Content(schema = @Schema(implementation = ComparacionSemestresResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falta un semestre, formato inválido o semestres iguales",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/comparacion")
    @PreAuthorize("hasAnyAuthority('EVENTOS_VER', 'REPORTES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<ComparacionSemestresResponse> comparar(
            @Parameter(description = "Semestre base", example = "2025-2", required = true)
            @RequestParam(required = false) String semestreA,
            @Parameter(description = "Semestre a comparar", example = "2026-1", required = true)
            @RequestParam(required = false) String semestreB,
            @RequestParam(required = false) Long tipoEventoId,
            @RequestParam(required = false) String tipoActividad) {
        return ResponseEntity.ok(periodoService.comparar(semestreA, semestreB, tipoEventoId, tipoActividad));
    }
}
