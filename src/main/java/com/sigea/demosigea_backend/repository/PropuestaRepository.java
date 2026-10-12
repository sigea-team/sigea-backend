package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Propuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para {@link Propuesta}.
 * <p>
 * En la HU-05 solo se usa el conteo para el Criterio 2. HU-14 y siguientes agregarán aquí las
 * consultas de recepción y evaluación.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PropuestaRepository extends JpaRepository<Propuesta, Long> {

    /** HU-05, Criterio 2: propuestas postuladas en la línea temática. */
    long countByLineaTematica_Id(Long lineaTematicaId);
}
