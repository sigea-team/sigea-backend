package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ParticipacionEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las constancias de quién respondió cada encuesta (HU-25).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ParticipacionEncuestaRepository extends JpaRepository<ParticipacionEncuesta, Long> {

    /** Constancia de que la persona ya respondió la encuesta. */
    Optional<ParticipacionEncuesta> findByEncuesta_IdAndPersona_Id(Long encuestaId, Long personaId);

    /** Cantidad de personas que respondieron la encuesta. */
    long countByEncuesta_Id(Long encuestaId);

    /** Encuestas que la persona ya respondió. */
    List<ParticipacionEncuesta> findByPersona_Id(Long personaId);

    /** Quiénes respondieron la encuesta, en orden de respuesta (solo se expone en encuestas identificadas). */
    @Query("SELECT p FROM ParticipacionEncuesta p JOIN FETCH p.persona WHERE p.encuesta.id = :encuestaId "
            + "ORDER BY p.fechaRespuesta ASC, p.id ASC")
    List<ParticipacionEncuesta> findByEncuestaConPersona(@Param("encuestaId") Long encuestaId);
}
