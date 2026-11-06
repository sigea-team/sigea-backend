package com.sigea.demosigea_backend.dto.clasificacion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Umbrales con los que se clasifican las propuestas de una convocatoria (HU-18, Criterio 1).
 *
 * @param umbralAceptacion puntuación mínima (0 - 10) para clasificar como aprobada (aceptada).
 * @param umbralAjustes    puntuación mínima (0 - 10) para clasificar como aprobada con ajustes; por debajo, rechazada.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Umbrales de clasificación de las propuestas de una convocatoria (escala 0 - 10)")
public record UmbralesClasificacionRequest(
        @Schema(example = "7.0")
        @NotNull(message = "El umbral de aprobación es obligatorio.")
        @DecimalMin(value = "0.0", message = "El umbral de aprobación no puede ser menor que 0.")
        @DecimalMax(value = "10.0", message = "El umbral de aprobación no puede ser mayor que 10.")
        @Digits(integer = 2, fraction = 2, message = "El umbral de aprobación admite hasta 2 decimales.")
        BigDecimal umbralAceptacion,

        @Schema(example = "5.0")
        @NotNull(message = "El umbral de aprobación con ajustes es obligatorio.")
        @DecimalMin(value = "0.0", message = "El umbral de aprobación con ajustes no puede ser menor que 0.")
        @DecimalMax(value = "10.0", message = "El umbral de aprobación con ajustes no puede ser mayor que 10.")
        @Digits(integer = 2, fraction = 2, message = "El umbral de aprobación con ajustes admite hasta 2 decimales.")
        BigDecimal umbralAjustes
) {
}
