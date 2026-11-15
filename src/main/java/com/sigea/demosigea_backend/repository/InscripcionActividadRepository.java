package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.InscripcionActividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de inscripciones a actividades individuales (HU-23).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface InscripcionActividadRepository extends JpaRepository<InscripcionActividad, Long> {

    /**
     * Busca la inscripción de un participante a una actividad puntual.
     */
    Optional<InscripcionActividad> findByInscripcion_IdAndActividad_Id(Long inscripcionId, Long actividadId);

    /**
     * Verifica si una inscripción ya incluye una actividad (Criterio 4).
     */
    boolean existsByInscripcion_IdAndActividad_Id(Long inscripcionId, Long actividadId);

    /**
     * Cuenta la cantidad de inscritos actuales en una actividad para control de aforo (Criterio 2).
     */
    long countByActividad_Id(Long actividadId);

    /**
     * Lista todas las inscripciones a actividades correspondientes a una inscripción de evento.
     */
    @Query("SELECT ia FROM InscripcionActividad ia JOIN FETCH ia.actividad a LEFT JOIN FETCH a.sala WHERE ia.inscripcion.id = :inscripcionId ORDER BY a.fecha, a.horaInicio")
    List<InscripcionActividad> findByInscripcion_Id(@Param("inscripcionId") Long inscripcionId);

    /**
     * Lista todas las actividades a las que una persona está inscrita en un evento determinado.
     */
    @Query("SELECT ia FROM InscripcionActividad ia JOIN FETCH ia.actividad a LEFT JOIN FETCH a.sala WHERE ia.inscripcion.persona.id = :personaId AND ia.inscripcion.evento.id = :eventoId ORDER BY a.fecha, a.horaInicio")
    List<InscripcionActividad> findByPersonaIdAndEventoId(@Param("personaId") Long personaId, @Param("eventoId") Long eventoId);

    /**
     * Busca las actividades activas (no canceladas) en las que la persona está inscrita en una fecha específica,
     * para validar posibles traslapes de horario (Criterio 3).
     */
    @Query("SELECT ia FROM InscripcionActividad ia JOIN FETCH ia.actividad a LEFT JOIN FETCH a.sala " +
            "WHERE ia.inscripcion.persona.id = :personaId AND a.fecha = :fecha AND a.estado <> 'cancelada'")
    List<InscripcionActividad> findActivasPorPersonaYFecha(@Param("personaId") Long personaId, @Param("fecha") LocalDate fecha);

    /**
     * Lista los participantes inscritos en una actividad específica.
     */
    @Query("SELECT ia FROM InscripcionActividad ia JOIN FETCH ia.inscripcion ie JOIN FETCH ie.persona p WHERE ia.actividad.id = :actividadId ORDER BY ia.fechaInscripcion ASC")
    List<InscripcionActividad> findByActividad_IdOrderByFechaInscripcionAsc(@Param("actividadId") Long actividadId);

    /**
     * Obtiene los conteos de inscripciones por actividad en una sala (orden descendente).
     * El primer elemento representa el aforo mínimo requerido por las inscripciones vigentes (HU-Salas, Criterio 4).
     */
    @Query("SELECT COUNT(ia) FROM InscripcionActividad ia WHERE ia.actividad.sala.id = :salaId GROUP BY ia.actividad.id ORDER BY COUNT(ia) DESC")
    List<Long> findConteosInscripcionesPorActividadEnSala(@Param("salaId") Long salaId);
}
