package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las participaciones de conferencistas en los eventos (HU-12, RF16 y RF17).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ParticipacionConferencistaRepository extends JpaRepository<ParticipacionConferencista, Long> {

    /** Conferencistas de un evento, en el orden en que fueron registrados. */
    @EntityGraph(attributePaths = {"persona", "evento"})
    List<ParticipacionConferencista> findByEvento_IdOrderByFechaRegistroAscIdAsc(Long eventoId);

    /** Historial de participaciones de una persona, del evento más reciente al más antiguo (RF17). */
    @EntityGraph(attributePaths = {"evento"})
    @Query("SELECT p FROM ParticipacionConferencista p WHERE p.persona.id = :personaId "
            + "ORDER BY p.evento.fechaInicio DESC, p.id DESC")
    List<ParticipacionConferencista> historialDePersona(@Param("personaId") Long personaId);

    /** Criterio 3: la persona ya está asociada al evento. */
    boolean existsByEvento_IdAndPersona_Id(Long eventoId, Long personaId);

    /** Indica si la persona participa (o participó) como conferencista en algún evento. */
    boolean existsByPersona_Id(Long personaId);

    /** Participaciones de varias personas a la vez (conteos y asociación en la lista de registrados). */
    List<ParticipacionConferencista> findByPersona_IdIn(Collection<Long> personaIds);

    /** Cantidad de eventos en los que ha participado la persona. */
    long countByPersona_Id(Long personaId);

    @EntityGraph(attributePaths = {"persona", "evento"})
    Optional<ParticipacionConferencista> findByIdAndEvento_Id(Long id, Long eventoId);
}
