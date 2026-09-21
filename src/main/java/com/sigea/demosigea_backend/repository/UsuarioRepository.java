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
}