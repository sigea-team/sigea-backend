package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Convocatoria}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    /**
     * Lista las convocatorias pertenecientes a un evento.
     */
    List<Convocatoria> findByEvento_IdOrderByIdAsc(Long eventoId);

    /**
     * Lista todas las convocatorias del sistema ordenadas descendentemente por ID.
     */
    List<Convocatoria> findAllByOrderByIdDesc();

    /**
     * Filtra convocatorias por estado.
     */
    List<Convocatoria> findByEstadoOrderByIdDesc(EstadoConvocatoria estado);

    /**
     * Filtra convocatorias por evento y estado.
     */
    List<Convocatoria> findByEvento_IdAndEstadoOrderByIdAsc(Long eventoId, EstadoConvocatoria estado);

    /**
     * Busca una convocatoria por ID y evento.
     */
    Optional<Convocatoria> findByIdAndEvento_Id(Long id, Long eventoId);
}
