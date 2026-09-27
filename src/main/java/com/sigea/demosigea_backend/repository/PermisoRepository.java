package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Permiso}.
 * <p>
 * Proporciona métodos CRUD y consultas de consulta especializadas sobre la tabla {@code permisos},
 * permitiendo la búsqueda por código único y la recuperación ordenada o agrupada por módulo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see Permiso
 */
@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    /**
     * Busca un permiso por su código identificador único, ignorando mayúsculas y minúsculas.
     *
     * @param codigo Código alfanumérico del permiso (ej. "ROLES_CREAR").
     * @return {@link Optional} con el permiso si existe, o vacío en caso contrario.
     */
    Optional<Permiso> findByCodigoIgnoreCase(String codigo);

    /**
     * Obtiene la lista de permisos asociados a un módulo funcional específico ordenados alfabéticamente por código.
     *
     * @param modulo Nombre del módulo funcional (ej. "ROLES", "EVENTOS").
     * @return Lista de permisos correspondientes al módulo.
     */
    List<Permiso> findByModuloIgnoreCaseOrderByCodigoAsc(String modulo);

    /**
     * Recupera todos los permisos del sistema ordenados ascendentemente por módulo y luego por código.
     *
     * @return Lista exhaustiva del catálogo de permisos ordenado.
     */
    List<Permiso> findAllByOrderByModuloAscCodigoAsc();
}
