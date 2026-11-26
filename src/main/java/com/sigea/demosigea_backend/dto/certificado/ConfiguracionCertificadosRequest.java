package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Condición de asistencia del evento para el certificado de asistente (RN-01).
 *
 * @param porcentajeMinimoAsistencia porcentaje de las actividades inscritas al que se debe asistir.
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Porcentaje mínimo de asistencia para el certificado de asistente")
public record ConfiguracionCertificadosRequest(
        @Schema(description = "Porcentaje (mayor que 0 y hasta 100, con máximo 2 decimales)", example = "80")
        @NotNull(message = "Indique el porcentaje mínimo de asistencia.")
        @DecimalMin(value = "0", inclusive = false, message = "El porcentaje mínimo debe ser mayor que 0.")
        @DecimalMax(value = "100", message = "El porcentaje mínimo no puede ser mayor que 100.")
        @Digits(integer = 3, fraction = 2, message = "El porcentaje admite máximo 2 decimales.")
        BigDecimal porcentajeMinimoAsistencia
) {
}
