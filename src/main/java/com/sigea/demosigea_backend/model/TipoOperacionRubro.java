package com.sigea.demosigea_backend.model;

/**
 * Tipo de modificación registrada en el historial de un rubro presupuestal (HU-07, Criterio 2).
 * <p>
 * Corresponde al {@code CHECK (tipo_operacion IN ('creacion','edicion','eliminacion'))} de la tabla
 * {@code historial_rubros} (changeset {@code hu07-002}).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum TipoOperacionRubro {
    /** Alta del rubro en el presupuesto preliminar. Solo tiene valores nuevos. */
    creacion,
    /** Cambio de nombre, cantidad o valor unitario. Tiene valores anteriores y nuevos. */
    edicion,
    /** Retiro del rubro del presupuesto vigente (borrado lógico). Solo tiene valores anteriores. */
    eliminacion
}
