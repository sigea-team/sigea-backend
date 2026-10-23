package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.CriterioRubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link CriterioRubrica} (HU-17).
 * <p>
 * Permite listar los criterios de evaluación configurados en una rúbrica específica,
 * con sus pesos porcentuales para el cálculo de la puntuación ponderada.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface CriterioRubricaRepository extends JpaRepository<CriterioRubrica, Long> {

    /**
     * Lista todos los criterios de una rúbrica específica.
     *
     * @param rubricaId ID de la rúbrica.
     * @return lista de criterios con sus nombres y pesos porcentuales.
     */
    List<CriterioRubrica> findByRubrica_Id(Long rubricaId);
}
