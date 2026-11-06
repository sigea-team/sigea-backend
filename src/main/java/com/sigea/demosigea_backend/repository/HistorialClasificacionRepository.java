package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.HistorialClasificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link HistorialClasificacion} (HU-18, Criterio 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface HistorialClasificacionRepository extends JpaRepository<HistorialClasificacion, Long> {

    /**
     * Historial de clasificaciones de una propuesta, con el usuario y su persona ya cargados.
     *
     * @param propuestaId ID de la propuesta.
     * @return movimientos del más reciente al más antiguo.
     */
    @Query("SELECT h FROM HistorialClasificacion h "
            + "LEFT JOIN FETCH h.usuario u LEFT JOIN FETCH u.persona "
            + "WHERE h.propuesta.id = :propuestaId ORDER BY h.fechaHora DESC, h.id DESC")
    List<HistorialClasificacion> findDetalleByPropuestaId(@Param("propuestaId") Long propuestaId);
}
