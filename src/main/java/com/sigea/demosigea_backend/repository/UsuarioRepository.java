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
     * Busca un usuario que coincida con el identificador proporcionado.
     * <p>
     * Como defensa en profundidad: si el identificador contiene '@', busca por correo electrónico;
     * si no contiene '@', busca por nombre de usuario. Esto evita la ambigüedad y previene
     * errores de consulta múltiple cuando un nombre de usuario pueda coincidir con el correo de otra persona.
     * </p>
     *
     * @param identificador Nombre de usuario o dirección de correo electrónico ingresada.
     * @return Un {@link Optional} con el {@link Usuario} encontrado, o vacío si no coincide ningún registro.
     */
    default Optional<Usuario> findByIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            return Optional.empty();
        }
        String idTrim = identificador.trim();
        if (idTrim.contains("@")) {
            return findByPersona_CorreoIgnoreCase(idTrim);
        } else {
            return findByNombreUsuarioIgnoreCase(idTrim);
        }
    }
}