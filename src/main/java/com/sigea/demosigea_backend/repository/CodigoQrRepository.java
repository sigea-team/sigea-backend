package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.CodigoQr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de los códigos QR de las inscripciones (HU-24).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface CodigoQrRepository extends JpaRepository<CodigoQr, Long> {

    /** Código de una inscripción al evento (hay a lo sumo uno). */
    Optional<CodigoQr> findByInscripcion_Id(Long inscripcionId);

    /** Verifica si un valor de código ya está en uso (unicidad al generar). */
    boolean existsByCodigo(String codigo);

    /**
     * Busca el código leído en el escáner, con la inscripción, la persona y el evento (Criterio 2 y 3).
     */
    @Query("SELECT q FROM CodigoQr q JOIN FETCH q.inscripcion i JOIN FETCH i.persona JOIN FETCH i.evento "
            + "WHERE q.codigo = :codigo")
    Optional<CodigoQr> findByCodigoConInscripcion(@Param("codigo") String codigo);
}
