package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link Evento} (HU-04: RF03 y RF04).
 * <p>
 * Solo usa métodos derivados; los filtros opcionales del listado se resuelven con
 * {@link JpaSpecificationExecutor}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    /** Ediciones derivadas de un evento base, en orden cronológico (Criterio 4). */
    List<Evento> findByEventoBase_IdOrderByFechaInicioAscIdAsc(Long eventoBaseId);

    /** Número de ediciones derivadas de un evento base (Criterio 5). */
    long countByEventoBase_Id(Long eventoBaseId);

    /** Indica si el evento base tiene el semestre dado. */
    boolean existsByIdAndSemestre(Long id, String semestre);

    /** Indica si alguna edición derivada del evento base tiene el semestre dado. */
    boolean existsByEventoBase_IdAndSemestre(Long eventoBaseId, String semestre);

    /** Como {@link #existsByIdAndSemestre}, pero sin contar el evento {@code excluirId} (usado al actualizar). */
    boolean existsByIdAndSemestreAndIdNot(Long id, String semestre, Long excluirId);

    /** Como {@link #existsByEventoBase_IdAndSemestre}, pero sin contar el evento {@code excluirId} (usado al actualizar). */
    boolean existsByEventoBase_IdAndSemestreAndIdNot(Long eventoBaseId, String semestre, Long excluirId);
}
