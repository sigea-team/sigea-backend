package com.sigea.demosigea_backend.exception;

/**
 * Error técnico al guardar, leer o eliminar un archivo en el almacenamiento de documentos (HU-14).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public class AlmacenamientoException extends RuntimeException {

    public AlmacenamientoException(String mensaje) {
        super(mensaje);
    }

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
