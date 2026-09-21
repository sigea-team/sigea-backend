package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Objeto de Transferencia de Datos (DTO) para la captura y validación de datos
 * en el proceso de registro de nuevos usuarios en la plataforma SIGEA.
 * <p>
 * Agrupa los datos personales primarios de la entidad {@code Persona} junto con las
 * credenciales de acceso iniciales para la creación del {@code Usuario}. Incorpora
 * restricciones Bean Validation para garantizar la integridad de los datos antes de
 * ser procesados por la capa de servicio.
 * </p>
 *
 * @param nombres Nombres de la persona. Obligatorio, máx. 100 caracteres.
 * @param apellidos Apellidos de la persona. Obligatorio, máx. 100 caracteres.
 * @param tipoDocumento Tipo de documento de identidad (ej. CC, TI, CE, PASAPORTE). Obligatorio, máx. 20 caracteres.
 * @param numeroDocumento Identificador numérico o alfanumérico único del documento. Obligatorio, máx. 30 caracteres.
 * @param correo Dirección de correo electrónico principal para autenticación y notificaciones. Obligatorio, debe ser un email válido, máx. 150 caracteres.
 * @param contrasena Clave de acceso en texto plano. Debe cumplir con la política estricta de complejidad: 8 a 64 caracteres, al menos una mayúscula, una minúscula, un número y un carácter especial.
 * @param nombreUsuario Identificador único de usuario deseado (opcional). Si no se proporciona, la lógica de negocio generará uno basado en el correo. Máx. 50 caracteres.
 * @param telefono Número telefónico o móvil de contacto (opcional). Máx. 30 caracteres.
 * @param afiliacionInstitucional Entidad, empresa o universidad de procedencia (opcional). Máx. 150 caracteres.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Petición de registro de nuevo usuario en la plataforma")
public record RegistroRequest(

        @Schema(description = "Nombres de la persona", example = "Carlos Andrés")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombres,

        @Schema(description = "Apellidos de la persona", example = "Gómez Pérez")
        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden exceder 100 caracteres")
        String apellidos,

        @Schema(description = "Tipo de documento de identidad (CC, TI, CE, PASAPORTE, etc.)", example = "CC")
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no puede exceder 20 caracteres")
        String tipoDocumento,

        @Schema(description = "Número único de documento de identidad", example = "1098765432")
        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 30, message = "El número de documento no puede exceder 30 caracteres")
        String numeroDocumento,

        @Schema(description = "Correo electrónico principal del usuario", example = "carlos.gomez@universidad.edu.co")
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El formato de correo electrónico no es válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String correo,

        @Schema(
                description = "Contraseña segura. Mínimo 8 caracteres, al menos una mayúscula, una minúscula, un dígito y un carácter especial",
                example = "Segura123*"
        )
        @NotBlank(message = "La contraseña es obligatoria")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,64}$",
                message = "La contraseña no cumple con la política mínima de seguridad. Requisitos: mínimo 8 caracteres, al menos una letra mayúscula, una letra minúscula, un número y un carácter especial (@$!%*?&._-#)"
        )
        String contrasena,

        @Schema(description = "Nombre de usuario opcional (si se omite, se generará a partir del correo)", example = "cgomez")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]{3,50}$",
                message = "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos (sin '@' ni espacios), y debe tener entre 3 y 50 caracteres"
        )
        @Size(max = 50, message = "El nombre de usuario no puede exceder 50 caracteres")
        String nombreUsuario,

        @Schema(description = "Número de teléfono de contacto (opcional)", example = "+573001234567")
        @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
        String telefono,

        @Schema(description = "Institución u organización a la que pertenece (opcional)", example = "Universidad Nacional")
        @Size(max = 150, message = "La afiliación institucional no puede exceder 150 caracteres")
        String afiliacionInstitucional
) {
}