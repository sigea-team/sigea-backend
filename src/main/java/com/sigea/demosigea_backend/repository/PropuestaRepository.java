package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.Propuesta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link Propuesta}.
 * <p>
 * Proporciona consultas sobre propuestas postuladas a convocatorias y líneas temáticas (HU-05, HU-13).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.1
 * @since 2026
 */
@Repository
public interface PropuestaRepository extends JpaRepository<Propuesta, Long> {

    /** HU-05, Criterio 2: propuestas postuladas en la línea temática. */
    long countByLineaTematica_Id(Long lineaTematicaId);

    /** Lista propuestas enviadas a una convocatoria ordenadas por fecha de envío descendente. */
    List<Propuesta> findByConvocatoria_IdOrderByFechaEnvioDesc(Long convocatoriaId);

    /** Cuenta propuestas enviadas a una convocatoria. */
    long countByConvocatoria_Id(Long convocatoriaId);

    /** Obtiene una propuesta cargando sus autores y personas asociadas en una sola consulta. */
    @Query("SELECT DISTINCT p FROM Propuesta p LEFT JOIN FETCH p.autores a LEFT JOIN FETCH a.persona WHERE p.id = :id")
    Optional<Propuesta> findByIdWithAutores(@Param("id") Long id);

    /**
     * HU-14, Criterio 3: obtiene la propuesta bloqueando su fila hasta terminar la transacción,
     * para que dos modificaciones simultáneas no generen el mismo número de versión.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Propuesta p WHERE p.id = :id")
    Optional<Propuesta> findByIdParaActualizar(@Param("id") Long id);
}

