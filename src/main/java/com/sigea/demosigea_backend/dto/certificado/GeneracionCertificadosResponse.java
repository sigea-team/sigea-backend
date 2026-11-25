package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado de generar los certificados de un evento (CU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado de la generación de certificados del evento")
public record GeneracionCertificadosResponse(
        @Schema(description = "ID del evento", example = "3") Long eventoId,
        @Schema(description = "Certificados emitidos en esta generación") List<CertificadoResponse> generados,
        @Schema(description = "Certificados que ya existían y no se duplicaron (Criterio 3)", example = "4") int yaEmitidos,
        @Schema(description = "Participantes que no cumplen los requisitos, con el motivo") List<ExclusionCertificadoResponse> excluidos
) {
}
