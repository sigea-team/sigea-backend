package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link Usuario}.
 * <p>
 * Proporciona operaciones CRUD y consultas especializadas para la autenticación,
 * verificación de credenciales y búsqueda de usuarios en la plataforma SIGEA.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Comprueba si existe un usuario en el sistema con el nombre de usuario especificado,
     * ignorando mayúsculas y minúsculas.
     *
     * @param nombreUsuario Nombre de usuario a verificar.
     * @return {@code true} si el nombre de usuario ya está registrado, {@code false} en caso contrario.
     */
    boolean existsByNombreUsuarioIgnoreCase(String nombreUsuario);

    /**
     * Busca un usuario por su nombre de usuario, sin diferenciar entre mayúsculas y minúsculas.
     *
     * @param nombreUsuario Nombre de usuario único del usuario.
     * @return Un {@link Optional} que contiene el {@link Usuario} si es localizado, o vacío si no existe.
     */
    Optional<Usuario> findByNombreUsuarioIgnoreCase(String nombreUsuario);

    /**
     * Busca un usuario mediante la dirección de correo electrónico asociada a su entidad {@code Persona}.
     *
     * @param correo Correo electrónico registrado de la persona.
     * @return Un {@link Optional} con el {@link Usuario} correspondiente, o vacío si no se encuentra match.
     */
    Optional<Usuario> findByPersona_CorreoIgnoreCase(String correo);

    /**
     * Busca un usuario que coincida con el identificador proporcionado, evaluándolo tanto
     * contra el nombre de usuario como contra el correo electrónico asociado.
     * <p>
     * Utilizado principalmente en el flujo de inicio de sesión para permitir el acceso con
     * cualquiera de las dos credenciales.
     * </p>
     *
     * @param identificador Nombre de usuario o dirección de correo electrónico ingresada.
     * @return Un {@link Optional} con el {@link Usuario} encontrado, o vacío si no coincide con ningún registro.
     */
    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE LOWER(u.nombreUsuario) = LOWER(:identificador) OR LOWER(p.correo) = LOWER(:identificador)")
    Optional<Usuario> findByIdentificador(@Param("identificador") String identificador);
}