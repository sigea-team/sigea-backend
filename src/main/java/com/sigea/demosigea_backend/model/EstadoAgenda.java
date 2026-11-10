package com.sigea.demosigea_backend.model;

/**
 * Estados de la agenda de un día del evento (HU-22), según la restricción de base de datos:
 * CHECK (estado IN ('no_publicada','publicada')) de {@code agendas_publicadas}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum EstadoAgenda {
    no_publicada,
    publicada
}
