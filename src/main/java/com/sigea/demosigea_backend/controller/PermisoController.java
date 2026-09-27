package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.permiso.PermisoResponse;
import com.sigea.demosigea_backend.service.PermisoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la consulta de permisos del sistema SIGEA.
 */
@RestController
@RequestMapping("/api/v1/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoService permisoService;

    /**
     * Retorna el catálogo de permisos. Si agrupado=true, los agrupa por módulo.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLES_CREAR', 'ROLES_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<?> listarPermisos(
            @RequestParam(name = "agrupado", defaultValue = "false") boolean agrupado
    ) {
        if (agrupado) {
            Map<String, List<PermisoResponse>> agrupados = permisoService.listarAgrupadosPorModulo();
            return ResponseEntity.ok(agrupados);
        }
        List<PermisoResponse> permisos = permisoService.listarTodos();
        return ResponseEntity.ok(permisos);
    }
}
