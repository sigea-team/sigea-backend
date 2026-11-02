package com.sigea.demosigea_backend.dto.evaluacion;

import com.sigea.demosigea_backend.model.CriterioRubrica;

import java.math.BigDecimal;

/**
 * Criterio de la rúbrica activa de la convocatoria, para que el evaluador sepa qué calificar (HU-17).
 *
 * @param criterioId     ID del criterio en {@code criterios_rubrica}.
 * @param nombre         Nombre del criterio.
 * @param pesoPorcentual Peso porcentual del criterio en la puntuación final.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record CriterioRubricaResponse(
        Long criterioId,
        String nombre,
        BigDecimal pesoPorcentual
) {

    /**
     * Construye el DTO a partir de la entidad {@link CriterioRubrica}.
     *
     * @param c entidad del criterio.
     * @return DTO del criterio.
     */
    public static CriterioRubricaResponse fromEntity(CriterioRubrica c) {
        return new CriterioRubricaResponse(c.getId(), c.getNombre(), c.getPesoPorcentual());
    }
}
