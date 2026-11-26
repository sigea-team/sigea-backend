package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.auth.ErrorResponse;
import com.sigea.demosigea_backend.dto.certificado.OrdenFirmantesRequest;
import com.sigea.demosigea_backend.dto.certificado.PlantillaCertificadoResponse;
import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.service.PlantillaCertificadoService;
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
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Plantilla de los certificados del evento (HU-26): imagen de fondo y firmantes (nombre, cargo e imagen de la firma).
 * Solo se modifica mientras no haya certificados emitidos en el evento.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/certificados/plantilla")
@RequiredArgsConstructor
@Tag(name = "Plantilla de certificados", description = "HU-26: fondo y firmas de los certificados del evento")
@SecurityRequirement(name = "bearerAuth")
public class PlantillaCertificadoController {

    private final PlantillaCertificadoService plantillaService;

    @Operation(summary = "Consultar la plantilla", description = "Fondo, firmantes y si la plantilla se puede modificar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla del evento"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> consultar(@PathVariable Long eventoId) {
        return ResponseEntity.ok(plantillaService.consultar(eventoId));
    }

    @Operation(summary = "Vista previa en PDF",
            description = "Certificado de muestra con la plantilla actual y datos de ejemplo.")
    @ApiResponse(responseCode = "200", description = "PDF de muestra",
            content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE))
    @GetMapping(value = "/vista-previa", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<byte[]> vistaPrevia(
            @PathVariable Long eventoId,
            @Parameter(description = "Rol de ejemplo: asistente, ponente o conferencista")
            @RequestParam(required = false) TipoCertificado tipo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("vista-previa-certificado.pdf").build().toString())
                .body(plantillaService.vistaPrevia(eventoId, tipo));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Fondo
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Subir o reemplazar la imagen de fondo",
            description = "PNG o JPG horizontal, máximo 5 MB y al menos 1000 px de ancho. Se recorta al centro a la "
                    + "proporción de una carta horizontal. Con fondo no se dibujan el marco ni el encabezado "
                    + "institucional predeterminados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "400", description = "Imagen inválida (ARCHIVO_INVALIDO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ya hay certificados emitidos "
                    + "(PLANTILLA_CERTIFICADO_BLOQUEADA) o el evento está archivado (EVENTO_ARCHIVADO)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/fondo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> subirFondo(@PathVariable Long eventoId,
                                                                   @RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(plantillaService.subirFondo(eventoId, bytes(archivo, "fondo")));
    }

    @Operation(summary = "Quitar la imagen de fondo", description = "Los certificados vuelven al diseño predeterminado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "409", description = "Ya hay certificados emitidos o el evento está archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/fondo")
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> eliminarFondo(@PathVariable Long eventoId) {
        return ResponseEntity.ok(plantillaService.eliminarFondo(eventoId));
    }

    @Operation(summary = "Imagen de fondo", description = "JPEG normalizado (2200 × 1700 px).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen", content = @Content(mediaType = MediaType.IMAGE_JPEG_VALUE)),
            @ApiResponse(responseCode = "404", description = "El evento no tiene fondo",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/fondo", produces = MediaType.IMAGE_JPEG_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<byte[]> imagenFondo(@PathVariable Long eventoId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).cacheControl(CacheControl.noStore())
                .body(plantillaService.imagenFondo(eventoId));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Firmantes
    // ──────────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Agregar un firmante",
            description = "Nombre, cargo e imagen de la firma (PNG o JPG, máximo 2 MB; si no tiene transparencia, el "
                    + "blanco se vuelve transparente). Se agrega al final del pie; máximo 4.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos o imagen inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Máximo de firmantes (FIRMANTES_CERTIFICADO_MAXIMO), "
                    + "certificados ya emitidos o evento archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/firmantes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> agregarFirmante(
            @PathVariable Long eventoId,
            @RequestParam("nombre") String nombre,
            @RequestParam("cargo") String cargo,
            @RequestPart("firma") MultipartFile firma) {
        return ResponseEntity.ok(plantillaService.agregarFirmante(eventoId, nombre, cargo, bytes(firma, "firma")));
    }

    @Operation(summary = "Modificar un firmante", description = "Nombre y cargo; la imagen de la firma es opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "404", description = "Firmante no encontrado en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Certificados ya emitidos o evento archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/firmantes/{firmanteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> actualizarFirmante(
            @PathVariable Long eventoId,
            @PathVariable Long firmanteId,
            @RequestParam("nombre") String nombre,
            @RequestParam("cargo") String cargo,
            @RequestPart(value = "firma", required = false) MultipartFile firma) {
        byte[] imagen = firma == null || firma.isEmpty() ? null : bytes(firma, "firma");
        return ResponseEntity.ok(plantillaService.actualizarFirmante(eventoId, firmanteId, nombre, cargo, imagen));
    }

    @Operation(summary = "Quitar un firmante", description = "Los demás se corren a la izquierda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "404", description = "Firmante no encontrado en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Certificados ya emitidos o evento archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/firmantes/{firmanteId}")
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> eliminarFirmante(@PathVariable Long eventoId,
                                                                         @PathVariable Long firmanteId) {
        return ResponseEntity.ok(plantillaService.eliminarFirmante(eventoId, firmanteId));
    }

    @Operation(summary = "Ordenar los firmantes", description = "IDs de todos los firmantes, de izquierda a derecha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plantilla actualizada"),
            @ApiResponse(responseCode = "400", description = "La lista no incluye exactamente a todos los firmantes",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Certificados ya emitidos o evento archivado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/firmantes/orden")
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<PlantillaCertificadoResponse> reordenar(@PathVariable Long eventoId,
                                                                  @Valid @RequestBody OrdenFirmantesRequest request) {
        return ResponseEntity.ok(plantillaService.reordenarFirmantes(eventoId, request.firmanteIds()));
    }

    @Operation(summary = "Imagen de la firma", description = "PNG normalizado con transparencia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen", content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "404", description = "Firmante no encontrado en el evento",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/firmantes/{firmanteId}/firma", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize(CertificadoController.PERMISO_CONFIGURACION)
    public ResponseEntity<byte[]> imagenFirma(@PathVariable Long eventoId, @PathVariable Long firmanteId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .body(plantillaService.imagenFirma(eventoId, firmanteId));
    }

    private static byte[] bytes(MultipartFile archivo, String campo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjunte la imagen (PNG o JPG).", Map.of(campo, "Adjunte la imagen."));
        }
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado.", Map.of(campo, "No se pudo leer."));
        }
    }
}
