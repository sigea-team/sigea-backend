package com.sigea.demosigea_backend.exception;

import lombok.Getter;

/**
 * Se lanza al actualizar un conferencista con un documento o correo que pertenece a OTRA persona
 * (HU-12). Se responde 409 {@code DATOS_PERSONA_DUPLICADOS} indicando el campo en conflicto.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
public class DatosPersonaDuplicadosException extends RecursoDuplicadoException {

    /** Campo del formulario en conflicto ({@code numeroDocumento} o {@code correo}). */
    private final String campo;

    public DatosPersonaDuplicadosException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}
