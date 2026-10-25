package com.sigea.demosigea_backend.model;

/**
 * Tipo de una publicación registrada en la trayectoria del conferencista (HU-12, Criterio 1).
 * <p>
 * Los valores coinciden con el CHECK {@code publicaciones_conferencista_tipo_check}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoPublicacion {
    articulo,
    libro,
    capitulo,
    ponencia,
    otro
}
