package com.sigea.demosigea_backend.model;

/**
 * Tipos de notificación que el sistema muestra en la campana del usuario.
 * <p>
 * Se guarda con {@code @Enumerated(EnumType.STRING)} en {@code notificaciones.tipo}. La columna no tiene CHECK
 * en base de datos: para un tipo nuevo basta con agregar la constante aquí.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoNotificacion {
    /** HU-11, Criterio 2: cambió la fecha límite de recepción de propuestas de una convocatoria publicada. */
    CONVOCATORIA_FECHA_MODIFICADA,
    /** HU-16, Criterio 1: se asignó una propuesta al evaluador; aparece en su bandeja. */
    PROPUESTA_ASIGNADA,
    /** HU-16, Criterio 3: la propuesta se reasignó a otro evaluador y sale de la bandeja del anterior. */
    ASIGNACION_RETIRADA
}
