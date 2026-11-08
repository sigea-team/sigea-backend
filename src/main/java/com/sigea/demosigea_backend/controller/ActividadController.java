package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.actividad.ActividadRequest;
import com.sigea.demosigea_backend.dto.actividad.ActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ProgramacionRechazadaResponse;
import com.sigea.demosigea_backend.dto.actividad.ValidacionActividadResponse;
import com.sigea.demosigea_backend.dto.agenda.ActividadPorProgramarRequest;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.ActividadService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import java.time.LocalDate;
import java.util.List;

/**
 * Programación de las actividades de la agenda de un evento (HU-21) y registro de actividades por programar para la
 * generación automática (HU-22).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/actividades")
@RequiredArgsConstructor
@Tag(name = "Agenda de actividades", description = "HU-21: programar actividades controlando conflictos y simultaneidad")
@SecurityRequirement(name = "bearerAuth")
public class ActividadController {

    private static final String GESTIONAR = "hasAnyAuthority('AGENDA_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";
    private static final String CONSULTAR = "hasAnyAuthority('EVENTOS_VER', 'AGENDA_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    private final ActividadService actividadService;

    @Operation(summary = "Agenda del evento",
            description = "Criterio 1: actividades del evento en orden cronológico. Filtra por fecha si se indica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda obtenida."),
            @ApiResponse(responseCode = "404", description = "Evento inexistente.")
    })
    @GetMapping
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<List<ActividadResponse>> listar(
            @PathVariable Long eventoId,
            @Parameter(description = "Fecha a consultar (AAAA-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(actividadService.listar(eventoId, fecha));
    }

    @Operation(summary = "Detalle de una actividad")
    @GetMapping("/{actividadId}")
    @PreAuthorize(CONSULTAR)
    public ResponseEntity<ActividadResponse> obtener(@PathVariable Long eventoId, @PathVariable Long actividadId) {
        return ResponseEntity.ok(actividadService.obtener(eventoId, actividadId));
    }

    @Operation(summary = "Revisar una actividad sin guardarla",
            description = "Criterio 2: devuelve los cruces de horario, sala o ponente y las advertencias de "
                    + "disponibilidad antes de confirmar. Para revisar una edición, envía actividadId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Revisión hecha (puede traer conflictos)."),
            @ApiResponse(responseCode = "400", description = "Faltan datos obligatorios o son inválidos (Criterio 4).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/validar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ValidacionActividadResponse> validar(
            @PathVariable Long eventoId,
            @Parameter(description = "ID de la actividad que se edita (se excluye de la revisión)")
            @RequestParam(required = false) Long actividadId,
            @Valid @RequestBody ActividadRequest request) {
        return ResponseEntity.ok(actividadService.validar(eventoId, actividadId, request));
    }

    @Operation(summary = "Programar una actividad",
            description = "Criterios 1 a 4. Responde 409 CONFLICTO_PROGRAMACION si hay cruces, o 409 "
                    + "CONFIRMACION_REQUERIDA si solo hay advertencias de disponibilidad (reenviar con "
                    + "confirmarAdvertencias = true).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Actividad programada."),
            @ApiResponse(responseCode = "400", description = "Faltan fecha, hora o sala, o los datos son inválidos (Criterio 4).",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, sala, tipo de actividad u otra referencia inexistente."),
            @ApiResponse(responseCode = "409", description = "Cruces con la agenda o advertencias sin confirmar (Criterios 2 y 3).",
                    content = @Content(schema = @Schema(implementation = ProgramacionRechazadaResponse.class)))
    })
    @PostMapping
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ActividadResponse> crear(@PathVariable Long eventoId,
                                                   @Valid @RequestBody ActividadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actividadService.crear(eventoId, request));
    }

    @Operation(summary = "Modificar una actividad",
            description = "Criterio 5: al cambiar horario, sala o ponente se revalidan los cruces con el resto de la agenda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad actualizada."),
            @ApiResponse(responseCode = "409", description = "El cambio genera cruces o advertencias sin confirmar.",
                    content = @Content(schema = @Schema(implementation = ProgramacionRechazadaResponse.class)))
    })
    @PutMapping("/{actividadId}")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ActividadResponse> actualizar(@PathVariable Long eventoId, @PathVariable Long actividadId,
                                                        @Valid @RequestBody ActividadRequest request) {
        return ResponseEntity.ok(actividadService.actualizar(eventoId, actividadId, request));
    }

    @Operation(summary = "Registrar una actividad por programar",
            description = "HU-22 Criterio 1: registra la actividad con su duración, sin fecha, horario ni sala, para que "
                    + "la generación automática de la agenda la ubique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Actividad registrada en estado por_programar."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento, tipo de actividad u otra referencia inexistente.")
    })
    @PostMapping("/por-programar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ActividadResponse> registrarPorProgramar(@PathVariable Long eventoId,
                                                                   @Valid @RequestBody ActividadPorProgramarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actividadService.registrarPorProgramar(eventoId, request));
    }

    @Operation(summary = "Modificar una actividad por programar",
            description = "HU-22: cambia nombre, duración, ponente u otros datos de una actividad que aún no tiene horario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad actualizada."),
            @ApiResponse(responseCode = "409", description = "La actividad ya tiene horario y sala.")
    })
    @PutMapping("/{actividadId}/por-programar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ActividadResponse> actualizarPorProgramar(@PathVariable Long eventoId,
                                                                    @PathVariable Long actividadId,
                                                                    @Valid @RequestBody ActividadPorProgramarRequest request) {
        return ResponseEntity.ok(actividadService.actualizarPorProgramar(eventoId, actividadId, request));
    }

    @Operation(summary = "Devolver una actividad a por programar",
            description = "HU-22 Criterio 1 (ajuste manual): quita fecha, horario y sala. Si la agenda ya estaba "
                    + "publicada, avisa a los inscritos (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actividad por programar."),
            @ApiResponse(responseCode = "409", description = "El evento está cerrado o la actividad está cancelada.")
    })
    @PostMapping("/{actividadId}/desprogramar")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<ActividadResponse> desprogramar(@PathVariable Long eventoId, @PathVariable Long actividadId) {
        return ResponseEntity.ok(actividadService.desprogramar(eventoId, actividadId));
    }

    @Operation(summary = "Eliminar una actividad")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Actividad eliminada."),
            @ApiResponse(responseCode = "409", description = "La actividad tiene registros asociados o el evento está cerrado.")
    })
    @DeleteMapping("/{actividadId}")
    @PreAuthorize(GESTIONAR)
    public ResponseEntity<Void> eliminar(@PathVariable Long eventoId, @PathVariable Long actividadId) {
        actividadService.eliminar(eventoId, actividadId);
        return ResponseEntity.noContent().build();
    }
}
