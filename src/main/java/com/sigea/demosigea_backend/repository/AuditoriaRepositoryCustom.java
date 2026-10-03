package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.dto.auditoria.FiltroAuditoria;
import com.sigea.demosigea_backend.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Fragmento de consulta dinámica del log de auditoría (HU-03, Criterio 2).
 */
public interface AuditoriaRepositoryCustom {

    /**
     * Busca registros aplicando solo los filtros no nulos, ordenados del más reciente al más antiguo.
     */
    Page<Auditoria> buscar(FiltroAuditoria filtro, Pageable pageable);
}
