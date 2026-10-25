package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ComiteEvaluador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link ComiteEvaluador} (HU-17).
 * <p>
 * Permite verificar y recuperar la membresía de un evaluador en el comité de una convocatoria.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ComiteEvaluadorRepository extends JpaRepository<ComiteEvaluador, Long> {

    /**
     * Busca el registro de un evaluador específico en el comité de una convocatoria.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @param personaId      ID de la persona evaluadora.
     * @return el miembro del comité, o vacío si no existe la vinculación.
     */
    Optional<ComiteEvaluador> findByConvocatoria_IdAndPersona_Id(Long convocatoriaId, Long personaId);

    /**
     * Evaluador del comité con su persona, afiliación y convocatoria ya cargadas (HU-16).
     *
     * @param id ID del miembro del comité evaluador.
     * @return el evaluador, o vacío si no existe.
     */
    @Query("SELECT ce FROM ComiteEvaluador ce JOIN FETCH ce.persona per LEFT JOIN FETCH per.afiliacion "
            + "JOIN FETCH ce.convocatoria WHERE ce.id = :id")
    Optional<ComiteEvaluador> findDetalleById(@Param("id") Long id);

    /**
     * Evaluadores registrados en el comité de una convocatoria, ordenados por apellidos (HU-16).
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return miembros del comité evaluador.
     */
    @Query("SELECT ce FROM ComiteEvaluador ce JOIN FETCH ce.persona per LEFT JOIN FETCH per.afiliacion "
            + "WHERE ce.convocatoria.id = :convocatoriaId ORDER BY per.apellidos, per.nombres")
    List<ComiteEvaluador> findByConvocatoriaConPersona(@Param("convocatoriaId") Long convocatoriaId);
}
