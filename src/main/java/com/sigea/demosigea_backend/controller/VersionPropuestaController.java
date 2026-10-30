package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.propuesta.ActualizarPropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorizacionEdicionResponse;
import com.sigea.demosigea_backend.dto.propuesta.AutorizarEdicionRequest;
import com.sigea.demosigea_backend.dto.propuesta.EnvioPropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.VersionPropuestaResponse;
import com.sigea.demosigea_backend.service.VersionPropuestaService;
import com.sigea.demosigea_backend.service.VersionPropuestaService.ArchivoDescarga;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador REST para el envío de propuestas con documentos y su historial de versiones (HU-14).
 * <p>
 * Los endpoints de envío y de nueva versión reciben {@code multipart/form-data} con dos partes:
 * <ul>
 *   <li>{@code datos}: JSON con la información de la propuesta (Content-Type {@code application/json}).</li>
 *   <li>{@code documentos}: uno o varios archivos.</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Tag(name = "Propuestas - Envío y versiones",
        description = "Envío de propuestas con documentos y trazabilidad de sus versiones (HU-14)")
@RestController
@RequestMapping("/api/v1/propuestas")
@RequiredArgsConstructor
public class VersionPropuestaController {

    private final VersionPropuestaService versionPropuestaService;

    @Operation(summary = "Enviar una propuesta con sus documentos",
            description = "Registra la propuesta en una convocatoria activa, guarda los documentos adjuntos como "
                    + "versión 1 y confirma la recepción (Criterios 1 y 2). Si no se envían autores, el usuario "
                    + "autenticado queda como autor principal.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Propuesta recibida"),
            @ApiResponse(responseCode = "400", description = "Convocatoria cerrada (CONVOCATORIA_CERRADA), archivo con formato "
                    + "o tamaño no permitido o documentos faltantes (ARCHIVO_INVALIDO), o campos obligatorios vacíos (VALIDACION_FALLIDA)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Convocatoria o línea temática no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Convocatoria no publicada o aún sin abrir, o autor duplicado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/envio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnvioPropuestaResponse> enviarPropuesta(
            @Valid @RequestPart("datos") PropuestaRequest datos,
            @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos) {
        EnvioPropuestaResponse respuesta = versionPropuestaService.enviarPropuesta(datos, documentos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Modificar una propuesta creando una nueva versión",
            description = "Aplica los cambios de 'datos' (opcional) y de 'documentos' (opcional) y crea una versión nueva; "
                    + "las anteriores se conservan como evidencia (Criterio 3). Si la propuesta tiene versión definitiva, "
                    + "solo se permite con autorización explícita del administrador (Criterio 5).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Versión creada"),
            @ApiResponse(responseCode = "400", description = "Convocatoria cerrada, archivo inválido, sin documentos o sin cambios",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es autor de la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Propuesta en versión definitiva (PROPUESTA_BLOQUEADA) o en un estado que no admite cambios",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/{id}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EnvioPropuestaResponse> crearNuevaVersion(
            @PathVariable Long id,
            @Valid @RequestPart(value = "datos", required = false) ActualizarPropuestaRequest datos,
            @RequestPart(value = "documentos", required = false) List<MultipartFile> documentos) {
        EnvioPropuestaResponse respuesta = versionPropuestaService.crearNuevaVersion(id, datos, documentos);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(summary = "Consultar el historial de versiones de una propuesta",
            description = "Devuelve todas las versiones con fecha y autor del cambio (Criterio 4). Disponible para el "
                    + "administrador, los evaluadores asignados a la propuesta y sus autores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial obtenido"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/versiones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VersionPropuestaResponse>> listarVersiones(@PathVariable Long id) {
        return ResponseEntity.ok(versionPropuestaService.listarVersiones(id));
    }

    @Operation(summary = "Consultar una versión específica de la propuesta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versión obtenida"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta o versión no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/versiones/{numeroVersion}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VersionPropuestaResponse> obtenerVersion(@PathVariable Long id,
                                                                   @PathVariable Integer numeroVersion) {
        return ResponseEntity.ok(versionPropuestaService.obtenerVersion(id, numeroVersion));
    }

    @Operation(summary = "Descargar un documento de la propuesta",
            description = "Descarga un documento de cualquier versión de la propuesta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Archivo"),
            @ApiResponse(responseCode = "403", description = "Sin acceso a la propuesta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/documentos/{documentoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable Long id, @PathVariable Long documentoId) {
        ArchivoDescarga archivo = versionPropuestaService.descargarDocumento(id, documentoId);

        MediaType tipo;
        try {
            tipo = archivo.tipoContenido() != null
                    ? MediaType.parseMediaType(archivo.tipoContenido())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IllegalArgumentException ex) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }

        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombreOriginal(), StandardCharsets.UTF_8)
                        .build()
                        .toString());
        if (archivo.tamanoBytes() != null) {
            respuesta.contentLength(archivo.tamanoBytes());
        }
        return respuesta.body(archivo.recurso());
    }

    @Operation(summary = "Autorizar la modificación de una propuesta bloqueada",
            description = "Concede una autorización explícita, de un solo uso, para modificar una propuesta que ya tiene "
                    + "versión definitiva (Criterio 5). Solo para el administrador/organizador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autorización concedida"),
            @ApiResponse(responseCode = "400", description = "Motivo vacío",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Propuesta no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "La propuesta fue rechazada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/autorizar-edicion")
    @PreAuthorize("hasAnyAuthority('CONVOCATORIAS_GESTIONAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<AutorizacionEdicionResponse> autorizarEdicion(
            @PathVariable Long id,
            @Valid @RequestBody AutorizarEdicionRequest request) {
        return ResponseEntity.ok(versionPropuestaService.autorizarEdicion(id, request.motivo()));
    }
}
