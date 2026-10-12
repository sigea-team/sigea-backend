package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.LineaTematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link LineaTematica} (HU-05, RF07).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface LineaTematicaRepository extends JpaRepository<LineaTematica, Long> {

    /** Líneas temáticas del evento, ordenadas por nombre. */
    List<LineaTematica> findByEvento_IdOrderByNombreAscIdAsc(Long eventoId);

    /** Busca una línea garantizando que pertenezca al evento indicado. */
    Optional<LineaTematica> findByIdAndEvento_Id(Long id, Long eventoId);

    /** Criterio 3: ¿ya existe una línea con ese nombre (sin distinguir mayúsculas) en el evento? */
    boolean existsByEvento_IdAndNombreIgnoreCase(Long eventoId, String nombre);

    /** Criterio 3 al actualizar: igual que el anterior, sin contar el propio registro. */
    boolean existsByEvento_IdAndNombreIgnoreCaseAndIdNot(Long eventoId, String nombre, Long excluirId);


}
