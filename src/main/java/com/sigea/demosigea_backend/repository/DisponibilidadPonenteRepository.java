package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.DisponibilidadPonente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link DisponibilidadPonente} — HU-Salas, Criterios 2 y 5.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface DisponibilidadPonenteRepository extends JpaRepository<DisponibilidadPonente, Long> {

    /** Franjas de disponibilidad de un ponente en un evento, ordenadas cronológicamente. */
    List<DisponibilidadPonente> findByPersona_IdAndEvento_IdOrderByFechaAscHoraInicioAsc(Long personaId, Long eventoId);

    /** Elimina todas las franjas de un ponente en un evento (para reemplazarlas en bloque). */
    void deleteByPersona_IdAndEvento_Id(Long personaId, Long eventoId);
}
