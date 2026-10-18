package com.sigea.demosigea_backend.dto.propuesta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO para la creación y postulación de una propuesta académica con sus autores y ponentes (HU-13).
 *
 * @param convocatoriaId  ID de la convocatoria abierta a la que se postula el trabajo (obligatorio).
 * @param titulo          Título de la propuesta académica (obligatorio, máx. 200 caracteres).
 * @param resumen         Resumen ejecutivo o abstract del trabajo.
 * @param palabrasClave   Palabras clave representativas (máx. 255 caracteres).
 * @param lineaTematicaId ID de la línea temática del evento en la que se clasifica (opcional).
 * @param autores         Lista de autores y ponentes asociados a la propuesta (Criterios 1 y 2).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para el envío o registro de una propuesta académica")
public record PropuestaRequest(

        @Schema(description = "ID de la convocatoria a la que se postula la propuesta", example = "1")
        @NotNull(message = "El ID de la convocatoria es obligatorio")
        Long convocatoriaId,

        @Schema(description = "Título descriptivo de la propuesta", example = "Modelos de Deep Learning para Diagnóstico Temprano")
        @NotBlank(message = "El título de la propuesta es obligatorio")
        @Size(max = 200, message = "El título no puede superar los 200 caracteres")
        String titulo,

        @Schema(description = "Resumen o abstract del trabajo académico", example = "El presente estudio evalúa arquitecturas neuronales...")
        String resumen,

        @Schema(description = "Palabras clave separadas por comas", example = "Deep Learning, Diagnóstico, Salud, Redes Neuronales")
        @Size(max = 255, message = "Las palabras clave no pueden superar los 255 caracteres")
        String palabrasClave,

        @Schema(description = "ID de la línea temática del evento asociada (opcional)", example = "3")
        Long lineaTematicaId,

        @Schema(description = "Autores y ponentes que postulan la propuesta (con sus roles específicos)")
        List<@Valid AutorRequest> autores
) {
}
