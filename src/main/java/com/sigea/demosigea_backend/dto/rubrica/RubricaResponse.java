package com.sigea.demosigea_backend.dto.rubrica;

import com.sigea.demosigea_backend.dto.evaluacion.CriterioRubricaResponse;
import com.sigea.demosigea_backend.model.CriterioRubrica;
import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * Rúbrica activa de una convocatoria con sus criterios (HU-15).
 *
 * @param rubricaId              ID de la rúbrica.
 * @param convocatoriaId         ID de la convocatoria.
 * @param nombre                 nombre de la rúbrica.
 * @param activo                 si es la rúbrica vigente.
 * @param sumaPesos              suma de los pesos de los criterios (siempre 100 al guardar).
 * @param criterios              criterios con peso y escala.
 * @param asignacionesReabiertas asignaciones que volvieron a pendiente al reemplazar la rúbrica (0 si no aplica).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record RubricaResponse(
        @Schema(example = "4") Long rubricaId,
        @Schema(example = "10") Long convocatoriaId,
        @Schema(example = "Rúbrica de ponencias 2026") String nombre,
        @Schema(example = "true") boolean activo,
        @Schema(example = "100.00") BigDecimal sumaPesos,
        List<CriterioRubricaResponse> criterios,
        @Schema(example = "0") long asignacionesReabiertas
) {

    /**
     * Construye la respuesta a partir de la rúbrica y sus criterios.
     *
     * @param rubrica                rúbrica.
     * @param criterios              criterios de la rúbrica.
     * @param asignacionesReabiertas asignaciones reabiertas por el reemplazo.
     * @return DTO de la rúbrica.
     */
    public static RubricaResponse of(RubricaEvaluacion rubrica, List<CriterioRubrica> criterios,
                                     long asignacionesReabiertas) {
        BigDecimal suma = criterios.stream()
                .map(CriterioRubrica::getPesoPorcentual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RubricaResponse(
                rubrica.getId(),
                rubrica.getConvocatoria() != null ? rubrica.getConvocatoria().getId() : null,
                rubrica.getNombre(),
                Boolean.TRUE.equals(rubrica.getActivo()),
                suma,
                criterios.stream().map(CriterioRubricaResponse::fromEntity).toList(),
                asignacionesReabiertas);
    }
}
