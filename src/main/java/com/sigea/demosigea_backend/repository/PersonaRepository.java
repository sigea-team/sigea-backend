package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Persona}.
 * <p>
 * Proporciona métodos para realizar operaciones CRUD y consultas personalizadas sobre la tabla de personas,
 * optimizando las búsquedas por correo electrónico y número de documento de identidad.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Verifica si existe alguna persona registrada con el correo electrónico especificado,
     * ignorando diferencias entre mayúsculas y minúsculas.
     *
     * @param correo Dirección de correo electrónico a verificar.
     * @return {@code true} si existe un registro con el correo dado, {@code false} en caso contrario.
     */
    boolean existsByCorreoIgnoreCase(String correo);

    /**
     * Verifica la existencia de una persona registrada mediante su número de documento de identidad.
     *
     * @param numeroDocumento Número de documento único a consultar.
     * @return {@code true} si el número de documento ya está registrado, {@code false} de lo contrario.
     */
    boolean existsByNumeroDocumento(String numeroDocumento);

    /**
     * Busca una persona por su dirección de correo electrónico, sin diferenciar mayúsculas de minúsculas.
     *
     * @param correo Dirección de correo electrónico asociada.
     * @return Un {@link Optional} que contiene la {@link Persona} si se encuentra, o un contenedor vacío si no.
     */
    Optional<Persona> findByCorreoIgnoreCase(String correo);

    /**
     * Busca una persona mediante su número de documento de identidad.
     *
     * @param numeroDocumento Número de documento registrado.
     * @return Un {@link Optional} con la {@link Persona} encontrada, o vacío si no existe match.
     */
    Optional<Persona> findByNumeroDocumento(String numeroDocumento);
}