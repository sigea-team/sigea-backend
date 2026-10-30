package com.sigea.demosigea_backend.dto.propuesta;

import com.sigea.demosigea_backend.model.DocumentoPropuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Documento adjunto a una versión de propuesta (HU-14).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Documento adjunto a una versión de la propuesta")
public record DocumentoPropuestaResponse(

        @Schema(description = "ID del documento", example = "7")
        Long id,

        @Schema(description = "Nombre original del archivo", example = "ponencia-final.pdf")
        String nombreOriginal,

        @Schema(description = "Tipo de contenido", example = "application/pdf")
        String tipoContenido,

        @Schema(description = "Tamaño en bytes", example = "524288")
        Long tamanoBytes,

        @Schema(description = "Indica si es el documento principal de la versión")
        Boolean principal,

        @Schema(description = "Fecha de carga del archivo")
        LocalDateTime fechaCarga,

        @Schema(description = "Ruta del endpoint de descarga", example = "/api/v1/propuestas/15/documentos/7")
        String urlDescarga
) {
    public static DocumentoPropuestaResponse fromEntity(DocumentoPropuesta d, Long propuestaId) {
        return new DocumentoPropuestaResponse(
                d.getId(),
                d.getNombreOriginal(),
                d.getTipoContenido(),
                d.getTamanoBytes(),
                d.getPrincipal(),
                d.getFechaCarga(),
                d.getId() != null ? "/api/v1/propuestas/" + propuestaId + "/documentos/" + d.getId() : null
        );
    }
}
