package com.sigea.demosigea_backend.dto.comite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * DTO para vincular una persona al comité organizador de un evento (HU-06, Criterio 1).
 * <p>
 * La persona se identifica por {@code personaId} o por {@code numeroDocumento} (al menos uno).
 * El número de documento facilita al organizador registrar a alguien sin conocer su ID interno.
 * Si se envían ambos, prevalece {@code personaId}.
 * </p>
 *
 * @param personaId       ID de la persona registrada en el sistema (opcional si se envía el documento).
 * @param numeroDocumento Número de documento de la persona (opcional si se envía el ID).
 * @param rolComite       Rol dentro de la organización del evento (obligatorio, máx. 50).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para agregar un responsable o miembro al comité organizador")
public record MiembroComiteRequest(
        @Schema(description = "ID de la persona", example = "12")
        @Positive(message = "El ID de la persona debe ser un número positivo.")
        Long personaId,

        @Schema(description = "Número de documento de la persona (alternativa al ID)", example = "1090123456")
        @Size(max = 30, message = "El número de documento no puede superar los 30 caracteres.")
        String numeroDocumento,

        @Schema(description = "Rol dentro del comité organizador", example = "Coordinador general")
        @NotBlank(message = "El rol dentro del comité es obligatorio.")
        @Size(max = 50, message = "El rol no puede superar los 50 caracteres.")
        String rolComite
) {
    /**
     * Exige que la persona venga identificada por ID o por número de documento.
     *
     * @return {@code true} si se envió al menos uno de los dos identificadores.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Debe indicar la persona mediante personaId o numeroDocumento.")
    public boolean isPersonaIdentificada() {
        return personaId != null || (numeroDocumento != null && !numeroDocumento.isBlank());
    }
}
