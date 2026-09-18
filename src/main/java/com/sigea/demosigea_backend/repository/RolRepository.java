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
}