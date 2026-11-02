package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link Actividad}.
 * <p>
 * Contiene consultas usadas por HU-05 (Criterio 2) y HU-Salas (Criterios 4 y 5).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    /** HU-05, Criterio 2: actividades de la agenda que usan el tipo de actividad. */
    long countByTipoActividad_Id(Long tipoActividadId);

    /** HU-05, Criterio 2: actividades de la agenda clasificadas en la línea temática. */
    long countByLineaTematica_Id(Long lineaTematicaId);

    /** HU-Salas, Criterio 4: número de actividades programadas en la sala (para detectar si tiene uso). */
    long countBySala_Id(Long salaId);

    /**
     * HU-Salas, Criterio 5: actividades de una sala en un evento específico.
     * Se usa para detectar conflictos al actualizar la disponibilidad horaria de la sala.
     */
    List<Actividad> findBySala_IdAndEvento_Id(Long salaId, Long eventoId);

    /**
     * HU-Salas, Criterio 5: actividades de un ponente en un evento específico.
     * Se usa para detectar conflictos al actualizar la disponibilidad horaria del ponente.
     */
    List<Actividad> findByPonente_IdAndEvento_Id(Long ponenteId, Long eventoId);
}
