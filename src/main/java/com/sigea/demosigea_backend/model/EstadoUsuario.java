package com.sigea.demosigea_backend.model;

/**
 * Estados permitidos para la cuenta de usuario según restricción de base de datos:
 * CHECK (estado IN ('activo','bloqueado','inactivo'))
 */
public enum EstadoUsuario {
    activo,
    bloqueado,
    inactivo
}
