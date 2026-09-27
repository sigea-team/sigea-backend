package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Rol}.
 * <p>
 * Proporciona métodos para realizar operaciones CRUD y consultas personalizadas sobre la
 * tabla de roles del sistema SIGEA.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * Busca un rol en el sistema por su nombre, ignorando diferencias entre mayúsculas y minúsculas.
     *
     * @param nombre Nombre del rol a consultar (ej. "ROLE_ADMIN", "ROLE_DOCENTE").
     * @return Un {@link Optional} que contiene el {@link Rol} si fue encontrado, o un contenedor vacío si no existe.
     */
    Optional<Rol> findByNombreIgnoreCase(String nombre);

    /**
     * Verifica si ya existe un rol con el nombre dado.
     */
    boolean existsByNombreIgnoreCase(String nombre);

    /**
     * Verifica si existe otro rol con el mismo nombre excluyendo un ID específico (para updates).
     */
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    /**
     * Obtiene un rol con sus permisos cargados tempranamente.
     */
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Rol r LEFT JOIN FETCH r.permisos WHERE r.id = :id")
    Optional<Rol> findByIdWithPermisos(@org.springframework.data.repository.query.Param("id") Long id);

    /**
     * Cuenta cuántos usuarios activos tienen asignado este rol.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId AND u.estado = com.sigea.demosigea_backend.model.EstadoUsuario.activo")
    long countUsuariosActivosByRolId(@org.springframework.data.repository.query.Param("rolId") Long rolId);

    /**
     * Cuenta cuántos usuarios en total (cualquier estado) tienen asignado este rol.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) FROM Usuario u JOIN u.roles r WHERE r.id = :rolId")
    long countTotalUsuariosByRolId(@org.springframework.data.repository.query.Param("rolId") Long rolId);
}