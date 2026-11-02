package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.AsignacionEvaluacion;
import com.sigea.demosigea_backend.model.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    /**
     * Asignaciones de una propuesta con el evaluador, su persona y la convocatoria ya cargados (HU-16).
     *
     * @param propuestaId ID de la propuesta.
     * @return asignaciones de la propuesta, de la más reciente a la más antigua.
     */
    @Query("SELECT a FROM AsignacionEvaluacion a "
            + "JOIN FETCH a.propuesta p JOIN FETCH p.convocatoria "
            + "JOIN FETCH a.comiteEvaluador ce JOIN FETCH ce.persona "
            + "WHERE p.id = :propuestaId ORDER BY a.fechaAsignacion DESC, a.id DESC")
    List<AsignacionEvaluacion> findDetalleByPropuestaId(@Param("propuestaId") Long propuestaId);

    /**
     * Asignaciones de varias propuestas en una sola consulta, con el evaluador y su persona ya cargados (HU-16).
     * Evita una consulta por propuesta al listar las propuestas de una convocatoria.
     *
     * @param propuestaIds IDs de las propuestas.
     * @return asignaciones de la más reciente a la más antigua.
     */
    @Query("SELECT a FROM AsignacionEvaluacion a "
            + "JOIN FETCH a.comiteEvaluador ce JOIN FETCH ce.persona "
            + "WHERE a.propuesta.id IN :propuestaIds ORDER BY a.fechaAsignacion DESC, a.id DESC")
    List<AsignacionEvaluacion> findDetalleByPropuestaIds(@Param("propuestaIds") Collection<Long> propuestaIds);

    /**
     * Bandeja del evaluador: asignaciones de todos los comités en los que participa la persona (HU-16, Criterio 1).
     *
     * @param personaId ID de la persona evaluadora.
     * @return asignaciones de la más reciente a la más antigua.
     */
    @Query("SELECT a FROM AsignacionEvaluacion a "
            + "JOIN FETCH a.propuesta p JOIN FETCH p.convocatoria "
            + "JOIN FETCH a.comiteEvaluador ce JOIN FETCH ce.persona per "
            + "WHERE per.id = :personaId ORDER BY a.fechaAsignacion DESC, a.id DESC")
    List<AsignacionEvaluacion> findBandejaByPersonaId(@Param("personaId") Long personaId);

    /**
     * Bandeja del evaluador con solo el trabajo actual: excluye las asignaciones en el estado indicado
     * (normalmente {@code completada}) (HU-16, Criterio 1).
     *
     * @param personaId ID de la persona evaluadora.
     * @param estado    Estado excluido.
     * @return asignaciones de la más reciente a la más antigua.
     */
    @Query("SELECT a FROM AsignacionEvaluacion a "
            + "JOIN FETCH a.propuesta p JOIN FETCH p.convocatoria "
            + "JOIN FETCH a.comiteEvaluador ce JOIN FETCH ce.persona per "
            + "WHERE per.id = :personaId AND a.estado <> :estado "
            + "ORDER BY a.fechaAsignacion DESC, a.id DESC")
    List<AsignacionEvaluacion> findBandejaByPersonaIdAndEstadoNot(@Param("personaId") Long personaId,
                                                                  @Param("estado") EstadoAsignacion estado);

    /**
     * Indica si el evaluador ya tiene asignada la propuesta (índice único propuesta_id + comite_evaluador_id).
     *
     * @param propuestaId       ID de la propuesta.
     * @param comiteEvaluadorId ID del miembro evaluador.
     * @return {@code true} si la asignación ya existe.
     */
    boolean existsByPropuesta_IdAndComiteEvaluador_Id(Long propuestaId, Long comiteEvaluadorId);

    /**
     * Carga de trabajo del evaluador: asignaciones que aún no ha completado (HU-16).
     *
     * @param comiteEvaluadorId ID del miembro evaluador.
     * @param estado            Estado excluido (normalmente {@code completada}).
     * @return número de asignaciones abiertas.
     */
    long countByComiteEvaluador_IdAndEstadoNot(Long comiteEvaluadorId, EstadoAsignacion estado);

    /**
     * HU-14, Criterio 4 (RNF08): indica si la persona es evaluadora asignada a la propuesta.
     *
     * @param propuestaId ID de la propuesta.
     * @param personaId   ID de la persona del evaluador.
     * @return {@code true} si existe una asignación de esa propuesta a ese evaluador.
     */
    boolean existsByPropuesta_IdAndComiteEvaluador_Persona_Id(Long propuestaId, Long personaId);
}
