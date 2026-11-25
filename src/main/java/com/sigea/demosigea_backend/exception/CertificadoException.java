package com.sigea.demosigea_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * No se emite o no se configura un certificado (HU-26). Lleva el código de negocio y el estado HTTP para que el
 * frontend muestre el motivo.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class CertificadoException extends RuntimeException {

    /** 409, Criterio 2: la persona no cumple los requisitos de asistencia o participación de su rol. */
    public static final String REQUISITOS_NO_CUMPLIDOS = "CERTIFICADO_REQUISITOS_NO_CUMPLIDOS";
    /** 409: el evento no ha terminado; la asistencia todavía puede cambiar. */
    public static final String EVENTO_NO_FINALIZADO = "EVENTO_NO_FINALIZADO";
    /** 409: el tipo pedido (evaluador, organizador) todavía no se emite. */
    public static final String TIPO_NO_DISPONIBLE = "CERTIFICADO_TIPO_NO_DISPONIBLE";
    /** 409: ya hay certificados de asistente emitidos y el porcentaje mínimo ya no se cambia. */
    public static final String CONFIGURACION_BLOQUEADA = "CONFIGURACION_CERTIFICADOS_BLOQUEADA";
    /** 409: la plantilla (fondo y firmantes) ya no se cambia porque hay certificados emitidos. */
    public static final String PLANTILLA_BLOQUEADA = "PLANTILLA_CERTIFICADO_BLOQUEADA";
    /** 409: el certificado admite máximo 4 firmantes. */
    public static final String MAXIMO_FIRMANTES = "FIRMANTES_CERTIFICADO_MAXIMO";
    /** 409: otra solicitud emitió el mismo certificado al mismo tiempo. */
    public static final String EMISION_SIMULTANEA = "CERTIFICADO_EMISION_SIMULTANEA";

    private final String codigo;
    private final HttpStatus status;

    public CertificadoException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }
}
