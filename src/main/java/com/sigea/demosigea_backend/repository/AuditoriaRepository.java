package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Auditoria;
import org.springframework.data.repository.Repository;

import java.util.Optional;

/**
 * Repositorio del log de auditoría (HU-03).
 * <p>
 * Extiende intencionalmente de {@link Repository} (y no de {@code JpaRepository}) para exponer
 * únicamente inserción y consulta. Así no existe ningún método {@code delete*}, {@code deleteAll}
 * o {@code saveAll} que permita alterar el historial desde el código (Criterio 3).
 * </p>
 */
@org.springframework.stereotype.Repository
public interface AuditoriaRepository extends Repository<Auditoria, Long>, AuditoriaRepositoryCustom {

    /** Inserta un nuevo registro de auditoría. */
    Auditoria save(Auditoria auditoria);

    /** Consulta un registro puntual. */
    Optional<Auditoria> findById(Long id);
}
