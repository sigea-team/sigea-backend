package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.HistorialRubro;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio del historial de modificaciones de rubros presupuestales (HU-07, Criterio 2).
 * <p>
 * Los listados cargan el usuario y su persona con {@code @EntityGraph} para mostrar quién hizo
 * cada cambio sin consultas adicionales (N+1).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface HistorialRubroRepository extends JpaRepository<HistorialRubro, Long> {

    /** Historial completo del presupuesto preliminar del evento, del cambio más reciente al más antiguo. */
    @EntityGraph(attributePaths = {"usuario", "usuario.persona"})
    List<HistorialRubro> findByEvento_IdOrderByFechaHoraDescIdDesc(Long eventoId);

    /** Historial de un rubro, del cambio más reciente al más antiguo. */
    @EntityGraph(attributePaths = {"usuario", "usuario.persona"})
    List<HistorialRubro> findByRubro_IdOrderByFechaHoraDescIdDesc(Long rubroId);
}
