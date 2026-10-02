package com.sigea.demosigea_backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Representa la estructura estándar para la transferencia de datos de errores (DTO)
 * devueltos por la API de SIGEA en respuestas HTTP no exitosas (4xx y 5xx).
 * <p>
 * Este {@code record} consolida los detalles técnicos y de negocio necesarios para
 * que los clientes del frontend o consumidores externos puedan procesar, mostrar y
 * gestionar las excepciones de manera uniforme.
 * </p>
 * <p>
 * Utiliza la anotación {@link JsonInclude} para omitir campos nulos durante la
 * serialización JSON, evitando enviar claves innecesarias según el tipo de error.
 * </p>
 *
 * @param timestamp Marca temporal exacta en la que se generó la excepción en el servidor.
 * @param status Código numérico del estado de respuesta HTTP (ej. 400, 403, 409, 500).
 * @param error Descripción estándar de la categoría del estado HTTP (ej. "Bad Request", "Forbidden").
 * @param message Mensaje legible y descriptivo que explica la causa del error.
 * @param codigo Código de negocio o regla de dominio violada (ej. "CUENTA_YA_EXISTE", "CORREO_NO_VERIFICADO").
 * @param correo Dirección de correo asociada al flujo del error, utilizada principalmente en bloqueos de autenticación para facilitar acciones como el reenvío de enlaces.
 * @param erroresValidacion Mapa que contiene los nombres de los campos con fallas de validación como claves y sus respectivos mensajes de error como valores.
 * @param path URI de la petición que originó el error.
 * @param bloqueadoHasta Fecha y hora en que finaliza el bloqueo temporal de la cuenta (solo para {@code CUENTA_BLOQUEADA}).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
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
        Map<String, String> erroresValidacion,

        @Schema(description = "URI de la petición que originó el error", example = "/api/v1/auth/login")
        String path,

        @Schema(description = "Fecha y hora en que finaliza el bloqueo temporal de la cuenta", example = "2026-09-17T17:15:00")
        LocalDateTime bloqueadoHasta
) {

    /**
     * Constructor de compatibilidad para los errores que no requieren {@code path}
     * ni {@code bloqueadoHasta}. Ambos campos quedan en {@code null} y, gracias a
     * {@link JsonInclude.Include#NON_NULL}, no se serializan en la respuesta.
     */
    public ErrorResponse(LocalDateTime timestamp, int status, String error, String message,
                         String codigo, String correo, Map<String, String> erroresValidacion) {
        this(timestamp, status, error, message, codigo, correo, erroresValidacion, null, null);
    }
}