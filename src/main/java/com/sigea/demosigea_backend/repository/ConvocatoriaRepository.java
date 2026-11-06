package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Convocatoria> findByEvento_IdOrderByIdDesc(Long eventoId);

    List<Convocatoria> findByEstadoOrderByIdDesc(EstadoConvocatoria estado);

    List<Convocatoria> findByEvento_IdAndEstadoOrderByIdDesc(Long eventoId, EstadoConvocatoria estado);

    List<Convocatoria> findAllByOrderByIdDesc();

    /** HU-11, Criterio 1: consulta pública de una convocatoria solo si está en el estado indicado. */
    Optional<Convocatoria> findByIdAndEstado(Long id, EstadoConvocatoria estado);

    /**
     * HU-18: convocatoria con bloqueo exclusivo hasta terminar la transacción. Lo toma el cambio de umbrales de
     * clasificación para que ninguna aplicación de la clasificación lea los umbrales mientras cambian.
     *
     * @param id ID de la convocatoria.
     * @return convocatoria bloqueada.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Convocatoria c WHERE c.id = :id")
    Optional<Convocatoria> findByIdParaActualizar(@Param("id") Long id);

    /**
     * HU-18: convocatoria con bloqueo compartido hasta terminar la transacción. Lo toman las operaciones que
     * clasifican propuestas con los umbrales de la convocatoria: pueden ejecutarse a la vez entre ellas, pero
     * esperan a que termine un cambio de umbrales (y este espera a que ellas terminen).
     *
     * @param id ID de la convocatoria.
     * @return convocatoria bloqueada en modo compartido, con los umbrales confirmados más recientes.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT c FROM Convocatoria c WHERE c.id = :id")
    Optional<Convocatoria> findByIdParaCompartir(@Param("id") Long id);
}
