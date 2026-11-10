package com.sigea.demosigea_backend.dto.observacion;

import com.sigea.demosigea_backend.model.ObservacionPropuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Observación o ajuste requerido de una propuesta, con la versión que la originó y la que la atendió (HU-19).
 *
 * @param id                      ID de la observación.
 * @param descripcion             texto.
 * @param requiereAjuste          si es un ajuste requerido (bloquea la aprobación mientras esté sin resolver).
 * @param numeroVersion           versión observada (la que la originó).
 * @param versionAtencionNumero   versión que subió el autor para atenderla; {@code null} si aún no la atiende.
 * @param versionAtencionId       ID de esa versión.
 * @param resuelto                si ya se dio por resuelta.
 * @param estado                  {@code pendiente} (sin versión que la atienda), {@code atendida} (con versión, por
 *                                revisar), {@code resuelta} o {@code comentario} (no requiere ajuste y no se ha
 *                                cerrado).
 * @param registradaPor           nombre de quien la registró.
 * @param registradaPorEvaluador  si la registró un miembro del comité evaluador.
 * @param fecha                   fecha de registro.
 * @param resueltaPor             nombre de quien la dio por resuelta.
 * @param fechaResolucion         fecha en que se dio por resuelta.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Observación o ajuste requerido de una propuesta")
public record ObservacionResponse(
        Long id,
        String descripcion,
        boolean requiereAjuste,
        Integer numeroVersion,
        Integer versionAtencionNumero,
        Long versionAtencionId,
        boolean resuelto,
        @Schema(example = "atendida", allowableValues = {"pendiente", "atendida", "resuelta", "comentario"})
        String estado,
        String registradaPor,
        boolean registradaPorEvaluador,
        LocalDateTime fecha,
        String resueltaPor,
        LocalDateTime fechaResolucion
) {

    public static final String PENDIENTE = "pendiente";
    public static final String ATENDIDA = "atendida";
    public static final String RESUELTA = "resuelta";
    public static final String COMENTARIO = "comentario";

    public static ObservacionResponse fromEntity(ObservacionPropuesta o) {
        VersionPropuesta atencion = o.getVersionAtencion();
        boolean ajuste = Boolean.TRUE.equals(o.getRequiereAjuste());
        boolean resuelto = Boolean.TRUE.equals(o.getResuelto());
        String estado;
        if (resuelto) {
            estado = RESUELTA;
        } else if (!ajuste) {
            estado = COMENTARIO;
        } else {
            estado = atencion != null ? ATENDIDA : PENDIENTE;
        }
        return new ObservacionResponse(
                o.getId(),
                o.getDescripcion(),
                ajuste,
                o.getVersion() != null ? o.getVersion().getNumeroVersion() : null,
                atencion != null ? atencion.getNumeroVersion() : null,
                atencion != null ? atencion.getId() : null,
                resuelto,
                estado,
                nombre(o.getUsuario()),
                o.getComiteEvaluador() != null,
                o.getFecha(),
                nombre(o.getResueltoPor()),
                o.getFechaResolucion());
    }

    /** Nombre completo del usuario, o {@code null}. */
    public static String nombre(Usuario usuario) {
        Persona persona = usuario != null ? usuario.getPersona() : null;
        if (persona == null) {
            return null;
        }
        String nombres = persona.getNombres() == null ? "" : persona.getNombres();
        String apellidos = persona.getApellidos() == null ? "" : persona.getApellidos();
        String completo = (nombres + " " + apellidos).trim();
        return completo.isEmpty() ? null : completo;
    }
}
