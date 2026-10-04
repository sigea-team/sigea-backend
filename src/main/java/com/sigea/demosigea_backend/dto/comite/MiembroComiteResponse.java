package com.sigea.demosigea_backend.dto.comite;

import com.sigea.demosigea_backend.model.ComiteOrganizador;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DTO de respuesta con una participación en el comité organizador (HU-06).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Participación de una persona en el comité organizador de un evento")
public record MiembroComiteResponse(
        @Schema(description = "ID de la participación (comite_id)", example = "5") Long id,
        @Schema(example = "3") Long eventoId,
        @Schema(example = "12") Long personaId,
        @Schema(example = "Ana María Pérez Gómez") String nombreCompleto,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "Coordinador general") String rolComite,
        LocalDateTime fechaAsignacion,
        @Schema(description = "Fecha de retiro (null si la participación está vigente)") LocalDateTime fechaRetiro,
        @Schema(description = "true = miembro vigente; false = retirado (historial)", example = "true") boolean activo
) {
    /**
     * Construye el DTO a partir de la entidad.
     *
     * @param miembro Entidad JPA (con la persona cargada).
     * @return DTO inmutable.
     */
    public static MiembroComiteResponse fromEntity(ComiteOrganizador miembro) {
        Persona persona = miembro.getPersona();
        return new MiembroComiteResponse(
                miembro.getId(),
                miembro.getEvento().getId(),
                persona.getId(),
                nombreCompleto(persona),
                persona.getNumeroDocumento(),
                persona.getCorreo(),
                miembro.getRolComite(),
                miembro.getFechaAsignacion(),
                miembro.getFechaRetiro(),
                miembro.isActivo()
        );
    }

    /**
     * Nombre completo de la persona, tolerante a valores nulos o vacíos.
     *
     * @param persona Persona (puede ser {@code null}).
     * @return "Nombres Apellidos" sin espacios sobrantes, o cadena vacía si no hay datos.
     */
    public static String nombreCompleto(Persona persona) {
        if (persona == null) {
            return "";
        }
        return Stream.of(persona.getNombres(), persona.getApellidos())
                .filter(parte -> parte != null && !parte.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
    }
}
