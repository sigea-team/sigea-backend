package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link AsignacionEvaluacion} (HU-17).
 * <p>
 * Permite consultar las asignaciones de evaluación de propuestas a evaluadores,
 * incluyendo verificación de completitud para el cálculo de puntuación ponderada.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface AsignacionEvaluacionRepository extends JpaRepository<AsignacionEvaluacion, Long> {

    /**
     * Lista todas las asignaciones de evaluación de una propuesta específica.
     *
     * @param propuestaId ID de la propuesta.
     * @return lista de asignaciones, incluyendo todos los estados.
     */
    List<AsignacionEvaluacion> findByPropuesta_Id(Long propuestaId);

    /**
     * Busca la asignación de un evaluador específico para una propuesta.
     *
     * @param propuestaId       ID de la propuesta.
     * @param comiteEvaluadorId ID del miembro evaluador.
     * @return la asignación, o vacío si no existe.
     */
    Optional<AsignacionEvaluacion> findByPropuesta_IdAndComiteEvaluador_Id(Long propuestaId, Long comiteEvaluadorId);

    /**
     * Verifica si alguna asignación de la propuesta tiene un estado distinto al indicado.
     * <p>
     * Se usa para comprobar si quedan evaluaciones pendientes o en proceso
     * antes de calcular la puntuación ponderada (Criterio 3, HU-17).
     * </p>
     *
     * @param propuestaId ID de la propuesta.
     * @param estado      Estado que se excluye de la búsqueda (normalmente {@code completada}).
     * @return {@code true} si existe al menos una asignación con un estado diferente al indicado.
     */
    boolean existsByPropuesta_IdAndEstadoNot(Long propuestaId, EstadoAsignacion estado);
}
