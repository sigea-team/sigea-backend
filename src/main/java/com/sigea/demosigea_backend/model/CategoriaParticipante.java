package com.sigea.demosigea_backend.model;

/**
 * Categorías de participante permitidas en la inscripción a un evento según restricción de base de datos:
 * CHECK (categoria_participante IN ('estudiante','egresado','docente','particular'))
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum CategoriaParticipante {
    estudiante,
    egresado,
    docente,
    particular
}
