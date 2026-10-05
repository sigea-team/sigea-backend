package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link Convocatoria}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    List<Convocatoria> findByEvento_IdOrderByIdDesc(Long eventoId);

    List<Convocatoria> findByEstadoOrderByIdDesc(EstadoConvocatoria estado);

    List<Convocatoria> findByEvento_IdAndEstadoOrderByIdDesc(Long eventoId, EstadoConvocatoria estado);

    List<Convocatoria> findAllByOrderByIdDesc();
}
