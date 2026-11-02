package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.DisponibilidadSala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link DisponibilidadSala} — HU-Salas, Criterios 1 y 5.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface DisponibilidadSalaRepository extends JpaRepository<DisponibilidadSala, Long> {

    /** Franjas de disponibilidad de una sala ordenadas cronológicamente. */
    List<DisponibilidadSala> findBySala_IdOrderByFechaAscHoraInicioAsc(Long salaId);

    /** Elimina todas las franjas de una sala (para reemplazarlas en bloque). */
    void deleteBySala_Id(Long salaId);
}
