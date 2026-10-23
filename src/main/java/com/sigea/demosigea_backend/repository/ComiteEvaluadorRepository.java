package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.ComiteEvaluador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link ComiteEvaluador} (HU-17).
 * <p>
 * Permite verificar y recuperar la membresía de un evaluador en el comité de una convocatoria.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface ComiteEvaluadorRepository extends JpaRepository<ComiteEvaluador, Long> {

    /**
     * Busca el registro de un evaluador específico en el comité de una convocatoria.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @param personaId      ID de la persona evaluadora.
     * @return el miembro del comité, o vacío si no existe la vinculación.
     */
    Optional<ComiteEvaluador> findByConvocatoria_IdAndPersona_Id(Long convocatoriaId, Long personaId);
}
