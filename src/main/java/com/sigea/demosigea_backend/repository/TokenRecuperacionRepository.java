package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.TipoToken;
import com.sigea.demosigea_backend.model.TokenRecuperacion;
import com.sigea.demosigea_backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión del acceso a datos de la entidad {@link TokenRecuperacion}.
 * <p>
 * Facilita las operaciones de persistencia y consulta para el manejo del ciclo de vida
 * de tokens de verificación de correo y recuperación de credenciales en SIGEA.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    /**
     * Busca un token específico filtrando por su valor de cadena y por su propósito/tipo.
     *
     * @param token Cadena de caracteres única correspondiente al token enviado al usuario.
     * @param tipo  Tipo o propósito del token (ej. {@code VERIFICACION_CORREO}, {@code RECUPERACION_PASSWORD}).
     * @return Un {@link Optional} que contiene el {@link TokenRecuperacion} si coincide la búsqueda, o un contenedor vacío si no existe.
     */
    Optional<TokenRecuperacion> findByTokenAndTipo(String token, TipoToken tipo);

    /**
     * Obtiene la lista de tokens activos (no utilizados) asociados a un usuario y a un tipo particular.
     * <p>
     * Utilizado comúnmente para invalidar o desechar tokens antiguos sin usar antes de generar uno nuevo para el mismo propósito.
     * </p>
     *
     * @param usuario Entidad {@link Usuario} propietaria de los tokens.
     * @param tipo    Tipo o propósito del token a consultar.
     * @return Una {@link List} con los {@link TokenRecuperacion} que aún no han sido consumidos ({@code usado = false}).
     */
    List<TokenRecuperacion> findByUsuarioAndTipoAndUsadoFalse(Usuario usuario, TipoToken tipo);
}