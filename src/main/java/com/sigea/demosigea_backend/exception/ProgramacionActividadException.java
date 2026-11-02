package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.actividad.AdvertenciaActividadResponse;
import com.sigea.demosigea_backend.dto.actividad.ConflictoActividadResponse;
import lombok.Getter;

import java.util.List;

/**
 * La actividad no se guarda: tiene cruces con la agenda o advertencias sin confirmar (HU-21, Criterios 2 y 5).
 * Se responde 409 con la lista de conflictos y advertencias.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class ProgramacionActividadException extends RuntimeException {

    /** Hay cruces con otras actividades: no se puede guardar. */
    public static final String CONFLICTO_PROGRAMACION = "CONFLICTO_PROGRAMACION";

    /** Solo hay advertencias: se puede guardar reenviando con confirmarAdvertencias = true. */
    public static final String CONFIRMACION_REQUERIDA = "CONFIRMACION_REQUERIDA";

    private final String codigo;
    private final transient List<ConflictoActividadResponse> conflictos;
    private final transient List<AdvertenciaActividadResponse> advertencias;

    public ProgramacionActividadException(String codigo, String mensaje,
                                          List<ConflictoActividadResponse> conflictos,
                                          List<AdvertenciaActividadResponse> advertencias) {
        super(mensaje);
        this.codigo = codigo;
        this.conflictos = List.copyOf(conflictos);
        this.advertencias = List.copyOf(advertencias);
    }
}
