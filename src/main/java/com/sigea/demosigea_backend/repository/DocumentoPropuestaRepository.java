package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.DocumentoPropuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link DocumentoPropuesta} (HU-14).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface DocumentoPropuestaRepository extends JpaRepository<DocumentoPropuesta, Long> {

    /** Busca un documento verificando que pertenezca a alguna versión de la propuesta indicada. */
    Optional<DocumentoPropuesta> findByIdAndVersion_Propuesta_Id(Long documentoId, Long propuestaId);
}
