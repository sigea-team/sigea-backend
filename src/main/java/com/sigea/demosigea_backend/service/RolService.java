package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.rol.RolRequest;
import com.sigea.demosigea_backend.dto.rol.RolResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Permiso;
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.PermisoRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión integral de roles y sus permisos (HU-02).
 * <p>
 * Contiene la lógica requerida para:
 * <ul>
 *   <li><b>Criterio 1:</b> Creación de roles y asignación de permisos sobre los módulos funcionales.</li>
 *   <li><b>Criterio 2:</b> Modificación de roles y permisos con impacto en cascada sobre los usuarios asignados.</li>
 *   <li><b>Criterio 3:</b> Validación previa a la eliminación impidiendo su borrado si existen usuarios activos asignados.</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see RolRepository
 * @see PermisoRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RolService {

    /** Nombre de la tabla afectada, usado en los registros de auditoría. */
    private static final String ENTIDAD_ROLES = "roles";

    /** Repositorio de persistencia para la entidad {@link Rol}. */
    private final RolRepository rolRepository;

    /** Repositorio de persistencia para la entidad {@link Permiso}. */
    private final PermisoRepository permisoRepository;

    /** Servicio de invalidación de tokens y sesiones activas. */
    private final com.sigea.demosigea_backend.security.TokenBlacklistService tokenBlacklistService;

    /** Servicio de auditoría de operaciones críticas (HU-03). */
    private final AuditoriaService auditoriaService;

    /**
     * Recupera el catálogo completo de roles registrados en el sistema, incorporando
     * sus permisos y el número de usuarios (tanto activos como globales) asignados.
     *
     * @return Lista de {@link RolResponse} con el balance de cada rol.
     */
    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .map(rol -> {
                    Rol rolConPermisos = rolRepository.findByIdWithPermisos(rol.getId()).orElse(rol);
                    long activos = rolRepository.countUsuariosActivosByRolId(rol.getId());
                    long total = rolRepository.countTotalUsuariosByRolId(rol.getId());
                    return RolResponse.fromEntity(rolConPermisos, activos, total);
                })
                .toList();
    }

    /**
     * Obtiene la información detallada de un rol específico a partir de su identificador.
     *
     * @param id Identificador numérico del rol.
     * @return DTO {@link RolResponse} con el rol y sus permisos.
     * @throws RecursoNoEncontradoException si no existe un rol con el ID suministrado.
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
     * Crea un nuevo rol en la base de datos y le asocia el conjunto de permisos especificados.
     * Cumple con el <b>Criterio 1</b> de la HU-02.
     *
     * @param request Datos del nuevo rol a registrar (nombre, descripción, permisosIds).
     * @return DTO {@link RolResponse} con el rol recién persistido.
     * @throws RecursoDuplicadoException si ya existe un rol con el mismo nombre.
     * @throws RecursoNoEncontradoException si alguno de los IDs de permisos indicados no existe.
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

        // HU-03: auditoría de la operación crítica (misma transacción)
        auditoriaService.registrar(TipoOperacionAuditoria.ROL_CREADO, ENTIDAD_ROLES, rolGuardado.getId(),
                DetalleAuditoria.de("despues", instantanea(rolGuardado)));

        return RolResponse.fromEntity(rolGuardado, 0, 0);
    }

    /**
     * Actualiza la información y permisos de un rol existente.
     * Cumple con el <b>Criterio 2</b> de la HU-02, aplicando el cambio a todos los usuarios que posean el rol.
     * <p>
     * NOTA IMPORTANTE - Sincronización de Permisos:
     * Cuando se modifica un rol, los usuarios con sesiones activas serán
     * forzados a re-autenticarse en la siguiente operación que requiera
     * autorización, garantizando efecto inmediato del cambio (Criterio 2).
     * </p>
     *
     * @param id Identificador del rol a modificar.
     * @param request Nuevos datos para el rol y nueva lista de permisos.
     * @return DTO {@link RolResponse} actualizado.
     * @throws RecursoNoEncontradoException si el rol o alguno de los permisos indicados no existen.
     * @throws RecursoDuplicadoException si se intenta renombrar a un nombre ya tomado por otro rol.
     */
    @Transactional
    public RolResponse actualizarRol(Long id, RolRequest request) {
        Rol rol = rolRepository.findByIdWithPermisos(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el rol con ID: " + id));

        String nombreNormalizado = request.nombre().trim().toUpperCase();

        // HU-03: se captura el estado previo para registrar el "antes" y el "después"
        Map<String, Object> estadoAnterior = instantanea(rol);

        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nombreNormalizado, id)) {
            throw new RecursoDuplicadoException("Ya existe otro rol registrado con el nombre: " + nombreNormalizado);
        }

        Set<Permiso> permisosActualizados = resolverPermisos(request.permisosIds());

        rol.setNombre(nombreNormalizado);
        rol.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        rol.setPermisos(permisosActualizados);
        rol.setFechaActualizacion(java.time.LocalDateTime.now());

        Rol rolActualizado = rolRepository.save(rol);
        log.info("Rol actualizado exitosamente: ID {}, Permisos asignados {}", rolActualizado.getId(), permisosActualizados.size());

        // Invalida sesiones activas para usuarios con este rol garantizando efecto inmediato (Criterio 2)
        tokenBlacklistService.invalidarSesionesDeRol(id);

        // HU-03: auditoría de la operación crítica "cambiar un rol"
        auditoriaService.registrar(TipoOperacionAuditoria.ROL_ACTUALIZADO, ENTIDAD_ROLES, id,
                DetalleAuditoria.de("antes", estadoAnterior, "despues", instantanea(rolActualizado)));

        long activos = rolRepository.countUsuariosActivosByRolId(id);
        long total = rolRepository.countTotalUsuariosByRolId(id);
        return RolResponse.fromEntity(rolActualizado, activos, total);
    }

    /**
     * Elimina un rol del sistema previa validación de usuarios activos.
     * Cumple con el <b>Criterio 3</b> de la HU-02, impidiendo la eliminación si existen cuentas activas asignadas.
     *
     * @param id Identificador numérico del rol a suprimir.
     * @throws RecursoNoEncontradoException si el rol no existe.
     * @throws OperacionNoPermitidaException si el rol tiene uno o más usuarios activos vinculados.
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

        // HU-03: se captura el rol antes de borrarlo para conservar evidencia de lo eliminado
        Map<String, Object> estadoEliminado = instantanea(rol);

        rolRepository.delete(rol);
        log.info("Rol eliminado exitosamente: ID {}, Nombre {}", id, rol.getNombre());

        auditoriaService.registrar(TipoOperacionAuditoria.ROL_ELIMINADO, ENTIDAD_ROLES, id,
                DetalleAuditoria.de("antes", estadoEliminado));
    }

    /**
     * Construye una fotografía del rol (nombre, descripción y códigos de permisos ordenados)
     * para guardarla como "datos afectados" en la auditoría (HU-03).
     */
    private Map<String, Object> instantanea(Rol rol) {
        List<String> codigos = rol.getPermisos() == null ? List.of() : rol.getPermisos().stream()
                .map(Permiso::getCodigo)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        return DetalleAuditoria.de(
                "nombre", rol.getNombre(),
                "descripcion", rol.getDescripcion(),
                "permisos", codigos
        );
    }

    /**
     * Resuelve y valida la existencia en base de datos de una colección de identificadores de permisos.
     *
     * @param permisosIds Colección de claves primarias de permisos.
     * @return Conjunto de entidades {@link Permiso} correspondientes.
     * @throws RecursoNoEncontradoException si uno o más IDs no se hallan en la base de datos.
     */
    private Set<Permiso> resolverPermisos(Set<Long> permisosIds) {
        if (permisosIds == null || permisosIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Permiso> permisosEncontrados = permisoRepository.findAllById(permisosIds);
        if (permisosEncontrados.size() != permisosIds.size()) {
            Set<Long> encontradosIds = permisosEncontrados.stream().map(Permiso::getId).collect(Collectors.toSet());
            Set<Long> faltantes = new HashSet<>(permisosIds);
            faltantes.removeAll(encontradosIds);
            throw new RecursoNoEncontradoException("Los siguientes IDs de permisos no existen en el sistema: " + faltantes);
        }
        return new HashSet<>(permisosEncontrados);
    }
}
