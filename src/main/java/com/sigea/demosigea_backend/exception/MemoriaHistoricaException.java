package com.sigea.demosigea_backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * HU-28: operación de la memoria histórica rechazada (evento sin finalizar, memoria ya consolidada, memoria
 * inexistente, confirmación de eliminación incorrecta).
 */
@Getter
public class MemoriaHistoricaException extends RuntimeException {

    /** El evento todavía no ha finalizado (no está cerrado ni en su último día de ejecución). */
    public static final String EVENTO_NO_FINALIZADO = "EVENTO_NO_FINALIZADO";
    /** El evento ya fue cerrado formalmente. */
    public static final String MEMORIA_YA_CONSOLIDADA = "MEMORIA_YA_CONSOLIDADA";
    /** El evento no tiene memoria histórica (no se ha cerrado formalmente). */
    public static final String MEMORIA_NO_ENCONTRADA = "MEMORIA_NO_ENCONTRADA";
    /** El nombre escrito para confirmar la eliminación no coincide con el del evento. */
    public static final String CONFIRMACION_INVALIDA = "CONFIRMACION_INVALIDA";
    /** No se puede eliminar el evento (por ejemplo, es el evento base de otras ediciones). */
    public static final String ELIMINACION_NO_PERMITIDA = "ELIMINACION_NO_PERMITIDA";

    private final String codigo;
    private final HttpStatus status;

    public MemoriaHistoricaException(String codigo, HttpStatus status, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
        this.status = status;
    }
}
