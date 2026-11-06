package com.sigea.demosigea_backend.dto.comiteevaluador;

import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Persona;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Miembro del comité evaluador de una convocatoria (HU-15, Criterio 3).
 *
 * @param comiteEvaluadorId    ID del miembro del comité (el que usa HU-16 para asignar propuestas).
 * @param convocatoriaId       ID de la convocatoria.
 * @param personaId            ID de la persona.
 * @param nombres              nombres.
 * @param apellidos            apellidos.
 * @param correo               correo.
 * @param numeroDocumento      número de documento.
 * @param afiliacion           afiliación institucional, si tiene.
 * @param areaExperticia       área de experticia.
 * @param tieneCuenta          si la persona tiene cuenta de usuario (sin cuenta no puede ingresar a evaluar).
 * @param propuestasAsignadas  propuestas que tiene asignadas en esta convocatoria.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record EvaluadorResponse(
        @Schema(example = "3") Long comiteEvaluadorId,
        @Schema(example = "10") Long convocatoriaId,
        @Schema(example = "21") Long personaId,
        @Schema(example = "Ana María") String nombres,
        @Schema(example = "Pérez Gómez") String apellidos,
        @Schema(example = "ana.perez@ufps.edu.co") String correo,
        @Schema(example = "1090123456") String numeroDocumento,
        @Schema(example = "UFPS") String afiliacion,
        @Schema(example = "Ingeniería de software") String areaExperticia,
        @Schema(example = "true") boolean tieneCuenta,
        @Schema(example = "0") long propuestasAsignadas
) {

    /**
     * Construye la respuesta a partir del miembro del comité.
     *
     * @param ce                  miembro del comité con su persona cargada.
     * @param convocatoriaId      ID de la convocatoria.
     * @param tieneCuenta         si la persona tiene cuenta de usuario.
     * @param propuestasAsignadas propuestas asignadas al evaluador.
     * @return DTO del evaluador.
     */
    public static EvaluadorResponse of(ComiteEvaluador ce, Long convocatoriaId, boolean tieneCuenta,
                                       long propuestasAsignadas) {
        Persona p = ce.getPersona();
        return new EvaluadorResponse(
                ce.getId(),
                convocatoriaId,
                p.getId(),
                p.getNombres(),
                p.getApellidos(),
                p.getCorreo(),
                p.getNumeroDocumento(),
                p.getAfiliacion() != null ? p.getAfiliacion().getNombreAfiliacion() : null,
                ce.getAreaExperticia(),
                tieneCuenta,
                propuestasAsignadas);
    }
}
