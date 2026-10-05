package com.sigea.demosigea_backend.dto.convocatoria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para la creación y edición de una convocatoria asociada a un evento.
 * <p>
 * El estado no se incluye en este DTO para evitar que el cliente modifique
 * el ciclo de vida de la convocatoria en la creación o actualización.
 * </p>
 *
 * @param eventoId      ID del evento al que pertenece la convocatoria (obligatorio).
 * @param titulo        Título descriptivo (obligatorio, máx 150 caracteres).
 * @param descripcion   Descripción amplia del objeto y alcances.
 * @param requisitos    Términos de referencia y requisitos para las propuestas.
 * @param fechaApertura Fecha y hora de inicio de la recepción de propuestas (obligatoria).
 * @param fechaCierre   Fecha y hora de finalización (obligatoria, debe ser posterior a la fecha de apertura).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para crear o actualizar una convocatoria de evento")
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConvocatoriaRequest(

        @Schema(description = "ID del evento asociado", example = "1")
        @NotNull(message = "El ID del evento es obligatorio.")
        Long eventoId,

        @Schema(description = "Título de la convocatoria", example = "Convocatoria de Ponencias Congreso 2026")
        @NotBlank(message = "El título de la convocatoria es obligatorio.")
        @Size(max = 150, message = "El título no debe exceder los 150 caracteres.")
        String titulo,

        @Schema(description = "Descripción de la convocatoria", example = "Se convocan artículos de investigación e innovación...")
        String descripcion,

        @Schema(description = "Requisitos de participación", example = "Documentos en formato PDF, máximo 10 páginas...")
        String requisitos,

        @Schema(description = "Fecha y hora de apertura", example = "2026-10-10T08:00:00")
        @NotNull(message = "La fecha de apertura es obligatoria.")
        LocalDateTime fechaApertura,

        @Schema(description = "Fecha y hora de cierre", example = "2026-11-15T23:59:59")
        @NotNull(message = "La fecha de cierre es obligatoria.")
        LocalDateTime fechaCierre
) {}
