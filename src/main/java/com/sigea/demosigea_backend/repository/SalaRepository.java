package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Sala} — HU-Salas.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {

    /** Salas del evento ordenadas por nombre y luego por ID. */
    List<Sala> findByEvento_IdOrderByNombreAscIdAsc(Long eventoId);

    /** Sala específica del evento (para validar pertenencia). */
    Optional<Sala> findByIdAndEvento_Id(Long salaId, Long eventoId);

    /** ¿Existe alguna sala con ese nombre (insensible a mayúsculas) en el evento? — Criterio 3. */
    boolean existsByEvento_IdAndNombreIgnoreCase(Long eventoId, String nombre);

    /** ¿Existe otra sala (distinta a la indicada) con ese nombre en el evento? — para actualizar. */
    boolean existsByEvento_IdAndNombreIgnoreCaseAndIdNot(Long eventoId, String nombre, Long salaId);
}
