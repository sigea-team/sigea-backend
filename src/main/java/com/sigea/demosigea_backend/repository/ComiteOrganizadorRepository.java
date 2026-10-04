package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ComiteOrganizador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link ComiteOrganizador} (HU-06, RF05).
 * <p>
 * Los listados cargan la persona con {@code @EntityGraph} para construir la respuesta sin
 * consultas adicionales (N+1).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ComiteOrganizadorRepository extends JpaRepository<ComiteOrganizador, Long> {

    /** Criterio 2: indica si la persona ya tiene una participación vigente en el evento. */
    boolean existsByEvento_IdAndPersona_IdAndActivoTrue(Long eventoId, Long personaId);

    /** Comité vigente del evento, en orden de vinculación. */
    @EntityGraph(attributePaths = "persona")
    List<ComiteOrganizador> findByEvento_IdAndActivoTrueOrderByFechaAsignacionAscIdAsc(Long eventoId);

    /** Comité completo del evento (vigentes primero y luego el historial de retirados). */
    @EntityGraph(attributePaths = "persona")
    List<ComiteOrganizador> findByEvento_IdOrderByActivoDescFechaAsignacionAscIdAsc(Long eventoId);

    /** Busca una participación garantizando que pertenezca al evento indicado. */
    @EntityGraph(attributePaths = "persona")
    Optional<ComiteOrganizador> findByIdAndEvento_Id(Long id, Long eventoId);
}
