package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Firmante de los certificados del evento (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Firmante de los certificados del evento")
public record FirmanteCertificadoResponse(
        @Schema(description = "ID del firmante", example = "4") Long firmanteId,
        @Schema(description = "Nombre impreso bajo la firma", example = "Judith del Pilar Rodríguez Tenjo") String nombre,
        @Schema(description = "Cargo impreso bajo el nombre", example = "Directora del Programa de Ingeniería de Sistemas")
        String cargo,
        @Schema(description = "Posición en el pie, de izquierda a derecha", example = "1") int orden,
        @Schema(description = "Ruta de la imagen de la firma (PNG, requiere sesión)",
                example = "/api/v1/eventos/3/certificados/plantilla/firmantes/4/firma") String urlFirma
) {
}
