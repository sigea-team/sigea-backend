package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.repository.PermisoRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Servicio de negocio para la gestión integral de roles y sus permisos (HU-02).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    /**
     * Lista todos los roles con sus permisos y conteo de usuarios asignados.
     */
    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .map(rol -> {
                    // Cargar permisos
                    Rol rolConPermisos = rolRepository.findByIdWithPermisos(rol.getId()).orElse(rol);
                    long activos = rolRepository.countUsuariosActivosByRolId(rol.getId());
                    long total = rolRepository.countTotalUsuariosByRolId(rol.getId());
                    return RolResponse.fromEntity(rolConPermisos, activos, total);
                })
                .toList();
    }

    /**
     * Obtiene el detalle de un rol específico por su identificador.
     */
    @Transactional(readOnly = true)
    public RolResponse obtenerPorId(Long id) {
        Rol rol = rolRepository.findByIdWithPermisos(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol con ID: " + id));

        long activos = rolRepository.countUsuariosActivosByRolId(id);
        long total = rolRepository.countTotalUsuariosByRolId(id);
        return RolResponse.fromEntity(rol, activos, total);
    }

    /**
     * Criterio 1: Crea un nuevo rol y le asigna un conjunto de permisos.
     * Guarda el rol y lo deja disponible para asignarlo a usuarios.
     */
    @Transactional
    public RolResponse crearRol(RolRequest request) {
        String nombreNormalizado = request.nombre().trim().toUpperCase();

        if (rolRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new RecursoDuplicadoException("Ya existe un rol registrado con el nombre: " + nombreNormalizado);
        }

        Set<Permiso> permisosAsignados = resolverPermisos(request.permisosIds());

        Rol nuevoRol = Rol.builder()
                .nombre(nombreNormalizado)
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .permisos(permisosAsignados)
                .build();

        Rol rolGuardado = rolRepository.save(nuevoRol);
        log.info("Rol creado exitosamente: ID {}, Nombre {}", rolGuardado.getId(), rolGuardado.getNombre());

        return RolResponse.fromEntity(rolGuardado, 0, 0);
    }

    /**
     * Criterio 2: Modifica los datos y permisos de un rol existente.
     * Al modificar los permisos de este rol, el cambio aplica a todos los usuarios
     * que tengan ese rol (relación en roles_permisos).
     */
    @Transactional
    public RolResponse actualizarRol(Long id, RolRequest request) {
        Rol rol = rolRepository.findByIdWithPermisos(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol con ID: " + id));

        String nombreNormalizado = request.nombre().trim().toUpperCase();

        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new RecursoDuplicadoException("Ya existe otro rol registrado con el nombre: " + nombreNormalizado);
        }

        Set<Permiso> permisosActualizados = resolverPermisos(request.permisosIds());

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        rol.setPermisos(permisosActualizados);

        Rol rolActualizado = rolRepository.save(rol);
        log.info("Rol actualizado exitosamente: ID {}, Permisos asignados {}", rolActualizado.getId(), permisosActualizados.size());

        long activos = rolRepository.countUsuariosActivosByRolId(id);
        long total = rolRepository.countTotalUsuariosByRolId(id);
        return RolResponse.fromEntity(rolActualizado, activos, total);
    }

    /**
     * Criterio 3: Elimina un rol del sistema.
     * Si el rol tiene usuarios activos asignados, el sistema impide la eliminación
     * y sugiere reasignar primero a los usuarios.
     */
    @Transactional
    public void eliminarRol(Long id) {
        Rol rol = rolRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol con ID: " + id));

        long usuariosActivos = rolRepository.countUsuariosActivosByRolId(id);
        if (usuariosActivos > 0) {
            throw new OperacionNoPermitidaException(
                    String.format(
                            "No es posible eliminar el rol '%s' porque tiene %d usuario(s) activo(s) asignado(s). Por favor reasigne o desactive a los usuarios antes de eliminar el rol.",
                            rol.getNombre(), usuariosActivos
                    )
            );
        }

        // Si no tiene usuarios activos, se procede con la eliminación segura
        rolRepository.delete(rol);
        log.info("Rol eliminado exitosamente: ID {}, Nombre {}", id, rol.getNombre());
    }

    /**
     * Resuelve los objetos {@link Permiso} a partir de un conjunto de IDs.
     */
    private Set<Permiso> resolverPermisos(Set<Long> permisosIds) {
        if (permisosIds == null || permisosIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permiso> permisosEncontrados = permisoRepository.findAllById(permisosIds);
        if (permisosEncontrados.size() != permisosIds.size()) {
            Set<Long> encontradosIds = permisosEncontrados.stream().map(Permiso::getId).collect(java.util.stream.Collectors.toSet());
            Set<Long> faltantes = new HashSet<>(permisosIds);
            faltantes.removeAll(encontradosIds);
            throw new RecursoNoEncontradoException("Los siguientes IDs de permisos no existen en el sistema: " + faltantes);
        }
        return new HashSet<>(permisosEncontrados);
    }
}
