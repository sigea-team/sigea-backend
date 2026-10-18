package com.sigea.demosigea_backend.dto.propuesta;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sigea.demosigea_backend.model.RolAutor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la captura y validación de datos de un autor o ponente en una propuesta académica (HU-13).
 * <p>
 * Contempla los campos obligatorios exigidos por el Criterio 3:
 * <ul>
 *   <li>{@code nombre}: obligatorio, máx. 100 caracteres.</li>
 *   <li>{@code correo}: obligatorio, formato de email válido, máx. 150 caracteres.</li>
 *   <li>{@code filiacion}: obligatorio, filiación o institución académica de procedencia, máx. 150 caracteres.</li>
 *   <li>{@code rol}: obligatorio, rol diferenciado (autor principal, coautor, ponente) exigido por el Criterio 2.</li>
 * </ul>
 * </p>
 *
 * @param personaId       ID de la persona registrada (opcional si se envía datos completos).
 * @param nombre          Nombre(s) del autor (obligatorio).
 * @param apellidos       Apellidos del autor (opcional, máx. 100).
 * @param correo          Correo electrónico único de contacto del autor (obligatorio).
 * @param filiacion       Filiación institucional del autor (obligatorio).
 * @param afiliacionId    ID del catálogo de afiliaciones (opcional).
 * @param rol             Rol específico en la propuesta: autor_principal, coautor, ponente (obligatorio).
 * @param orden           Orden de autoría (opcional, si es null se asigna secuencialmente).
 * @param tipoDocumento   Tipo de documento de identidad (opcional).
 * @param numeroDocumento Número de documento de identidad (opcional).
 * @param telefono        Teléfono de contacto (opcional).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para registrar un autor o ponente en una propuesta académica")
public record AutorRequest(

        @Schema(description = "ID de la persona (si ya existe registrada en el sistema)", example = "5")
        Long personaId,

        @Schema(description = "Nombre(s) del autor o ponente", example = "Carlos Andrés")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        @JsonAlias({"nombres", "nombre"})
        String nombre,

        @Schema(description = "Apellidos del autor o ponente", example = "Gómez Pérez")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        @JsonAlias({"apellidos", "apellido"})
        String apellidos,

        @Schema(description = "Correo electrónico del autor o ponente", example = "carlos.gomez@ufps.edu.co")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El formato de correo no es válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        @JsonAlias({"correo", "email"})
        String correo,

        @Schema(description = "Filiación o institución académica de procedencia", example = "Universidad Francisco de Paula Santander")
        @NotBlank(message = "La filiación es obligatoria")
        @Size(max = 150, message = "La filiación no puede exceder 150 caracteres")
        @JsonAlias({"filiacion", "afiliacion", "afiliacionInstitucional"})
        String filiacion,

        @Schema(description = "ID de la afiliación institucional en catálogo (opcional)", example = "1")
        Long afiliacionId,

        @Schema(description = "Rol específico del autor en la propuesta (autor_principal, coautor, ponente)", example = "autor_principal")
        @NotNull(message = "El rol es obligatorio")
        RolAutor rol,

        @Schema(description = "Orden de prelación en la autoría (1 para autor principal)", example = "1")
        Integer orden,

        @Schema(description = "Tipo de documento de identidad (opcional)", example = "CC")
        @Size(max = 20, message = "El tipo de documento no puede exceder 20 caracteres")
        String tipoDocumento,

        @Schema(description = "Número único de documento de identidad (opcional)", example = "1098765432")
        @Size(max = 30, message = "El número de documento no puede exceder 30 caracteres")
        String numeroDocumento,

        @Schema(description = "Teléfono de contacto (opcional)", example = "+573001234567")
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
        String telefono
) {
    /**
     * Alias para compatibilidad con {@code nombres}.
     */
    public String nombres() {
        return nombre;
    }

    /**
     * Alias para compatibilidad con {@code afiliacion}.
     */
    public String afiliacion() {
        return filiacion;
    }
}
