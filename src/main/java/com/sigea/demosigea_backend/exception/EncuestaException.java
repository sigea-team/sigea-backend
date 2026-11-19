package com.sigea.demosigea_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Operación rechazada sobre una encuesta de satisfacción (HU-25). Lleva el código de negocio, el estado HTTP y,
 * si aplica, el detalle por campo o por pregunta para que el frontend lo muestre junto al campo.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class EncuestaException extends RuntimeException {

    /** 400: configuración de la encuesta o de sus preguntas inválida (detalle por campo). */
    public static final String CONFIGURACION_INVALIDA = "ENCUESTA_CONFIGURACION_INVALIDA";
    /** 409: la encuesta ya no está en borrador (o tiene respuestas) y no se puede modificar o eliminar. */
    public static final String NO_EDITABLE = "ENCUESTA_NO_EDITABLE";
    /** 409: la encuesta no está en el estado que exige la operación (publicar, cerrar). */
    public static final String ESTADO_INVALIDO = "ENCUESTA_ESTADO_INVALIDO";
    /** 409: falta algo para publicar (preguntas, fecha de cierre vencida...). */
    public static final String INCOMPLETA = "ENCUESTA_INCOMPLETA";
    /** 409: el evento está en configuración; nadie ha podido asistir todavía. */
    public static final String EVENTO_NO_DISPONIBLE = "EVENTO_NO_DISPONIBLE";
    /** 409: la actividad evaluada está cancelada o por programar. */
    public static final String ACTIVIDAD_NO_DISPONIBLE = "ACTIVIDAD_NO_DISPONIBLE";
    /** 403: el participante no asistió al evento o a la actividad (Criterio 3). */
    public static final String NO_HABILITADA = "ENCUESTA_NO_HABILITADA";
    /** 409: la encuesta está cerrada o pasó su fecha de cierre. */
    public static final String CERRADA = "ENCUESTA_CERRADA";
    /** 409: el participante ya respondió la encuesta. */
    public static final String YA_RESPONDIDA = "ENCUESTA_YA_RESPONDIDA";
    /** 400: respuestas incompletas o fuera de rango (detalle por pregunta). */
    public static final String RESPUESTAS_INVALIDAS = "RESPUESTAS_INVALIDAS";
    /** 409: se piden respuestas individuales de una encuesta anónima. */
    public static final String ANONIMA = "ENCUESTA_ANONIMA";

    private final String codigo;
    private final HttpStatus status;
    private final transient Map<String, String> errores;

    public EncuestaException(String codigo, HttpStatus status, String mensaje) {
        this(codigo, status, mensaje, null);
    }

    public EncuestaException(String codigo, HttpStatus status, String mensaje, Map<String, String> errores) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
        this.errores = errores == null || errores.isEmpty() ? null : Map.copyOf(errores);
    }
}
