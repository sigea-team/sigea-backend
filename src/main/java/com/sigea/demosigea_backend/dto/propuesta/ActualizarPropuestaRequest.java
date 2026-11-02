package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos para modificar una propuesta y generar una nueva versión (HU-14, Criterio 3).
 * <p>
 * Todos los campos son opcionales: los que lleguen en {@code null} conservan el valor de la versión anterior.
 * </p>
 * <p>
 * {@code documentosConservados} indica qué documentos de la versión anterior pasan a la nueva:
 * <ul>
 *   <li>{@code null}: se conservan todos.</li>
 *   <li>lista vacía: no se conserva ninguno (se reemplazan por los archivos nuevos).</li>
 *   <li>lista de IDs: solo se conservan esos. Para reemplazar un documento, se omite su ID y se envía el archivo nuevo.</li>
 * </ul>
 * </p>
 *
 * @param titulo                Nuevo título (máx. 200).
 * @param resumen               Nuevo resumen.
 * @param palabrasClave         Nuevas palabras clave (máx. 255).
 * @param lineaTematicaId       Nueva línea temática.
 * @param notaCambio            Descripción breve del cambio realizado (máx. 1000).
 * @param documentosConservados IDs de documentos de la versión anterior que se conservan.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Cambios de la propuesta para crear una nueva versión")
public record ActualizarPropuestaRequest(

        @Schema(description = "Nuevo título de la propuesta", example = "Modelos de Deep Learning para Diagnóstico Temprano (v2)")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String titulo,

        @Schema(description = "Nuevo resumen de la propuesta")
        String resumen,

        @Schema(description = "Nuevas palabras clave separadas por comas")
        @Size(max = 255, message = "Las palabras clave no pueden superar los 255 caracteres")
        String palabrasClave,

        @Schema(description = "Nueva línea temática", example = "3")
        Long lineaTematicaId,

        @Schema(description = "Descripción breve del cambio", example = "Se ajustó la metodología según observaciones")
        @Size(max = 1000, message = "La nota del cambio no puede superar los 1000 caracteres")
        String notaCambio,

        @Schema(description = "IDs de documentos de la versión anterior que se conservan (null = todos, [] = ninguno)")
        List<Long> documentosConservados
) {
}
