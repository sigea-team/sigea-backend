package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.GastoEjecutado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link GastoEjecutado} (HU-09).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface GastoEjecutadoRepository extends JpaRepository<GastoEjecutado, Long> {

    /**
     * Acumulado ejecutado de un rubro (Criterio 1).
     *
     * @param rubroId ID del rubro.
     * @return Suma de los gastos del rubro, o 0 si no tiene gastos (Criterio 4).
     */
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM GastoEjecutado g WHERE g.rubro.id = :rubroId")
    BigDecimal sumarPorRubro(@Param("rubroId") Long rubroId);

    /**
     * Número de gastos registrados en un rubro.
     *
     * @param rubroId ID del rubro.
     * @return Cantidad de gastos.
     */
    long countByRubro_Id(Long rubroId);

    /**
     * Total ejecutado del evento: suma de los gastos de todos sus rubros.
     *
     * @param eventoId ID del evento.
     * @return Total ejecutado, o 0 si no hay gastos (Criterio 4).
     */
    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM GastoEjecutado g WHERE g.rubro.evento.id = :eventoId")
    BigDecimal sumarPorEvento(@Param("eventoId") Long eventoId);

    /**
     * Acumulado ejecutado y número de gastos de cada rubro del evento que tiene gastos.
     * Los rubros sin gastos no aparecen: su acumulado es 0.
     *
     * @param eventoId ID del evento.
     * @return Una fila por rubro con gastos.
     */
    @Query("SELECT g.rubro.id AS rubroId, SUM(g.valor) AS totalEjecutado, COUNT(g) AS cantidadGastos "
            + "FROM GastoEjecutado g WHERE g.rubro.evento.id = :eventoId GROUP BY g.rubro.id")
    List<AcumuladoRubro> acumuladosPorRubro(@Param("eventoId") Long eventoId);

    /**
     * Gastos del evento, del más reciente al más antiguo.
     *
     * @param eventoId ID del evento.
     * @return Gastos con rubro y usuario cargados.
     */
    @EntityGraph(attributePaths = {"rubro", "registradoPor", "registradoPor.persona"})
    List<GastoEjecutado> findByRubro_Evento_IdOrderByFechaGastoDescIdDesc(Long eventoId);

    /**
     * Gastos de un rubro, del más reciente al más antiguo.
     *
     * @param rubroId ID del rubro.
     * @return Gastos con rubro y usuario cargados.
     */
    @EntityGraph(attributePaths = {"rubro", "registradoPor", "registradoPor.persona"})
    List<GastoEjecutado> findByRubro_IdOrderByFechaGastoDescIdDesc(Long rubroId);

    /**
     * Busca un gasto garantizando que pertenezca al evento indicado.
     *
     * @param id       ID del gasto.
     * @param eventoId ID del evento.
     * @return Gasto con rubro y usuario cargados.
     */
    @EntityGraph(attributePaths = {"rubro", "registradoPor", "registradoPor.persona"})
    Optional<GastoEjecutado> findByIdAndRubro_Evento_Id(Long id, Long eventoId);

    /**
     * Proyección del acumulado ejecutado de un rubro.
     */
    interface AcumuladoRubro {

        /** @return ID del rubro. */
        Long getRubroId();

        /** @return Suma de los gastos del rubro. */
        BigDecimal getTotalEjecutado();

        /** @return Número de gastos del rubro. */
        Long getCantidadGastos();
    }
}
