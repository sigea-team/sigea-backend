package com.sigea.demosigea_backend.dto.rubrica;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Criterio de la rúbrica con su peso y su escala de calificación (HU-15, Criterio 1).
 * <p>
 * Si no se envía la escala se usa 0 - 10, la misma que acepta el registro de evaluaciones (HU-17).
 * </p>
 *
 * @param nombre         nombre del criterio, único dentro de la rúbrica.
 * @param pesoPorcentual peso del criterio; la suma de todos debe ser exactamente 100.
 * @param escalaMinima   valor mínimo de la escala (opcional, por defecto 0).
 * @param escalaMaxima   valor máximo de la escala (opcional, por defecto 10).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record CriterioRubricaRequest(

        @Schema(description = "Nombre del criterio", example = "Originalidad")
        @NotBlank(message = "El nombre del criterio es obligatorio.")
        @Size(max = 100, message = "El nombre del criterio no puede superar los 100 caracteres.")
        String nombre,

        @Schema(description = "Peso porcentual del criterio", example = "40.00")
        @NotNull(message = "El peso porcentual del criterio es obligatorio.")
        @DecimalMin(value = "0.00", inclusive = false, message = "El peso del criterio debe ser mayor que 0.")
        @DecimalMax(value = "100.00", message = "El peso del criterio no puede superar 100.")
        @Digits(integer = 3, fraction = 2, message = "El peso admite como máximo 2 decimales.")
        BigDecimal pesoPorcentual,

        @Schema(description = "Valor mínimo de la escala (0 por defecto)", example = "0")
        @DecimalMin(value = "0.00", message = "La escala mínima no puede ser menor que 0.")
        @DecimalMax(value = "10.00", message = "La escala mínima no puede superar 10.")
        @Digits(integer = 2, fraction = 2, message = "La escala admite como máximo 2 decimales.")
        BigDecimal escalaMinima,

        @Schema(description = "Valor máximo de la escala (10 por defecto)", example = "10")
        @DecimalMin(value = "0.00", message = "La escala máxima no puede ser menor que 0.")
        @DecimalMax(value = "10.00", message = "La escala máxima no puede superar 10.")
        @Digits(integer = 2, fraction = 2, message = "La escala admite como máximo 2 decimales.")
        BigDecimal escalaMaxima
) {
}
