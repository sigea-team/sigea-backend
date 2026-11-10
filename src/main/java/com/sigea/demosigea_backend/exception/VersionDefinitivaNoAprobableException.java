package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.observacion.ObservacionResponse;
import lombok.Getter;

import java.util.List;

/**
 * No se aprueba la versión definitiva de una propuesta (HU-19, Criterio 4): tiene ajustes requeridos sin resolver o
 * fue clasificada como aprobada con ajustes y no se han registrado los ajustes. Se responde 409 con los pendientes.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class VersionDefinitivaNoAprobableException extends RuntimeException {

    /** Hay ajustes requeridos sin resolver. */
    public static final String OBSERVACIONES_PENDIENTES = "OBSERVACIONES_PENDIENTES";

    /** La propuesta se clasificó como aprobada con ajustes, pero no tiene ajustes registrados. */
    public static final String AJUSTES_SIN_REGISTRAR = "AJUSTES_SIN_REGISTRAR";

    private final String codigo;
    private final transient List<ObservacionResponse> pendientes;

    public VersionDefinitivaNoAprobableException(String codigo, String mensaje, List<ObservacionResponse> pendientes) {
        super(mensaje);
        this.codigo = codigo;
        this.pendientes = List.copyOf(pendientes);
    }
}
