package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.dto.comite.MiembroComiteResponse;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.PerfilConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Conferencista asociado a un evento: fila del listado del módulo de conferencistas (HU-12).
 *
 * @param participacionId               ID de la asociación con el evento.
 * @param tienePerfil                   {@code true} si la persona ya tiene trayectoria registrada.
 * @param fechaActualizacionTrayectoria Última modificación de la trayectoria.
 */
@Schema(description = "Conferencista asociado a un evento")
public record ConferencistaEventoResponse(
        @Schema(example = "7") Long participacionId,
        @Schema(example = "3") Long eventoId,
        @Schema(example = "12") Long personaId,
        @Schema(example = "Ana María Pérez Gómez") String nombreCompleto,
        @Schema(example = "CC") String tipoDocumento,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "3001234567") String telefono,
        @Schema(example = "conferencista") TipoParticipacionConferencista tipoParticipacion,
        @Schema(example = "Inteligencia artificial aplicada a la educación") String tema,
        LocalDateTime fechaRegistro,
        @Schema(example = "true") boolean tienePerfil,
        @Schema(example = "Profesora titular") String cargoActual,
        @Schema(example = "Universidad Nacional de Colombia") String institucion,
        LocalDateTime fechaActualizacionTrayectoria
) {

    /**
     * @param participacion Participación (con persona y evento cargados).
     * @param perfil        Trayectoria de la persona; puede ser {@code null}.
     */
    public static ConferencistaEventoResponse de(ParticipacionConferencista participacion, PerfilConferencista perfil) {
        Persona persona = participacion.getPersona();
        return new ConferencistaEventoResponse(
                participacion.getId(),
                participacion.getEvento().getId(),
                persona.getId(),
                MiembroComiteResponse.nombreCompleto(persona),
                persona.getTipoDocumento(),
                persona.getNumeroDocumento(),
                persona.getCorreo(),
                persona.getTelefono(),
                participacion.getTipoParticipacion(),
                participacion.getTema(),
                participacion.getFechaRegistro(),
                perfil != null,
                perfil != null ? perfil.getCargoActual() : null,
                perfil != null ? perfil.getInstitucion() : null,
                perfil != null ? perfil.getFechaActualizacion() : null);
    }
}
