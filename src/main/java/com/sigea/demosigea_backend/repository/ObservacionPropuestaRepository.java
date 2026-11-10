package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ObservacionPropuesta;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de {@link ObservacionPropuesta} (HU-19).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ObservacionPropuestaRepository extends JpaRepository<ObservacionPropuesta, Long> {

    /**
     * Observaciones de todas las versiones de una propuesta, en orden de registro, con la versión observada, la
     * versión de atención y quién las registró o resolvió.
     */
    @Query("SELECT o FROM ObservacionPropuesta o "
            + "JOIN FETCH o.version v "
            + "LEFT JOIN FETCH o.versionAtencion "
            + "LEFT JOIN FETCH o.usuario u LEFT JOIN FETCH u.persona "
            + "LEFT JOIN FETCH o.resueltoPor r LEFT JOIN FETCH r.persona "
            + "WHERE v.propuesta.id = :propuestaId "
            + "ORDER BY o.fecha ASC, o.id ASC")
    List<ObservacionPropuesta> findDePropuesta(@Param("propuestaId") Long propuestaId);

    /** Observación de una propuesta por su ID. */
    Optional<ObservacionPropuesta> findByIdAndVersion_Propuesta_Id(Long id, Long propuestaId);

    /**
     * Criterio 2: vincula a la nueva versión los ajustes requeridos sin resolver de las versiones anteriores de la
     * propuesta (los que la originaron). Si uno ya estaba vinculado a una versión anterior, pasa a la nueva.
     *
     * @return número de observaciones vinculadas.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE ObservacionPropuesta o SET o.versionAtencion = :nueva "
            + "WHERE o.resuelto = false AND o.requiereAjuste = true "
            + "AND o.version.id IN (SELECT v.id FROM VersionPropuesta v "
            + "    WHERE v.propuesta.id = :propuestaId AND v.numeroVersion < :numeroVersion)")
    int vincularPendientesAVersion(@Param("propuestaId") Long propuestaId,
                                   @Param("numeroVersion") Integer numeroVersion,
                                   @Param("nueva") VersionPropuesta nueva);
}
