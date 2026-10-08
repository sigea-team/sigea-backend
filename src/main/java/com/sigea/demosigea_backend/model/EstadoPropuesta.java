package com.sigea.demosigea_backend.model;

/**
 * Estados de una propuesta académica según la restricción de base de datos:
 * CHECK (estado IN ('recibida','en_evaluacion','aprobada','ajustes','rechazada')).
 */
public enum EstadoPropuesta {
    recibida,
    en_evaluacion,
    aprobada,
    ajustes,
    rechazada
}
