package com.sigea.demosigea_backend.model;

/**
 * Tipos de token permitidos según restricción de base de datos:
 * CHECK (tipo IN ('verificacion','recuperacion'))
 */
public enum TipoToken {
    verificacion,
    recuperacion
}
