package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Sala} — HU-Salas.
 * Permite gestionar salas tanto a nivel institucional (desvinculadas) como vinculadas a eventos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {

    /** Todas las salas institucionales ordenadas por nombre y luego por ID. */
    List<Sala> findAllByOrderByNombreAscIdAsc();

    /** Salas vinculadas a un evento específico (a través de evento_salas o por su evento directo). */
    @Query("SELECT DISTINCT s FROM Sala s LEFT JOIN s.eventos e "
            + "WHERE e.id = :eventoId OR s.evento.id = :eventoId "
            + "ORDER BY s.nombre ASC, s.id ASC")
    List<Sala> findByEvento_IdOrderByNombreAscIdAsc(@Param("eventoId") Long eventoId);

    /** Sala específica vinculada a un evento (para validar pertenencia). */
    @Query("SELECT DISTINCT s FROM Sala s LEFT JOIN s.eventos e "
            + "WHERE s.id = :salaId AND (e.id = :eventoId OR s.evento.id = :eventoId)")
    Optional<Sala> findByIdAndEvento_Id(@Param("salaId") Long salaId, @Param("eventoId") Long eventoId);

    /** Verifica si una sala está vinculada a un evento. */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sala s LEFT JOIN s.eventos e "
            + "WHERE s.id = :salaId AND (e.id = :eventoId OR s.evento.id = :eventoId)")
    boolean isSalaLinkedToEvento(@Param("salaId") Long salaId, @Param("eventoId") Long eventoId);

    /** ¿Existe alguna sala con ese nombre (insensible a mayúsculas) en el evento? — Criterio 3. */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sala s LEFT JOIN s.eventos e "
            + "WHERE (e.id = :eventoId OR s.evento.id = :eventoId) AND LOWER(s.nombre) = LOWER(:nombre)")
    boolean existsByEvento_IdAndNombreIgnoreCase(@Param("eventoId") Long eventoId, @Param("nombre") String nombre);

    /** ¿Existe otra sala con ese nombre en el evento? — para actualizar. */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Sala s LEFT JOIN s.eventos e "
            + "WHERE (e.id = :eventoId OR s.evento.id = :eventoId) AND LOWER(s.nombre) = LOWER(:nombre) AND s.id != :salaId")
    boolean existsByEvento_IdAndNombreIgnoreCaseAndIdNot(@Param("eventoId") Long eventoId, @Param("nombre") String nombre, @Param("salaId") Long salaId);

    /** ¿Existe alguna sala institucional global con ese nombre? */
    boolean existsByNombreIgnoreCase(String nombre);

    /** ¿Existe otra sala con ese nombre? */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long salaId);
}
