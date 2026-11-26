package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de pedir un certificado (HU-26). Si ya existía se devuelve el mismo, sin crear otro (Criterio 3).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Certificado entregado al participante")
public record EmisionCertificadoResponse(
        @Schema(description = "true si se emitió en esta solicitud; false si ya existía y se entrega el mismo", example = "true")
        boolean generadoAhora,

        @Schema(description = "Mensaje para el usuario", example = "Se generó tu certificado de asistente.")
        String mensaje,

        @Schema(description = "Certificado")
        CertificadoResponse certificado
) {
}
