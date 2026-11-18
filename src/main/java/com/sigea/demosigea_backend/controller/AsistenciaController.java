package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asistencia.AsistenciaRechazadaResponse;
import com.sigea.demosigea_backend.dto.asistencia.AsistenciaResponse;
import com.sigea.demosigea_backend.dto.asistencia.AsistenciasActividadResponse;
import com.sigea.demosigea_backend.dto.asistencia.RegistrarAsistenciaManualRequest;
import com.sigea.demosigea_backend.dto.asistencia.RegistrarAsistenciaQrRequest;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.AsistenciaService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de asistencia a las actividades con el código QR de la inscripción (HU-24, RF45).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/actividades/{actividadId}/asistencias")
@RequiredArgsConstructor
@Tag(name = "Asistencia", description = "HU-24: registro de asistencia a actividades por QR o manual")
@SecurityRequirement(name = "bearerAuth")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    @Operation(summary = "Registrar asistencia con el código QR",
            description = "Valida el código leído del QR y registra la asistencia con fecha y hora (Criterio 2). Rechaza "
                    + "el QR de otro evento, de quien no está inscrito en la actividad o una asistencia ya registrada (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asistencia registrada"),
            @ApiResponse(responseCode = "400", description = "Código vacío o demasiado largo (VALIDACION_FALLIDA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Actividad no encontrada (RECURSO_NO_ENCONTRADO) o QR desconocido (QR_INVALIDO)",
                    content = @Content(schema = @Schema(implementation = AsistenciaRechazadaResponse.class))),
            @ApiResponse(responseCode = "409", description = "QR_NO_VIGENTE, QR_OTRO_EVENTO, NO_INSCRITO_ACTIVIDAD, "
                    + "ACTIVIDAD_NO_DISPONIBLE, EVENTO_NO_DISPONIBLE o ASISTENCIA_DUPLICADA (con asistenciaExistente)",
                    content = @Content(schema = @Schema(implementation = AsistenciaRechazadaResponse.class)))
    })
    @PostMapping
    @PreAuthorize(CodigoQrController.PERMISO_GESTION)
    public ResponseEntity<AsistenciaResponse> registrarPorQr(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId,
            @Valid @RequestBody RegistrarAsistenciaQrRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.registrarPorQr(eventoId, actividadId, request.codigo()));
    }

    @Operation(summary = "Registrar asistencia manual",
            description = "Registra la asistencia de una inscripción sin leer el QR (por ejemplo, el participante no lo tiene "
                    + "a mano). Aplica las mismas validaciones que el registro por QR.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asistencia registrada"),
            @ApiResponse(responseCode = "400", description = "Falta la inscripción (VALIDACION_FALLIDA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Actividad o inscripción no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "NO_INSCRITO_ACTIVIDAD, ACTIVIDAD_NO_DISPONIBLE, "
                    + "EVENTO_NO_DISPONIBLE o ASISTENCIA_DUPLICADA",
                    content = @Content(schema = @Schema(implementation = AsistenciaRechazadaResponse.class)))
    })
    @PostMapping("/manual")
    @PreAuthorize(CodigoQrController.PERMISO_GESTION)
    public ResponseEntity<AsistenciaResponse> registrarManual(
            @PathVariable Long eventoId,
            @PathVariable Long actividadId,
            @Valid @RequestBody RegistrarAsistenciaManualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(asistenciaService.registrarManual(eventoId, actividadId, request.inscripcionId()));
    }

    @Operation(summary = "Listar asistencias de una actividad",
            description = "Asistencias registradas en la actividad, en orden de registro, con el total de inscritos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asistencias de la actividad"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Actividad no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize(CodigoQrController.PERMISO_GESTION)
    public ResponseEntity<AsistenciasActividadResponse> listar(@PathVariable Long eventoId, @PathVariable Long actividadId) {
        return ResponseEntity.ok(asistenciaService.listarPorActividad(eventoId, actividadId));
    }
}
