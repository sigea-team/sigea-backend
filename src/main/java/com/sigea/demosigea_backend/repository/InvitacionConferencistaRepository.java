package com.sigea.demosigea_backend.repository;

import com.sigea.demosigea_backend.model.EstadoInvitacion;
import com.sigea.demosigea_backend.model.InvitacionConferencista;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de las invitaciones a conferencistas nuevos (HU-12).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface InvitacionConferencistaRepository extends JpaRepository<InvitacionConferencista, Long> {

    /** Invitación del enlace del correo (pública). */
    @EntityGraph(attributePaths = "evento")
    Optional<InvitacionConferencista> findByToken(String token);

    /** Invitaciones de un evento en un estado, de la más reciente a la más antigua. */
    List<InvitacionConferencista> findByEvento_IdAndEstadoOrderByFechaEnvioDescIdDesc(Long eventoId, EstadoInvitacion estado);

    /** Invitación pendiente (vigente o vencida) para un correo en un evento; hay como máximo una. */
    Optional<InvitacionConferencista> findFirstByEvento_IdAndCorreoIgnoreCaseAndEstado(Long eventoId, String correo,
                                                                                       EstadoInvitacion estado);

    @EntityGraph(attributePaths = "evento")
    Optional<InvitacionConferencista> findByIdAndEvento_Id(Long id, Long eventoId);
}
