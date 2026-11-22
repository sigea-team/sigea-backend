package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.memoria.PendienteCierreResponse;
import lombok.Getter;

import java.util.List;

/**
 * HU-28 / CU-32, flujo alterno 1.1-1.2: el evento tiene procesos pendientes (evaluaciones sin finalizar,
 * certificados sin generar...) y el cierre formal no se confirmó explícitamente. Responde 409 {@value #CODIGO} con
 * la lista de pendientes.
 */
@Getter
public class CierreConPendientesException extends RuntimeException {

    public static final String CODIGO = "CIERRE_CON_PENDIENTES";

    private final transient List<PendienteCierreResponse> pendientes;

    public CierreConPendientesException(String mensaje, List<PendienteCierreResponse> pendientes) {
        super(mensaje);
        this.pendientes = List.copyOf(pendientes);
    }
}
