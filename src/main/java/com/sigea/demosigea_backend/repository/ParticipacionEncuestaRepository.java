package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ParticipacionEncuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    /**
     * Registra que la persona respondió la encuesta si todavía no lo había hecho (CU-22, flujo alterno 1.1).
     * Con {@code ON CONFLICT DO NOTHING} un envío duplicado, también uno simultáneo, no lanza una excepción de la base
     * de datos ni deja la transacción marcada para rollback: simplemente no inserta. Si otra transacción está
     * registrando la misma participación, esta espera a que termine y luego inserta (si la otra se revirtió) o no.
     *
     * @return 1 si se registró; 0 si la persona ya había respondido.
     */
    @Modifying
    @Query(value = """
            INSERT INTO participaciones_encuesta (encuesta_id, persona_id, fecha_respuesta)
            VALUES (:encuestaId, :personaId, :fechaRespuesta)
            ON CONFLICT (encuesta_id, persona_id) DO NOTHING
            """, nativeQuery = true)
    int registrarSiNoExiste(@Param("encuestaId") Long encuestaId,
                            @Param("personaId") Long personaId,
                            @Param("fechaRespuesta") LocalDateTime fechaRespuesta);

    /** Cantidad de personas que respondieron la encuesta. */
    long countByEncuesta_Id(Long encuestaId);

    /** Encuestas que la persona ya respondió. */
    List<ParticipacionEncuesta> findByPersona_Id(Long personaId);

    /** Quiénes respondieron la encuesta, en orden de respuesta (solo se expone en encuestas identificadas). */
    @Query("SELECT p FROM ParticipacionEncuesta p JOIN FETCH p.persona WHERE p.encuesta.id = :encuestaId "
            + "ORDER BY p.fechaRespuesta ASC, p.id ASC")
    List<ParticipacionEncuesta> findByEncuestaConPersona(@Param("encuestaId") Long encuestaId);
}
