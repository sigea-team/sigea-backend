package com.sigea.demosigea_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

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
