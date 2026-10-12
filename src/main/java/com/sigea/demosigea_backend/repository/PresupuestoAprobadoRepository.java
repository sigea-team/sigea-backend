package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.EstadoPresupuestoAprobado;
import com.sigea.demosigea_backend.model.PresupuestoAprobado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link PresupuestoAprobado} (HU-08).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PresupuestoAprobadoRepository extends JpaRepository<PresupuestoAprobado, Long> {

    /**
     * Busca la versión de presupuesto de un evento según su estado (ej. la versión oficial 'aprobado').
     *
     * @param eventoId ID del evento.
     * @param estado   Estado del presupuesto.
     * @return Presupuesto si existe.
     */
    Optional<PresupuestoAprobado> findByEvento_IdAndEstado(Long eventoId, EstadoPresupuestoAprobado estado);

    /**
     * Consulta el historial completo de versiones de presupuestos de un evento ordenado
     * cronológicamente de forma descendente (más reciente primero).
     *
     * @param eventoId ID del evento.
     * @return Lista de versiones históricas y vigentes.
     */
    List<PresupuestoAprobado> findByEvento_IdOrderByFechaAprobacionDescIdDesc(Long eventoId);

    /**
     * Verifica si existe alguna versión aprobada previa para el evento.
     *
     * @param eventoId ID del evento.
     * @return {@code true} si existe al menos un registro.
     */
    boolean existsByEvento_Id(Long eventoId);
}
