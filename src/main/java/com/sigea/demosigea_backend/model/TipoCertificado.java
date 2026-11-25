package com.sigea.demosigea_backend.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Tipo de certificado (HU-26, RF47). Los valores coinciden con el CHECK {@code certificados_tipo_certificado_check}
 * (changeset hu26-002).
 * <ul>
 *   <li>{@link #asistente}: cumplió el porcentaje mínimo de asistencia del evento.</li>
 *   <li>{@link #ponente}: tuvo a cargo al menos una actividad realizada del evento.</li>
 *   <li>{@link #conferencista}: como ponente, pero registrado como conferencista invitado del evento (HU-12).</li>
 *   <li>{@link #evaluador} y {@link #organizador}: previstos en RF47; HU-26 todavía no los emite.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
@RequiredArgsConstructor
public enum TipoCertificado {
    asistente("Asistente", "asistente", true),
    ponente("Ponente", "ponente", true),
    conferencista("Conferencista", "conferencista", true),
    evaluador("Evaluador", "evaluador", false),
    organizador("Organizador", "organizador", false);

    /** Nombre para mostrar. */
    private final String descripcion;

    /** Rol tal como se escribe en el texto del certificado («participó como ...»). */
    private final String rolEnTexto;

    /** {@code true} si HU-26 emite este tipo. */
    private final boolean emisionDisponible;
}
