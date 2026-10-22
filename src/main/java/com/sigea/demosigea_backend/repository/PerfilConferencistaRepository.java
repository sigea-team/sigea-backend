package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.PerfilConferencista;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de la trayectoria (perfil) de los conferencistas (HU-12).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PerfilConferencistaRepository extends JpaRepository<PerfilConferencista, Long> {

    /** Perfil de una persona (hay como máximo uno por persona). */
    Optional<PerfilConferencista> findByPersona_Id(Long personaId);

    /** Indica si la persona ya tiene su trayectoria registrada. */
    boolean existsByPersona_Id(Long personaId);

    /** Perfiles de varias personas a la vez (listados de un evento, sin consultas por fila). */
    List<PerfilConferencista> findByPersona_IdIn(Collection<Long> personaIds);

    /**
     * Conferencistas registrados (personas con trayectoria) cuyo nombre, documento o correo contiene
     * el patrón. Con {@code patron = "%"} devuelve todos. Se usa para elegir a quién vincular a un evento.
     *
     * @param patron   Patrón LIKE en minúsculas (ej. {@code "%perez%"}).
     * @param pageable Límite de resultados.
     */
    @Query("SELECT p FROM PerfilConferencista p JOIN FETCH p.persona per "
            + "WHERE LOWER(CONCAT(per.nombres, ' ', per.apellidos)) LIKE :patron "
            + "OR LOWER(per.numeroDocumento) LIKE :patron OR LOWER(per.correo) LIKE :patron "
            + "ORDER BY per.nombres ASC, per.apellidos ASC, per.id ASC")
    List<PerfilConferencista> buscarRegistrados(@Param("patron") String patron, Pageable pageable);
}
