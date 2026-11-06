package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Evaluacion} (HU-17).
 * <p>
 * Permite gestionar las calificaciones registradas por criterio dentro de cada asignación
 * de evaluación, y consultar todas las evaluaciones de una propuesta entre todos los evaluadores.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    /**
     * Lista todas las calificaciones registradas dentro de una asignación.
     *
     * @param asignacionId ID de la asignación de evaluación.
     * @return lista de evaluaciones por criterio de la asignación.
     */
    List<Evaluacion> findByAsignacion_Id(Long asignacionId);

    /**
     * Busca la calificación de un criterio específico dentro de una asignación.
     *
     * @param asignacionId ID de la asignación.
     * @param criterioId   ID del criterio de la rúbrica.
     * @return la evaluación existente, o vacío si aún no ha sido registrada.
     */
    Optional<Evaluacion> findByAsignacion_IdAndCriterio_Id(Long asignacionId, Long criterioId);

    /**
     * Verifica si ya existe una calificación para un criterio en una asignación.
     *
     * @param asignacionId ID de la asignación.
     * @param criterioId   ID del criterio.
     * @return {@code true} si ya fue registrada una calificación para ese criterio.
     */
    boolean existsByAsignacion_IdAndCriterio_Id(Long asignacionId, Long criterioId);

    /**
     * Lista todas las evaluaciones registradas para una propuesta entre todos sus evaluadores.
     * <p>
     * Se utiliza para agregar las calificaciones por criterio y calcular los promedios
     * necesarios para la puntuación ponderada (Criterio 1, HU-17).
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @return lista de todas las evaluaciones de todos los evaluadores asignados a la propuesta.
     */
    List<Evaluacion> findByAsignacion_Propuesta_Id(Long propuestaId);

    /**
     * Indica si la asignación ya tiene calificaciones registradas (impide reasignarla, HU-16).
     *
     * @param asignacionId ID de la asignación.
     * @return {@code true} si existe al menos una calificación.
     */
    boolean existsByAsignacion_Id(Long asignacionId);

    /**
     * HU-15, Criterio 4: número de calificaciones registradas con los criterios de una rúbrica.
     *
     * @param rubricaId ID de la rúbrica.
     * @return calificaciones que dependen de la rúbrica.
     */
    long countByCriterio_Rubrica_Id(Long rubricaId);

    /**
     * HU-15, Criterio 4: asignaciones que ya tienen calificaciones con los criterios de una rúbrica.
     * Son las que deben volver a evaluarse si la rúbrica se reemplaza.
     *
     * @param rubricaId ID de la rúbrica.
     * @return asignaciones afectadas, con su propuesta cargada.
     */
    @Query("SELECT a FROM AsignacionEvaluacion a JOIN FETCH a.propuesta "
            + "WHERE EXISTS (SELECT 1 FROM Evaluacion e WHERE e.asignacion = a AND e.criterio.rubrica.id = :rubricaId)")
    List<AsignacionEvaluacion> findAsignacionesConCalificacionesDeRubrica(@Param("rubricaId") Long rubricaId);
}
