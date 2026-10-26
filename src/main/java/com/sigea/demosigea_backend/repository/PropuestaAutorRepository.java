package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.PropuestaAutorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de la relación {@link PropuestaAutor}.
 * <p>
 * Facilita operaciones CRUD y consultas sobre la tabla {@code propuestas_autores},
 * permitiendo listar y gestionar autores y ponentes por propuesta o por persona.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PropuestaAutorRepository extends JpaRepository<PropuestaAutor, PropuestaAutorId> {

    /**
     * Obtiene la lista de autores y ponentes de una propuesta ordenados según su orden de autoría.
     *
     * @param propuestaId ID de la propuesta académica.
     * @return Lista de autores ordenados ascendentemente.
     */
    List<PropuestaAutor> findByPropuesta_IdOrderByOrdenAsc(Long propuestaId);

    /**
     * Verifica si una persona ya se encuentra registrada como autor o ponente en una propuesta.
     *
     * @param propuestaId ID de la propuesta académica.
     * @param personaId   ID de la persona.
     * @return {@code true} si la persona ya está asociada a la propuesta, {@code false} de lo contrario.
     */
    boolean existsByPropuesta_IdAndPersona_Id(Long propuestaId, Long personaId);

    /**
     * Busca la vinculación específica de una persona a una propuesta.
     *
     * @param propuestaId ID de la propuesta académica.
     * @param personaId   ID de la persona.
     * @return {@link Optional} con el registro si existe.
     */
    Optional<PropuestaAutor> findByPropuesta_IdAndPersona_Id(Long propuestaId, Long personaId);

    /**
     * Cuenta el número de autores registrados en una propuesta.
     *
     * @param propuestaId ID de la propuesta académica.
     * @return Cantidad de autores vinculados.
     */
    long countByPropuesta_Id(Long propuestaId);

    /**
     * Obtiene todas las participaciones de autoría de una persona.
     *
     * @param personaId ID de la persona.
     * @return Lista de relaciones {@link PropuestaAutor}.
     */
    List<PropuestaAutor> findByPersona_Id(Long personaId);

    /**
     * Elimina la asociación de un autor con una propuesta específica.
     *
     * @param propuestaId ID de la propuesta.
     * @param personaId   ID de la persona a desvincular.
     */
    void deleteByPropuesta_IdAndPersona_Id(Long propuestaId, Long personaId);

    /**
     * Autores de varias propuestas en una sola consulta, con la persona ya cargada (HU-16). Evita una consulta
     * por propuesta al listar las propuestas de una convocatoria.
     *
     * @param propuestaIds IDs de las propuestas.
     * @return autores ordenados por propuesta y orden de autoría.
     */
    @Query("SELECT pa FROM PropuestaAutor pa JOIN FETCH pa.persona "
            + "WHERE pa.propuesta.id IN :propuestaIds ORDER BY pa.propuesta.id, pa.orden")
    List<PropuestaAutor> findByPropuestaIdsConPersona(@Param("propuestaIds") Collection<Long> propuestaIds);
}
