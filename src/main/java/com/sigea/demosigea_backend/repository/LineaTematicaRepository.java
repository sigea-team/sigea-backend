package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.LineaTematica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link LineaTematica}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface LineaTematicaRepository extends JpaRepository<LineaTematica, Long> {

    /**
     * Obtiene todas las líneas temáticas asociadas a un evento ordenadas por ID asc.
     */
    List<LineaTematica> findByEvento_IdOrderByIdAsc(Long eventoId);

    /**
     * Busca una línea temática por su ID y evento.
     */
    Optional<LineaTematica> findByIdAndEvento_Id(Long id, Long eventoId);

    /**
     * Verifica si ya existe una línea temática con el mismo nombre (sin distinguir mayúsculas/minúsculas) en el evento.
     */
    boolean existsByEvento_IdAndNombreIgnoreCase(Long eventoId, String nombre);

    /**
     * Verifica si ya existe otra línea temática con el mismo nombre en el mismo evento (para actualizaciones).
     */
    boolean existsByEvento_IdAndNombreIgnoreCaseAndIdNot(Long eventoId, String nombre, Long id);

    /**
     * Comprueba si la línea temática está referenciada por alguna propuesta o actividad.
     */
    @Query(value = "SELECT (SELECT COUNT(*) FROM propuestas WHERE linea_tematica_id = :id) + " +
                   "(SELECT COUNT(*) FROM actividades WHERE linea_tematica_id = :id) > 0", nativeQuery = true)
    boolean estaEnUso(@Param("id") Long id);
}
