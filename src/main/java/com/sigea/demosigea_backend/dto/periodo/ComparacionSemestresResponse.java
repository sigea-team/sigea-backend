package com.sigea.demosigea_backend.dto.periodo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Indicadores de dos semestres presentados de forma comparativa (HU-27, Criterio 5).
 *
 * @param semestreA   Primer semestre (base de la comparación).
 * @param semestreB   Segundo semestre.
 * @param periodoA    Indicadores del primer semestre.
 * @param periodoB    Indicadores del segundo semestre.
 * @param comparativo Variación de cada indicador de A a B.
 * @param mensaje     Aviso si alguno de los semestres no tiene información (null si ambos tienen).
 */
@Schema(description = "Comparación de indicadores entre dos semestres")
public record ComparacionSemestresResponse(
        String semestreA,
        String semestreB,
        IndicadoresPeriodoResponse periodoA,
        IndicadoresPeriodoResponse periodoB,
        List<VariacionIndicadorResponse> comparativo,
        String mensaje
) {
}
