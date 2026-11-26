package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Configuración de certificados del evento (RN-01).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Configuración de certificados del evento")
public record ConfiguracionCertificadosResponse(
        @Schema(description = "ID del evento", example = "3") Long eventoId,
        @Schema(description = "Porcentaje mínimo de asistencia", example = "80") BigDecimal porcentajeMinimoAsistencia,
        @Schema(description = "Si el porcentaje se puede cambiar (no hay certificados de asistente y el evento no está archivado)",
                example = "true") boolean editable,
        @Schema(description = "Motivo por el que no se puede cambiar, o null", example = "Ya se emitieron 4 certificados de asistente.")
        String motivoNoEditable,
        @Schema(description = "Certificados emitidos en el evento", example = "0") long certificadosEmitidos
) {
}
