package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.InscripcionEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de inscripciones a eventos (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface InscripcionEventoRepository extends JpaRepository<InscripcionEvento, Long> {

    /**
     * Busca la inscripción de una persona a un evento específico.
     */
    Optional<InscripcionEvento> findByEvento_IdAndPersona_Id(Long eventoId, Long personaId);

    /**
     * Verifica si una persona ya está inscrita en un evento (Criterio 4).
     */
    boolean existsByEvento_IdAndPersona_Id(Long eventoId, Long personaId);

    /**
     * Lista todas las inscripciones asociadas a un evento, ordenadas por fecha reciente.
     */
    @Query("SELECT ie FROM InscripcionEvento ie JOIN FETCH ie.persona p WHERE ie.evento.id = :eventoId ORDER BY ie.fechaInscripcion DESC")
    List<InscripcionEvento> findByEvento_IdOrderByFechaInscripcionDesc(@Param("eventoId") Long eventoId);

    /**
     * Lista todas las inscripciones de una persona a distintos eventos.
     */
    @Query("SELECT ie FROM InscripcionEvento ie JOIN FETCH ie.evento e WHERE ie.persona.id = :personaId ORDER BY ie.fechaInscripcion DESC")
    List<InscripcionEvento> findByPersona_IdOrderByFechaInscripcionDesc(@Param("personaId") Long personaId);

    /**
     * Cantidad total de inscritos en un evento.
     */
    long countByEvento_Id(Long eventoId);
}
