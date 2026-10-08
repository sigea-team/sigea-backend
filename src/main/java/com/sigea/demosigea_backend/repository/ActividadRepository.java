package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para {@link Actividad}.
 * <p>
 * En la HU-05 solo se usan los conteos para el Criterio 2. HU-21 agregará aquí las consultas de
 * programación y detección de conflictos.
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
}
