package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.asistencia.CodigoQrResponse;
import com.sigea.demosigea_backend.dto.asistencia.ReenvioQrResponse;
import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.service.CodigoQrService;
import com.sigea.demosigea_backend.service.GeneradorImagenQr;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Código QR de las inscripciones al evento (HU-24, RF44): consulta, imagen y reenvío.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/inscripciones")
@RequiredArgsConstructor
@Tag(name = "Código QR de inscripción", description = "HU-24: código QR único por inscripción, su imagen y su reenvío")
@SecurityRequirement(name = "bearerAuth")
public class CodigoQrController {

    static final String PERMISO_GESTION =
            "hasAnyAuthority('ASISTENCIA_REGISTRAR', 'INSCRIPCIONES_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')";

    private final CodigoQrService codigoQrService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Participante
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Consultar mi código QR",
            description = "Código QR único de la inscripción del usuario autenticado (Criterio 1). Es el mismo para todas "
                    + "las actividades del evento a las que esté inscrito.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código QR de la inscripción"),
            @ApiResponse(responseCode = "404", description = "El usuario no está inscrito en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/mi-inscripcion/qr")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CodigoQrResponse> consultarMiCodigo(@PathVariable Long eventoId) {
        return ResponseEntity.ok(codigoQrService.consultarMiCodigo(eventoId));
    }

    @Operation(summary = "Imagen PNG de mi código QR",
            description = "Imagen del código QR de la inscripción del usuario autenticado, lista para mostrar o descargar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen PNG", content = @Content(mediaType = "image/png")),
            @ApiResponse(responseCode = "404", description = "El usuario no está inscrito en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/mi-inscripcion/qr/imagen", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> imagenMiCodigo(
            @PathVariable Long eventoId,
            @Parameter(description = "Ancho y alto en píxeles (120 a 1024)")
            @RequestParam(defaultValue = "" + GeneradorImagenQr.TAMANO_POR_DEFECTO) int tamano) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noStore())
                .header("Content-Disposition", "inline; filename=\"codigo-qr-evento-" + eventoId + ".png\"")
                .body(codigoQrService.imagenMiCodigo(eventoId, tamano));
    }

    @Operation(summary = "Reenviar mi código QR",
            description = "Reenvía por correo el código QR de la inscripción del usuario autenticado. No genera un código "
                    + "distinto (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado del reenvío (ver campo enviado)"),
            @ApiResponse(responseCode = "404", description = "El usuario no está inscrito en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El participante no tiene correo registrado (OPERACION_NO_PERMITIDA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/mi-inscripcion/qr/reenviar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReenvioQrResponse> reenviarMiCodigo(@PathVariable Long eventoId) {
        return ResponseEntity.ok(codigoQrService.reenviarMiCodigo(eventoId));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Organizador
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Consultar el código QR de una inscripción",
            description = "Código QR de una inscripción del evento (organizador).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código QR de la inscripción"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{inscripcionId}/qr")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<CodigoQrResponse> consultarCodigo(@PathVariable Long eventoId, @PathVariable Long inscripcionId) {
        return ResponseEntity.ok(codigoQrService.consultarCodigo(eventoId, inscripcionId));
    }

    @Operation(summary = "Reenviar el código QR de una inscripción",
            description = "El organizador reenvía al participante su mismo código QR (Criterio 4).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado del reenvío (ver campo enviado)"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Inscripción no encontrada en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El participante no tiene correo registrado (OPERACION_NO_PERMITIDA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{inscripcionId}/qr/reenviar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<ReenvioQrResponse> reenviarCodigo(@PathVariable Long eventoId, @PathVariable Long inscripcionId) {
        return ResponseEntity.ok(codigoQrService.reenviarCodigo(eventoId, inscripcionId));
    }
}
