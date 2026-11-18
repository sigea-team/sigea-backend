package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Encuesta;
import com.sigea.demosigea_backend.model.EstadoEncuesta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las encuestas de satisfacción (HU-25).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface EncuestaRepository extends JpaRepository<Encuesta, Long> {

    /** Encuestas de un evento con su actividad, de la más reciente a la más antigua. */
    @Query("SELECT e FROM Encuesta e LEFT JOIN FETCH e.actividad WHERE e.evento.id = :eventoId "
            + "ORDER BY e.fechaCreacion DESC, e.id DESC")
    List<Encuesta> findByEventoConActividad(@Param("eventoId") Long eventoId);

    /** Encuesta de un evento con su evento y su actividad. */
    @Query("SELECT e FROM Encuesta e JOIN FETCH e.evento LEFT JOIN FETCH e.actividad "
            + "WHERE e.id = :id AND e.evento.id = :eventoId")
    Optional<Encuesta> findByIdAndEvento_Id(@Param("id") Long id, @Param("eventoId") Long eventoId);

    /** Encuesta con su evento y su actividad (vista del participante). */
    @Query("SELECT e FROM Encuesta e JOIN FETCH e.evento LEFT JOIN FETCH e.actividad WHERE e.id = :id")
    Optional<Encuesta> findByIdConEvento(@Param("id") Long id);

    /**
     * Encuesta bloqueada en exclusiva (SELECT ... FOR UPDATE) para editarla, publicarla, cerrarla o eliminarla.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Encuesta e WHERE e.id = :id AND e.evento.id = :eventoId")
    Optional<Encuesta> findByIdAndEvento_IdParaActualizar(@Param("id") Long id, @Param("eventoId") Long eventoId);

    /**
     * Encuesta bloqueada en modo compartido (SELECT ... FOR SHARE) mientras se guardan respuestas: varias personas
     * responden a la vez, pero un cierre espera a que terminen y las respuestas posteriores ven la encuesta cerrada.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT e FROM Encuesta e WHERE e.id = :id")
    Optional<Encuesta> findByIdParaResponder(@Param("id") Long id);

    /**
     * Criterios 1 y 3: encuestas en los estados indicados habilitadas para la persona, es decir, de eventos o
     * actividades a las que asistió (HU-24). La del evento en general exige asistencia a alguna actividad del evento;
     * la de una actividad, asistencia a esa actividad.
     */
    @Query("SELECT e FROM Encuesta e JOIN FETCH e.evento LEFT JOIN FETCH e.actividad "
            + "WHERE e.estado IN :estados AND EXISTS ("
            + "  SELECT a.id FROM Asistencia a "
            + "  WHERE a.inscripcion.persona.id = :personaId AND a.inscripcion.evento = e.evento "
            + "    AND (e.actividad IS NULL OR a.actividad = e.actividad)) "
            + "ORDER BY e.fechaPublicacion DESC, e.id DESC")
    List<Encuesta> findHabilitadasParaPersona(@Param("personaId") Long personaId,
                                              @Param("estados") Collection<EstadoEncuesta> estados);
}
