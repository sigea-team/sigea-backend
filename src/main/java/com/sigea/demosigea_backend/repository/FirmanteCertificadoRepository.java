package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.FirmanteCertificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Firmantes de los certificados de cada evento (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface FirmanteCertificadoRepository extends JpaRepository<FirmanteCertificado, Long> {

    /** Firmantes en el orden en que se imprimen. */
    List<FirmanteCertificado> findByEvento_IdOrderByOrdenAscIdAsc(Long eventoId);

    Optional<FirmanteCertificado> findByIdAndEvento_Id(Long id, Long eventoId);
}
