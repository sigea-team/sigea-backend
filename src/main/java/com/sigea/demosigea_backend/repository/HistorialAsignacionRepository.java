package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.HistorialAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio del historial de asignaciones de evaluación (HU-16, Criterio 3).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface HistorialAsignacionRepository extends JpaRepository<HistorialAsignacion, Long> {

    /**
     * Historial de asignaciones y reasignaciones de una propuesta, del más reciente al más antiguo, con los
     * evaluadores y el usuario ya cargados.
     *
     * @param propuestaId ID de la propuesta.
     * @return movimientos registrados.
     */
    @Query("SELECT h FROM HistorialAsignacion h "
            + "LEFT JOIN FETCH h.evaluadorAnterior evAnt LEFT JOIN FETCH evAnt.persona "
            + "JOIN FETCH h.evaluadorNuevo evNue JOIN FETCH evNue.persona "
            + "LEFT JOIN FETCH h.usuario u LEFT JOIN FETCH u.persona "
            + "WHERE h.propuesta.id = :propuestaId "
            + "ORDER BY h.fechaHora DESC, h.id DESC")
    List<HistorialAsignacion> findHistorialDePropuesta(@Param("propuestaId") Long propuestaId);
}
