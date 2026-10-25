package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Usuario}.
 * <p>
 * Proporciona operaciones CRUD y consultas especializadas para la autenticación,
 * verificación de credenciales y búsqueda de usuarios en la plataforma SIGEA.
 * La autenticación se realiza exclusivamente a través del correo electrónico,
 * dado que la columna {@code nombre_usuario} fue eliminada de la tabla {@code usuarios}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario mediante la dirección de correo electrónico asociada a su entidad {@code Persona}.
     *
     * @param correo Correo electrónico registrado de la persona.
     * @return Un {@link Optional} con el {@link Usuario} correspondiente, o vacío si no se encuentra match.
     */
    Optional<Usuario> findByPersona_CorreoIgnoreCase(String correo);

    /**
     * Indica si la persona tiene una cuenta de usuario. Se usa en HU-12 para no cambiar desde el
     * módulo de conferencistas el correo con el que esa persona inicia sesión.
     *
     * @param personaId ID de la persona.
     * @return {@code true} si existe una cuenta vinculada a la persona.
     */
    boolean existsByPersona_Id(Long personaId);
}