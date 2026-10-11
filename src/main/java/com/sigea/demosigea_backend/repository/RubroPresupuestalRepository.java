package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.RubroPresupuestal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link RubroPresupuestal}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface RubroPresupuestalRepository extends JpaRepository<RubroPresupuestal, Long> {

    /**
     * Consulta los rubros presupuestales activos asignados a un evento.
     *
     * @param eventoId ID del evento.
     * @return Lista de rubros activos.
     */
    List<RubroPresupuestal> findByEvento_IdAndActivoTrue(Long eventoId);

    /**
     * Determina si existen rubros presupuestales activos proyectados para el evento.
     * Sirve para verificar la precondición del presupuesto preliminar (HU-08, Criterio 1).
     *
     * @param eventoId ID del evento.
     * @return {@code true} si existe al menos un rubro activo.
     */
    boolean existsByEvento_IdAndActivoTrue(Long eventoId);

    /**
     * Calcula la sumatoria del presupuesto preliminar proyectado para un evento
     * a partir de sus rubros activos (cantidad * valor_unitario_proyectado).
     *
     * @param eventoId ID del evento.
     * @return Total preliminar estimado, o 0 si no hay rubros activos.
     */
    @Query("SELECT COALESCE(SUM(r.cantidad * r.valorUnitarioProyectado), 0) " +
           "FROM RubroPresupuestal r " +
           "WHERE r.evento.id = :eventoId AND r.activo = true")
    BigDecimal calcularTotalPreliminarPorEvento(@Param("eventoId") Long eventoId);
}
