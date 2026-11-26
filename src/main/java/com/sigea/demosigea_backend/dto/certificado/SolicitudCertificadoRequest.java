package com.sigea.demosigea_backend.dto.certificado;

import com.sigea.demosigea_backend.model.TipoCertificado;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud del participante para generar su certificado en un evento (HU-26, Criterio 1).
 *
 * @param tipo rol del certificado: asistente, ponente o conferencista.
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Rol del certificado que se solicita")
public record SolicitudCertificadoRequest(
        @Schema(description = "Rol del certificado", example = "asistente",
                allowableValues = {"asistente", "ponente", "conferencista"})
        @NotNull(message = "Indique el tipo de certificado.")
        TipoCertificado tipo
) {
}
