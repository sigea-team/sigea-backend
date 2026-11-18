package com.sigea.demosigea_backend.model;

/**
 * Tipo de respuesta de una pregunta de encuesta (HU-25). Corresponde al CHECK
 * {@code preguntas_encuesta_tipo_respuesta_check}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoRespuestaEncuesta {
    /** Valor entero dentro del rango de la pregunta (por ejemplo, 1 a 5). */
    escala,
    /** Texto libre. */
    texto,
    /** Una opción de la lista configurada (selección única). */
    opcion
}
