package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Comparación de un indicador entre dos semestres (HU-27, Criterio 5).
 *
 * @param codigo              Código estable del indicador (ej. {@code totalEventos}).
 * @param nombre              Nombre legible del indicador.
 * @param valorA              Valor en el primer semestre (null si no aplica, ej. tasa sin propuestas).
 * @param valorB              Valor en el segundo semestre.
 * @param diferencia          valorB - valorA (null si alguno es null).
 * @param variacionPorcentual Variación de A a B en porcentaje (null si valorA es 0 o null).
 * @param tendencia           AUMENTO, DISMINUCION, SIN_CAMBIO o NO_COMPARABLE.
 */
@Schema(description = "Indicador comparado entre dos semestres")
public record VariacionIndicadorResponse(
        String codigo,
        String nombre,
        BigDecimal valorA,
        BigDecimal valorB,
        BigDecimal diferencia,
        BigDecimal variacionPorcentual,
        String tendencia
) {
}
