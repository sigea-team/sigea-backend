package com.sigea.demosigea_backend.dto.convocatoria;

import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para la creación o modificación de una convocatoria.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Solicitud para crear o actualizar una convocatoria de trabajos académicos")
public record ConvocatoriaRequest(

        @Schema(description = "ID del evento asociado a la convocatoria", example = "1")
        Long eventoId,

        @Schema(description = "Título de la convocatoria", example = "Convocatoria Abierta de Ponencias V Congreso SIGEA 2026")
        @NotBlank(message = "El título de la convocatoria es obligatorio.")
        @Size(max = 150, message = "El título no debe exceder los 150 caracteres.")
        String titulo,

        @Schema(description = "Descripción detallada de la convocatoria", example = "Se convoca a la comunidad académica a presentar sus propuestas de investigación.")
        String descripcion,

        @Schema(description = "Requisitos y términos de referencia", example = "Formato IEEE, máximo 6 páginas, archivo PDF anónimo para revisión a ciegas.")
        String requisitos,

        @Schema(description = "Fecha y hora de inicio de la recepción de propuestas", example = "2026-03-01T08:00:00")
        @NotNull(message = "La fecha de apertura es obligatoria.")
        LocalDateTime fechaApertura,

        @Schema(description = "Fecha y hora límite para la entrega de propuestas", example = "2026-05-31T23:59:59")
        @NotNull(message = "La fecha de cierre es obligatoria.")
        LocalDateTime fechaCierre,

        @Schema(description = "Estado de la convocatoria (borrador, publicada, cerrada). Si es nulo en creación, por defecto se asigna borrador.", example = "borrador")
        EstadoConvocatoria estado
) {}
