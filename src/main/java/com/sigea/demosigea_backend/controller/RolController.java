package com.sigea.demosigea_backend.controller;

import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de roles y asignación de permisos (HU-02).
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    /**
     * Consulta general de roles del sistema.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(rolService.listarRoles());
    }

    /**
     * Consulta detallada de un rol específico.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_VER', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtenerPorId(id));
    }

    /**
     * Criterio 1: Crear nuevo rol y asignarle permisos sobre los módulos.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLES_CREAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> crearRol(@Valid @RequestBody RolRequest request) {
        RolResponse nuevoRol = rolService.crearRol(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRol);
    }

    /**
     * Criterio 2: Modificar un rol y actualizar sus permisos.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_EDITAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<RolResponse> actualizarRol(
            @PathVariable Long id,
            @Valid @RequestBody RolRequest request
    ) {
        RolResponse rolActualizado = rolService.actualizarRol(id, request);
        return ResponseEntity.ok(rolActualizado);
    }

    /**
     * Criterio 3: Eliminar un rol (con validación de usuarios activos asignados).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLES_ELIMINAR', 'ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> eliminarRol(@PathVariable Long id) {
        rolService.eliminarRol(id);
        return ResponseEntity.ok(Map.of(
                "mensaje", "Rol eliminado exitosamente.",
                "rolId", String.valueOf(id)
        ));
    }
}
