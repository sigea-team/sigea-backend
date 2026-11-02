package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.VersionPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link VersionPropuesta} (HU-14).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface VersionPropuestaRepository extends JpaRepository<VersionPropuesta, Long> {

    /** Historial completo de versiones de una propuesta, de la más antigua a la más reciente (Criterio 4). */
    List<VersionPropuesta> findByPropuesta_IdOrderByNumeroVersionAsc(Long propuestaId);

    /** Última versión registrada de la propuesta. */
    Optional<VersionPropuesta> findTopByPropuesta_IdOrderByNumeroVersionDesc(Long propuestaId);

    /** Versión específica de una propuesta por su número. */
    Optional<VersionPropuesta> findByPropuesta_IdAndNumeroVersion(Long propuestaId, Integer numeroVersion);

    /** Indica si la propuesta ya tiene una versión definitiva aprobada (Criterio 5). */
    boolean existsByPropuesta_IdAndEsDefinitivaTrue(Long propuestaId);

    /** Primera versión de la propuesta (la del envío inicial). */
    Optional<VersionPropuesta> findFirstByPropuesta_IdOrderByNumeroVersionAsc(Long propuestaId);
}
