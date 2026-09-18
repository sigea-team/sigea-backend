package com.sigea.demosigea_backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Objeto de Transferencia de Datos (DTO) que representa la respuesta emitida por
 * el servidor tras un inicio de sesión exitoso en la plataforma SIGEA.
 * <p>
 * Retorna el token de acceso JWT necesario para autenticar las peticiones subsecuentes
 * en los endpoints protegidos, junto con la información del perfil del usuario
 * autenticado y sus permisos dentro del sistema.
 * </p>
 *
 * @param token Cadena compacta firmada en formato JWT (Json Web Token) otorgada para autorizar peticiones HTTP.
 * @param tipoToken Esquema de autorización HTTP utilizado para el envío del token (por defecto {@code "Bearer"}).
 * @param usuarioId Identificador único de la entidad {@code Usuario} en la base de datos.
 * @param nombreUsuario Nombre de usuario (username) único registrado en el sistema.
 * @param correo Dirección de correo electrónico institucional o personal asociada a la cuenta.
 * @param nombreCompleto Nombres y apellidos concatenados de la persona asociada al usuario.
 * @param roles Lista de roles de seguridad otorgados al usuario (ej. {@code "ADMIN"}, {@code "PARTICIPANTE"}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Respuesta de autenticación exitosa con token JWT")
public record LoginResponse(
        @Schema(description = "Token de autenticación JWT Bearer", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Tipo de esquema de token", example = "Bearer")
        String tipoToken,

        @Schema(description = "Identificador del usuario", example = "1")
        Long usuarioId,

        @Schema(description = "Nombre de usuario autenticado", example = "cgomez")
        String nombreUsuario,

        @Schema(description = "Correo electrónico del usuario autenticado", example = "carlos.gomez@universidad.edu.co")
        String correo,

        @Schema(description = "Nombres y apellidos completos de la persona", example = "Carlos Andrés Gómez Pérez")
        String nombreCompleto,

        @Schema(description = "Lista de roles asignados al usuario", example = "[\"PARTICIPANTE\"]")
        List<String> roles
) {
}