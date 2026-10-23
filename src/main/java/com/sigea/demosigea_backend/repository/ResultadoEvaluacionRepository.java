package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ResultadoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link ResultadoEvaluacion} (HU-17).
 * <p>
 * Permite recuperar y persistir los resultados de evaluación calculados para cada propuesta,
 * incluyendo la puntuación ponderada y la clasificación final.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ResultadoEvaluacionRepository extends JpaRepository<ResultadoEvaluacion, Long> {

    /**
     * Busca el resultado de evaluación de una propuesta específica.
     *
     * @param propuestaId ID de la propuesta.
     * @return el resultado calculado, o vacío si aún no se han completado todas las evaluaciones.
     */
    Optional<ResultadoEvaluacion> findByPropuesta_Id(Long propuestaId);
}
