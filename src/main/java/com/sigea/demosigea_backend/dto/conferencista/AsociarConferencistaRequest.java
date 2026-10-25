package com.sigea.demosigea_backend.dto.conferencista;

import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Asocia al evento a una persona que ya existe en el sistema (HU-12, Criterio 3).
 * <p>
 * Si la persona todavía no tiene trayectoria registrada (por ejemplo, un usuario que se registró
 * como participante), {@code trayectoria} es obligatoria. Si ya la tiene, puede omitirse; si se
 * envía, la reemplaza.
 * </p>
 *
 * @param personaId         ID de la persona existente.
 * @param tipoParticipacion Conferencista o ponente.
 * @param tema              Tema de su participación en el evento (opcional).
 * @param trayectoria       Trayectoria (obligatoria solo si la persona no la tiene).
 */
@Schema(description = "Asociación de una persona existente a un evento como conferencista")
public record AsociarConferencistaRequest(
        @Schema(example = "12")
        @NotNull(message = "Debe indicar la persona a asociar")
        Long personaId,

        @Schema(example = "conferencista")
        @NotNull(message = "El tipo de participación es obligatorio")
        TipoParticipacionConferencista tipoParticipacion,

        @Schema(example = "Inteligencia artificial aplicada a la educación")
        @Size(max = 200, message = "El tema no puede exceder 200 caracteres")
        String tema,

        @Valid
        TrayectoriaRequest trayectoria
) {
}
