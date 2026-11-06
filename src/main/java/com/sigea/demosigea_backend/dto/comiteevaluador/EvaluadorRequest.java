package com.sigea.demosigea_backend.dto.comiteevaluador;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Registro de un miembro del comité evaluador de una convocatoria (HU-15, Criterio 3).
 * <p>
 * La persona se identifica por su correo. Si el correo no pertenece a ninguna persona registrada,
 * se crea con los datos personales opcionales de esta solicitud (tipo y número de documento, nombres
 * y apellidos son entonces obligatorios).
 * </p>
 *
 * @param correo          correo de la persona evaluadora.
 * @param areaExperticia  área de experticia del evaluador.
 * @param tipoDocumento   tipo de documento (solo para personas nuevas).
 * @param numeroDocumento número de documento (solo para personas nuevas).
 * @param nombres         nombres (solo para personas nuevas).
 * @param apellidos       apellidos (solo para personas nuevas).
 * @param telefono        teléfono (opcional, solo para personas nuevas).
 * @param afiliacionId    afiliación institucional (opcional, solo para personas nuevas; se usa en HU-16 para
 *                        advertir conflictos de interés).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public record EvaluadorRequest(

        @Schema(description = "Correo del evaluador", example = "ana.perez@ufps.edu.co")
        @NotBlank(message = "El correo del evaluador es obligatorio.")
        @Email(message = "El correo del evaluador no tiene un formato válido.")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres.")
        String correo,

        @Schema(description = "Área de experticia", example = "Ingeniería de software")
        @NotBlank(message = "El área de experticia es obligatoria.")
        @Size(max = 150, message = "El área de experticia no puede superar los 150 caracteres.")
        String areaExperticia,

        @Schema(description = "Tipo de documento (solo si la persona no está registrada)", example = "CC")
        @Size(max = 20, message = "El tipo de documento no puede superar los 20 caracteres.")
        String tipoDocumento,

        @Schema(description = "Número de documento (solo si la persona no está registrada)", example = "1090123456")
        @Size(max = 30, message = "El número de documento no puede superar los 30 caracteres.")
        String numeroDocumento,

        @Schema(description = "Nombres (solo si la persona no está registrada)", example = "Ana María")
        @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres.")
        String nombres,

        @Schema(description = "Apellidos (solo si la persona no está registrada)", example = "Pérez Gómez")
        @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres.")
        String apellidos,

        @Schema(description = "Teléfono (opcional)", example = "3001234567")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres.")
        String telefono,

        @Schema(description = "ID de la afiliación institucional (opcional)", example = "1")
        @Positive(message = "El ID de la afiliación debe ser un número positivo.")
        Long afiliacionId
) {
}
