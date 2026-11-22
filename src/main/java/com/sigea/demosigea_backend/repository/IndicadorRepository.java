package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Indicador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Indicadores de gestión de los eventos (tabla {@code indicadores}). En HU-28 se calculan al cierre formal.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public interface IndicadorRepository extends JpaRepository<Indicador, Long> {

    List<Indicador> findByEvento_IdOrderByIdAsc(Long eventoId);

    /** Indicadores con los códigos indicados de varios eventos (listado de la memoria histórica, sin N+1). */
    List<Indicador> findByEvento_IdInAndCodigoInOrderByIdAsc(Collection<Long> eventoIds, Collection<String> codigos);
}
