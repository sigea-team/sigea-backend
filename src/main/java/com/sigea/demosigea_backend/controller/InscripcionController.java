package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.inscripcion.CupoActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionActividadResponse;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoRequest;
import com.sigea.demosigea_backend.dto.inscripcion.InscripcionEventoResponse;
import com.sigea.demosigea_backend.dto.inscripcion.MiInscripcionResponse;
import com.sigea.demosigea_backend.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la inscripción de participantes a eventos y a sus actividades (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}")
@RequiredArgsConstructor
@Tag(name = "Inscripciones", description = "HU-23: Inscripción a eventos y actividades con control de aforo y prevención de cruces")
@SecurityRequirement(name = "bearerAuth")
public class InscripcionController {

    private static final String PERMISO_GESTION = "hasAnyAuthority('INSCRIPCIONES_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    private final InscripcionService inscripcionService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Inscripción al Evento
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Inscribirse al evento",
            description = "Registra la inscripción del participante al evento con su categoría seleccionada (Criterio 1 y 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscripción registrada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de inscripción inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Inscripciones no abiertas o ya inscrito previamente (INSCRIPCION_DUPLICADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/inscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InscripcionEventoResponse> inscribirAEvento(
            @PathVariable Long eventoId,
            @Valid @RequestBody InscripcionEventoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inscripcionService.inscribirAEvento(eventoId, request));
    }

    @Operation(summary = "Consultar mi inscripción al evento",
            description = "Retorna el detalle de la inscripción del participante autenticado junto a sus actividades inscritas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de inscripción encontrado"),
            @ApiResponse(responseCode = "404", description = "El participante no se encuentra inscrito en este evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/inscripciones/mi-inscripcion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MiInscripcionResponse> obtenerMiInscripcion(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.obtenerMiInscripcion(eventoId));
    }

    @Operation(summary = "Cancelar mi inscripción al evento",
            description = "Cancela la inscripción al evento y libera en cascada los cupos de todas sus actividades (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscripción cancelada exitosamente"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/inscripciones/mi-inscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void cancelarMiInscripcion(@PathVariable Long eventoId) {
        inscripcionService.cancelarMiInscripcionEvento(eventoId);
    }

    @Operation(summary = "Listar inscripciones al evento",
            description = "Consulta la lista de participantes inscritos en el evento (requiere permisos de administración).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de inscripciones"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/inscripciones")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<List<InscripcionEventoResponse>> listarInscripcionesEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarInscripcionesEvento(eventoId));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Inscripción a Actividades Puntuales
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Inscribirse a una actividad",
            description = "Inscribe al participante en una actividad puntual validando aforo de sala y cruces de horario (Criterio 2, 3 y 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inscripción a actividad registrada"),
            @ApiResponse(responseCode = "404", description = "Actividad o evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Cupo agotado (CUPO_AGOTADO), traslape de horario (CONFLICTO_HORARIO_INSCRIPCION) o ya inscrito (INSCRIPCION_DUPLICADA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/actividades/{actividadId}/inscripciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InscripcionActividadResponse> inscribirAActividad(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inscripcionService.inscribirAActividad(eventoId, actividadId));
    }

    @Operation(summary = "Cancelar inscripción a una actividad",
            description = "Cancela la inscripción a la actividad puntual y libera el cupo inmediatamente (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Inscripción a actividad cancelada"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/actividades/{actividadId}/inscripciones/mi-inscripcion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void cancelarInscripcionActividad(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId) {
        inscripcionService.cancelarInscripcionActividad(eventoId, actividadId);
    }

    @Operation(summary = "Consultar disponibilidad de cupos de una actividad",
            description = "Consulta aforo máximo, cantidad de inscritos y cupos libres restantes (Criterios 2, 4 y 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cupos de la actividad"),
            @ApiResponse(responseCode = "404", description = "Actividad no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/actividades/{actividadId}/cupos")
    public ResponseEntity<CupoActividadResponse> consultarCupo(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId) {
        return ResponseEntity.ok(inscripcionService.consultarCupo(eventoId, actividadId));
    }

    @Operation(summary = "Listar inscritos a una actividad",
            description = "Consulta los participantes inscritos en una actividad específica (requiere permisos de administración).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de inscritos a la actividad"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Actividad no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/actividades/{actividadId}/inscripciones")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<List<InscripcionActividadResponse>> listarInscritosActividad(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId) {
        return ResponseEntity.ok(inscripcionService.listarInscritosActividad(eventoId, actividadId));
    }

    @Operation(summary = "Listar mis actividades inscritas",
            description = "Consulta las actividades del evento a las que el usuario actual se encuentra inscrito.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mis actividades inscritas")
    })
    @GetMapping("/mis-actividades-inscritas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<InscripcionActividadResponse>> listarMisActividades(@PathVariable Long eventoId) {
        return ResponseEntity.ok(inscripcionService.listarMisActividades(eventoId));
    }
}
