package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.certificado.CertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.ConfiguracionCertificadosRequest;
import com.sigea.demosigea_backend.dto.certificado.ConfiguracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.EmisionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.GeneracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.HabilitacionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.MisCertificadosEventoResponse;
import com.sigea.demosigea_backend.dto.certificado.SolicitudCertificadoRequest;
import com.sigea.demosigea_backend.service.CertificadoService;
import com.sigea.demosigea_backend.service.CertificadoService.ArchivoCertificado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Certificados de participación (HU-26, RF47, CU-23): el participante consulta, genera y descarga los suyos; el
 * organizador genera los del evento y configura el porcentaje mínimo de asistencia.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Certificados", description = "HU-26: certificados de asistente, ponente y conferencista")
@SecurityRequirement(name = "bearerAuth")
public class CertificadoController {

    static final String PERMISO_GESTION = "hasAnyAuthority('CERTIFICADOS_EMITIR', 'ROLE_ADMIN', 'ADMIN')";
    static final String PERMISO_CONFIGURACION =
            "hasAnyAuthority('CERTIFICADOS_EMITIR', 'EVENTOS_EDITAR', 'ROLE_ADMIN', 'ADMIN')";

    private final CertificadoService certificadoService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Participante
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Mis certificados en un evento",
            description = "Roles del usuario autenticado en el evento (asistente, ponente o conferencista), si cumple los "
                    + "requisitos de cada uno, el motivo si no, y los certificados ya emitidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado de los certificados del participante"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/certificados/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MisCertificadosEventoResponse> consultarMios(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.consultarMios(eventoId));
    }

    @Operation(summary = "Generar mi certificado",
            description = "Genera el certificado del rol indicado si el usuario cumple los requisitos (Criterio 1). Si no "
                    + "los cumple responde 409 CERTIFICADO_REQUISITOS_NO_CUMPLIDOS con el motivo (Criterio 2). Si ya "
                    + "lo tenía, responde 200 con el mismo certificado, sin duplicarlo (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Certificado generado"),
            @ApiResponse(responseCode = "200", description = "El certificado ya existía; se entrega el mismo"),
            @ApiResponse(responseCode = "400", description = "Tipo de certificado inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "No cumple los requisitos (CERTIFICADO_REQUISITOS_NO_CUMPLIDOS), "
                    + "los certificados no están habilitados (CERTIFICADOS_NO_HABILITADOS), el evento no ha terminado "
                    + "(EVENTO_NO_FINALIZADO), el tipo no se emite (CERTIFICADO_TIPO_NO_DISPONIBLE) o el evento está "
                    + "archivado (EVENTO_ARCHIVADO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/certificados/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmisionCertificadoResponse> solicitar(@PathVariable Long eventoId,
                                                                @Valid @RequestBody SolicitudCertificadoRequest request) {
        EmisionCertificadoResponse respuesta = certificadoService.solicitar(eventoId, request.tipo());
        return ResponseEntity.status(respuesta.generadoAhora() ? HttpStatus.CREATED : HttpStatus.OK).body(respuesta);
    }

    @Operation(summary = "Mis certificados",
            description = "Certificados del usuario autenticado en todos los eventos, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Certificados del participante")
    @GetMapping("/certificados/mios")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CertificadoResponse>> listarMios() {
        return ResponseEntity.ok(certificadoService.listarMios());
    }

    @Operation(summary = "Mis certificados por evento",
            description = "Eventos terminados en los que participó el usuario autenticado, con el estado de sus "
                    + "certificados en cada uno: los emitidos y, si no cumple los requisitos, el motivo.")
    @ApiResponse(responseCode = "200", description = "Estado de los certificados del participante por evento")
    @GetMapping("/certificados/mios/eventos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MisCertificadosEventoResponse>> listarMisEventos() {
        return ResponseEntity.ok(certificadoService.listarMisEventos());
    }

    @Operation(summary = "Descargar un certificado en PDF",
            description = "Lo descarga su titular o quien tiene el permiso CERTIFICADOS_EMITIR. Siempre es la misma "
                    + "versión del certificado (Criterio 3).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF del certificado",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "403", description = "El certificado es de otra persona"),
            @ApiResponse(responseCode = "404", description = "Certificado no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/certificados/{certificadoId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> descargar(@PathVariable Long certificadoId) {
        ArchivoCertificado archivo = certificadoService.descargar(certificadoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.nombre()).build().toString())
                .body(archivo.contenido());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Organizador (CU-23)
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Habilitar los certificados del evento",
            description = "Con el evento terminado y la plantilla completa (fondo y al menos una firma), la plantilla "
                    + "queda fija y se generan los certificados de todos los que cumplen los requisitos. Desde ese "
                    + "momento cada participante encuentra el suyo en SIGEA. Antes de habilitar nadie puede generarlos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificados habilitados y generados"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Plantilla incompleta (PLANTILLA_CERTIFICADO_INCOMPLETA), "
                    + "ya habilitados (CERTIFICADOS_YA_HABILITADOS), evento sin terminar (EVENTO_NO_FINALIZADO) o "
                    + "archivado (EVENTO_ARCHIVADO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/certificados/habilitar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<HabilitacionCertificadosResponse> habilitar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.habilitar(eventoId));
    }

    @Operation(summary = "Generar los certificados del evento",
            description = "Con los certificados ya habilitados, genera los que falten de quienes cumplen los requisitos "
                    + "(los que ya existían no se duplican y los que no cumplen se listan con el motivo, CU-23).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de la generación"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento no ha terminado (EVENTO_NO_FINALIZADO) o está "
                    + "archivado (EVENTO_ARCHIVADO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eventos/{eventoId}/certificados/generar")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<GeneracionCertificadosResponse> generarDelEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.generarDelEvento(eventoId));
    }

    @Operation(summary = "Certificados emitidos en el evento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Certificados del evento"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/certificados")
    @PreAuthorize(PERMISO_GESTION)
    public ResponseEntity<List<CertificadoResponse>> listarDelEvento(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.listarDelEvento(eventoId));
    }

    @Operation(summary = "Configuración de certificados del evento",
            description = "Porcentaje mínimo de asistencia para el certificado de asistente (RN-01) y si se puede cambiar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/eventos/{eventoId}/certificados/configuracion")
    @PreAuthorize(PERMISO_CONFIGURACION)
    public ResponseEntity<ConfiguracionCertificadosResponse> consultarConfiguracion(@PathVariable Long eventoId) {
        return ResponseEntity.ok(certificadoService.consultarConfiguracion(eventoId));
    }

    @Operation(summary = "Cambiar el porcentaje mínimo de asistencia",
            description = "No se puede cambiar si ya hay certificados de asistente emitidos "
                    + "(CONFIGURACION_CERTIFICADOS_BLOQUEADA) o si el evento está archivado (EVENTO_ARCHIVADO).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuración actualizada"),
            @ApiResponse(responseCode = "400", description = "Porcentaje inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Configuración bloqueada o evento archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/eventos/{eventoId}/certificados/configuracion")
    @PreAuthorize(PERMISO_CONFIGURACION)
    public ResponseEntity<ConfiguracionCertificadosResponse> actualizarConfiguracion(
            @PathVariable Long eventoId, @Valid @RequestBody ConfiguracionCertificadosRequest request) {
        return ResponseEntity.ok(certificadoService.actualizarConfiguracion(eventoId, request.porcentajeMinimoAsistencia()));
    }
}
