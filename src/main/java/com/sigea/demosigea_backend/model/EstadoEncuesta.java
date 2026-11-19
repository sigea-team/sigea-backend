package com.sigea.demosigea_backend.model;

/**
 * Estado de una encuesta de satisfacción (HU-25). Corresponde al CHECK {@code encuestas_estado_check}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoEncuesta {
    /** En configuración: se puede editar y eliminar; los participantes no la ven. */
    borrador,
    /** Publicada: habilitada para los participantes que asistieron (Criterio 1). Ya no se edita. */
    publicada,
    /** Cerrada por el administrador: no recibe más respuestas; los resultados se siguen consultando. */
    cerrada
}
