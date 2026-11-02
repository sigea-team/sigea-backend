package com.sigea.demosigea_backend.dto.propuesta;

import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Versión de una propuesta con la fecha y el autor del cambio (HU-14, Criterio 4).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Versión de una propuesta con fecha, autor del cambio y documentos")
public record VersionPropuestaResponse(

        @Schema(description = "ID de la versión", example = "21")
        Long id,

        @Schema(description = "ID de la propuesta", example = "15")
        Long propuestaId,

        @Schema(description = "Número de la versión", example = "2")
        Integer numeroVersion,

        @Schema(description = "Fecha y hora del cambio")
        LocalDateTime fechaCreacion,

        @Schema(description = "ID del usuario que hizo el cambio", example = "4")
        Long autorCambioUsuarioId,

        @Schema(description = "Nombre de quien hizo el cambio", example = "Carlos Andrés Gómez Pérez")
        String autorCambioNombre,

        @Schema(description = "Correo de quien hizo el cambio", example = "carlos.gomez@ufps.edu.co")
        String autorCambioCorreo,

        @Schema(description = "Título de la propuesta en esta versión")
        String titulo,

        @Schema(description = "Resumen de la propuesta en esta versión")
        String resumen,

        @Schema(description = "Palabras clave en esta versión")
        String palabrasClave,

        @Schema(description = "ID de la línea temática en esta versión")
        Long lineaTematicaId,

        @Schema(description = "Nombre de la línea temática en esta versión")
        String lineaTematicaNombre,

        @Schema(description = "Descripción del cambio registrada por el autor")
        String notaCambio,

        @Schema(description = "Indica si es la versión definitiva aprobada (RF29)")
        Boolean esDefinitiva,

        @Schema(description = "Documentos de la versión")
        List<DocumentoPropuestaResponse> documentos
) {
    public static VersionPropuestaResponse fromEntity(VersionPropuesta v) {
        Long propuestaId = v.getPropuesta() != null ? v.getPropuesta().getId() : null;
        Usuario usuario = v.getCreadoPor();
        Persona persona = usuario != null ? usuario.getPersona() : null;
        String nombre = null;
        if (persona != null) {
            String apellidos = persona.getApellidos();
            nombre = ((persona.getNombres() == null ? "" : persona.getNombres())
                    + (apellidos != null && !apellidos.isBlank() ? " " + apellidos : "")).trim();
        }
        List<DocumentoPropuestaResponse> documentos = v.getDocumentos() == null ? List.of()
                : v.getDocumentos().stream()
                        .map(d -> DocumentoPropuestaResponse.fromEntity(d, propuestaId))
                        .toList();

        return new VersionPropuestaResponse(
                v.getId(),
                propuestaId,
                v.getNumeroVersion(),
                v.getFechaCreacion(),
                usuario != null ? usuario.getId() : null,
                nombre,
                persona != null ? persona.getCorreo() : null,
                v.getTitulo(),
                v.getResumen(),
                v.getPalabrasClave(),
                v.getLineaTematica() != null ? v.getLineaTematica().getId() : null,
                v.getLineaTematica() != null ? v.getLineaTematica().getNombre() : null,
                v.getObservaciones(),
                Boolean.TRUE.equals(v.getEsDefinitiva()),
                documentos
        );
    }
}
