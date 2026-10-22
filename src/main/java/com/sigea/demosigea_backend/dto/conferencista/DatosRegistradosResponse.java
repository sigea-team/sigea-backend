package com.sigea.demosigea_backend.dto.conferencista;

/**
 * Datos que SIGEA ya tiene de la persona invitada (p. ej. un participante con cuenta), para
 * precargar el formulario de la invitación. {@code trayectoria} es {@code null} si aún no la tiene.
 * <p>
 * El enlace de la invitación se abre sin iniciar sesión, así que el documento y el teléfono nunca se
 * devuelven completos: solo enmascarados (p. ej. {@code "•••••678"}) para que la persona sepa que ya
 * están registrados. Si los deja vacíos al completar la invitación, se conservan los guardados.
 *
 * @param tipoDocumento        Tipo de documento registrado.
 * @param documentoEnmascarado Número de documento con solo los 3 últimos dígitos visibles, o {@code null}.
 * @param nombres              Nombres.
 * @param apellidos            Apellidos.
 * @param telefonoEnmascarado  Teléfono con solo los 3 últimos dígitos visibles, o {@code null}.
 * @param trayectoria          Trayectoria pública ya registrada, o {@code null}.
 */
public record DatosRegistradosResponse(
        String tipoDocumento,
        String documentoEnmascarado,
        String nombres,
        String apellidos,
        String telefonoEnmascarado,
        TrayectoriaResponse trayectoria
) {

    /** Caracteres que quedan visibles al final de un dato enmascarado. */
    static final int VISIBLES = 3;

    /**
     * Oculta un dato sensible dejando visibles solo sus últimos 3 caracteres.
     *
     * @param valor Documento o teléfono.
     * @return El valor enmascarado, o {@code null} si está vacío.
     */
    public static String enmascarar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() <= VISIBLES) {
            return "•".repeat(limpio.length());
        }
        return "•".repeat(Math.min(limpio.length() - VISIBLES, 6)) + limpio.substring(limpio.length() - VISIBLES);
    }
}
