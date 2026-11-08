package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.AgendaPublicada;
import com.sigea.demosigea_backend.model.EstadoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link AgendaPublicada} (HU-22).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface AgendaPublicadaRepository extends JpaRepository<AgendaPublicada, Long> {

    /** Días de agenda del evento, en orden. */
    List<AgendaPublicada> findByEvento_IdOrderByFechaDiaAsc(Long eventoId);

    /** Días del evento en un estado, en orden (Criterio 3: los publicados son los visibles). */
    List<AgendaPublicada> findByEvento_IdAndEstadoOrderByFechaDiaAsc(Long eventoId, EstadoAgenda estado);

    /** Fila de un día concreto del evento. */
    Optional<AgendaPublicada> findByEvento_IdAndFechaDia(Long eventoId, LocalDate fechaDia);

    /** ¿El evento tiene algún día con la agenda en ese estado? */
    boolean existsByEvento_IdAndEstado(Long eventoId, EstadoAgenda estado);

    /** ¿La agenda de ese día del evento está en ese estado? (Criterio 5: el cambio afecta una agenda publicada). */
    boolean existsByEvento_IdAndFechaDiaAndEstado(Long eventoId, LocalDate fechaDia, EstadoAgenda estado);
}
