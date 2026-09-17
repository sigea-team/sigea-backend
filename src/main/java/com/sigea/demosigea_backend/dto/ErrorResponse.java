package com.sigea.demosigea_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Estructura estándar de respuesta para errores de la API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @Schema(description = "Fecha y hora del error", example = "2026-09-17T17:00:00")
        LocalDateTime timestamp,

        @Schema(description = "Código de estado HTTP", example = "400")
        int status,

        @Schema(description = "Nombre del error HTTP", example = "Bad Request")
        String error,

        @Schema(description = "Mensaje explicativo del error", example = "La cuenta ya existe con ese correo o documento.")
        String message,

        @Schema(description = "Código de negocio específico del error", example = "CORREO_NO_VERIFICADO")
        String codigo,

        @Schema(description = "Correo relacionado cuando aplica reenvío de verificación", example = "carlos.gomez@universidad.edu.co")
        String correo,

        @Schema(description = "Detalle de validación de campos inválidos")
        Map<String, String> erroresValidacion
) {
}
