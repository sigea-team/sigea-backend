package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las asistencias registradas a las actividades (HU-24).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    /** Asistencia ya registrada de una inscripción en una actividad (Criterio 3). */
    Optional<Asistencia> findByInscripcion_IdAndActividad_Id(Long inscripcionId, Long actividadId);

    /** Verifica si la inscripción ya tiene asistencia en la actividad. */
    boolean existsByInscripcion_IdAndActividad_Id(Long inscripcionId, Long actividadId);

    /** Verifica si la inscripción al evento tiene alguna asistencia registrada. */
    boolean existsByInscripcion_Id(Long inscripcionId);

    /** Cantidad de asistentes registrados en una actividad. */
    long countByActividad_Id(Long actividadId);

    /** Asistencias de una actividad con los datos del participante, en orden de llegada. */
    @Query("SELECT a FROM Asistencia a JOIN FETCH a.inscripcion i JOIN FETCH i.persona "
            + "WHERE a.actividad.id = :actividadId ORDER BY a.fechaHoraRegistro ASC, a.id ASC")
    List<Asistencia> findByActividadConParticipante(@Param("actividadId") Long actividadId);
}
