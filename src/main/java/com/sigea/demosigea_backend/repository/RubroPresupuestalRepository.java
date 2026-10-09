package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.RubroPresupuestal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link RubroPresupuestal} (HU-07).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface RubroPresupuestalRepository extends JpaRepository<RubroPresupuestal, Long> {

    /** Rubros vigentes del presupuesto preliminar, en orden de registro. */
    List<RubroPresupuestal> findByEvento_IdAndActivoTrueOrderByIdAsc(Long eventoId);

    /** Todos los rubros del evento: vigentes primero y luego los eliminados. */
    List<RubroPresupuestal> findByEvento_IdOrderByActivoDescIdAsc(Long eventoId);

    /** Busca un rubro garantizando que pertenezca al evento indicado. */
    Optional<RubroPresupuestal> findByIdAndEvento_Id(Long id, Long eventoId);

    /** Indica si ya existe un rubro vigente con ese nombre en el evento (sin distinguir mayúsculas). */
    boolean existsByEvento_IdAndNombreIgnoreCaseAndActivoTrue(Long eventoId, String nombre);

    /** Igual que el anterior, excluyendo el rubro que se está editando. */
    boolean existsByEvento_IdAndNombreIgnoreCaseAndActivoTrueAndIdNot(Long eventoId, String nombre, Long id);

    /**
     * Indica si el evento ya tiene un presupuesto aprobado (HU-08). Una vez aprobado, el preliminar
     * queda congelado (flujo alterno de CU-29).
     * <p>
     * Se consulta con SQL nativo para no crear aquí la entidad {@code PresupuestoAprobado}, que le
     * corresponde a HU-08 y se desarrolla en paralelo.
     * </p>
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM presupuestos_aprobados "
            + "WHERE evento_id = :eventoId AND estado = 'aprobado')", nativeQuery = true)
    boolean existePresupuestoAprobado(@Param("eventoId") Long eventoId);
}
