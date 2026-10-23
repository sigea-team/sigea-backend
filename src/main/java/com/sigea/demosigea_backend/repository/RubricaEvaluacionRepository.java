package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.RubricaEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link RubricaEvaluacion} (HU-17).
 * <p>
 * Permite recuperar la rúbrica activa configurada para una convocatoria específica,
 * necesaria para validar criterios y calcular puntuaciones ponderadas.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface RubricaEvaluacionRepository extends JpaRepository<RubricaEvaluacion, Long> {

    /**
     * Busca la rúbrica activa asociada a una convocatoria.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return rúbrica activa, o vacío si no existe ninguna configurada.
     */
    Optional<RubricaEvaluacion> findByConvocatoria_IdAndActivoTrue(Long convocatoriaId);
}
