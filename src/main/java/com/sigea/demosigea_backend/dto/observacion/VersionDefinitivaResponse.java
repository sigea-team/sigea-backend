package com.sigea.demosigea_backend.dto.observacion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resultado de aprobar la versión definitiva de una propuesta (HU-19, Criterio 3).
 *
 * @param propuestaId              ID de la propuesta.
 * @param titulo                   título.
 * @param estadoPropuesta          estado de la propuesta ({@code aprobada}).
 * @param versionId                ID de la versión definitiva.
 * @param numeroVersion            número de la versión definitiva.
 * @param versionDefinitivaAnterior número de la versión que era definitiva antes (si se reaprobó tras una
 *                                 modificación autorizada); {@code null} si es la primera aprobación.
 * @param fechaDefinitiva          fecha de aprobación.
 * @param aprobadaPor              nombre de quien la aprobó.
 * @param ajustesResueltos         ajustes requeridos resueltos que tenía la propuesta.
 * @param mensaje                  confirmación.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Versión definitiva aprobada")
public record VersionDefinitivaResponse(
        Long propuestaId,
        String titulo,
        String estadoPropuesta,
        Long versionId,
        Integer numeroVersion,
        Integer versionDefinitivaAnterior,
        LocalDateTime fechaDefinitiva,
        String aprobadaPor,
        long ajustesResueltos,
        String mensaje
) {
}
