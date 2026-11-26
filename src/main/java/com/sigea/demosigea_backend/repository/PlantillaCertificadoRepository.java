package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.PlantillaCertificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Plantilla (fondo) de los certificados de cada evento (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface PlantillaCertificadoRepository extends JpaRepository<PlantillaCertificado, Long> {

    Optional<PlantillaCertificado> findByEvento_Id(Long eventoId);
}
