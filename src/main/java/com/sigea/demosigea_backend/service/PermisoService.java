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
 * Servicio de negocio para la consulta y catalogación de permisos del sistema SIGEA.
 * <p>
 * Brinda acceso al catálogo de permisos atómicos definidos en la base de datos,
 * soportando la visualización lineal o agrupada por módulo funcional para la interfaz
 * de asignación de permisos sobre los roles.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see PermisoRepository
 * @see PermisoResponse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermisoService {

    /** Repositorio de acceso a datos para permisos. */
    private final PermisoRepository permisoRepository;

    /**
     * Recupera todos los permisos del sistema ordenados alfabéticamente por módulo y código.
     *
     * @return Lista de DTOs {@link PermisoResponse} con la totalidad de los permisos registrados.
     */
    @Transactional(readOnly = true)
    public List<PermisoResponse> listarTodos() {
        return permisoRepository.findAllByOrderByModuloAscCodigoAsc().stream()
                .map(PermisoResponse::fromEntity)
                .toList();
    }

    /**
     * Recupera y agrupa los permisos del sistema por el nombre de su módulo funcional.
     *
     * @return Mapa asociativo donde la clave es el nombre del módulo (ej. "ROLES", "EVENTOS")
     *         y el valor es la lista de permisos correspondientes a dicho módulo.
     */
    @Transactional(readOnly = true)
    public Map<String, List<PermisoResponse>> listarAgrupadosPorModulo() {
        return listarTodos().stream()
                .collect(Collectors.groupingBy(PermisoResponse::modulo));
    }
}
