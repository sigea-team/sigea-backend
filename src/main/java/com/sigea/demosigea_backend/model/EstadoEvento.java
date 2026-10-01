package com.sigea.demosigea_backend.model;

/**
 * Estados permitidos para un evento según restricción de base de datos:
 * CHECK (estado IN ('en_configuracion','habilitado','en_ejecucion','cerrado'))
 * <p>
 * Todo evento (o edición) nace en {@code en_configuracion} (HU-04, Criterio 1).
 * Mientras permanezca en ese estado se considera "no publicado" y su configuración
 * puede modificarse (HU-04, Criterio 2).
 * </p>
 */
public enum EstadoEvento {
    en_configuracion,
    habilitado,
    en_ejecucion,
    cerrado
}
