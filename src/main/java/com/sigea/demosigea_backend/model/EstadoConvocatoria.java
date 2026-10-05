package com.sigea.demosigea_backend.model;

/**
 * Estados permitidos para una convocatoria según restricción de base de datos:
 * CHECK (estado IN ('borrador', 'publicada', 'cerrada'))
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoConvocatoria {
    borrador,
    publicada,
    cerrada
}
