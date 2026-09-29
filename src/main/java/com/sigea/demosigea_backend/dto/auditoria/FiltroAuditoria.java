package com.sigea.demosigea_backend.dto.auditoria;

import java.time.LocalDate;

/**
 * Criterios de filtrado del log de auditoría (HU-03, Criterio 2).
 * Todos los campos son opcionales; los que llegan en {@code null} no se aplican.
 *
 * @param usuarioId ID del usuario que ejecutó la operación.
 * @param correo    Texto contenido en el correo del usuario (búsqueda parcial, sin distinguir mayúsculas).
 * @param accion    Tipo de operación exacto (ver {@code TipoOperacionAuditoria}).
 * @param entidad   Recurso afectado (ej. {@code roles}).
 * @param desde     Fecha inicial, inclusiva.
 * @param hasta     Fecha final, inclusiva (se toma el día completo).
 */
public record FiltroAuditoria(
        Long usuarioId,
        String correo,
        String accion,
        String entidad,
        LocalDate desde,
        LocalDate hasta
) {
}
