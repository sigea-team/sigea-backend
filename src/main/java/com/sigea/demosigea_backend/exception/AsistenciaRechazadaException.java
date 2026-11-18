package com.sigea.demosigea_backend.exception;

import com.sigea.demosigea_backend.dto.asistencia.AsistenciaResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * No se registra la asistencia porque el QR no es válido para la actividad o la asistencia ya existe
 * (HU-24, Criterio 3). Lleva el código de negocio para que el escáner muestre el motivo.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class AsistenciaRechazadaException extends RuntimeException {

    /** El código leído no corresponde a ninguna inscripción. */
    public static final String QR_INVALIDO = "QR_INVALIDO";
    /** El código existe pero está marcado como no vigente. */
    public static final String QR_NO_VIGENTE = "QR_NO_VIGENTE";
    /** El código es de una inscripción a otro evento. */
    public static final String QR_OTRO_EVENTO = "QR_OTRO_EVENTO";
    /** La persona está inscrita al evento pero no a la actividad. */
    public static final String NO_INSCRITO_ACTIVIDAD = "NO_INSCRITO_ACTIVIDAD";
    /** La actividad está por programar o cancelada. */
    public static final String ACTIVIDAD_NO_DISPONIBLE = "ACTIVIDAD_NO_DISPONIBLE";
    /** El evento está en configuración o cerrado. */
    public static final String EVENTO_NO_DISPONIBLE = "EVENTO_NO_DISPONIBLE";
    /** La asistencia ya estaba registrada. */
    public static final String ASISTENCIA_DUPLICADA = "ASISTENCIA_DUPLICADA";

    private final String codigo;
    private final HttpStatus status;
    private final transient AsistenciaResponse asistenciaExistente;

    public AsistenciaRechazadaException(String codigo, HttpStatus status, String mensaje) {
        this(codigo, status, mensaje, null);
    }

    public AsistenciaRechazadaException(String codigo, HttpStatus status, String mensaje,
                                        AsistenciaResponse asistenciaExistente) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
        this.asistenciaExistente = asistenciaExistente;
    }
}
