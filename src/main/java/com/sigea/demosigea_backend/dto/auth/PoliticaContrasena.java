package com.sigea.demosigea_backend.dto.auth;

/**
 * Política única de contraseñas de SIGEA. La usan el registro de usuarios ({@link RegistroRequest}),
 * el restablecimiento de contraseña ({@link RestablecerContrasenaRequest}) y la cuenta que crea el
 * conferencista invitado (HU-12, {@code CompletarInvitacionRequest}), para que las tres validen y
 * respondan exactamente igual. {@code PoliticaContrasenaTest} falla si alguna deja de usarla.
 *
 * <p>Mínimo 8 y máximo 64 caracteres, con al menos una mayúscula, una minúscula, un número y un
 * carácter especial de {@code @$!%*?&._-#}.</p>
 */
public final class PoliticaContrasena {

    /** Expresión regular de la política (la misma que valida el frontend). */
    public static final String PATRON =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,64}$";

    /** Mensaje cuando la contraseña no cumple la política. */
    public static final String MENSAJE =
            "La contraseña no cumple con la política mínima de seguridad. Requisitos: mínimo 8 caracteres, al menos una letra mayúscula, una letra minúscula, un número y un carácter especial (@$!%*?&._-#)";

    /** Mensaje cuando falta la contraseña. */
    public static final String MENSAJE_OBLIGATORIA = "La contraseña es obligatoria";

    private PoliticaContrasena() {
    }
}
