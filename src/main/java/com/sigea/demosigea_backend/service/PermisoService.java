package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para consulta y catálogo de permisos del sistema SIGEA.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermisoService {

    private final PermisoRepository permisoRepository;

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarTodos() {
        return permisoRepository.findAllByOrderByModuloAscCodigoAsc().stream()
                .map(PermisoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<PermisoResponse>> listarAgrupadosPorModulo() {
        return listarTodos().stream()
                .collect(Collectors.groupingBy(PermisoResponse::modulo));
    }
}
