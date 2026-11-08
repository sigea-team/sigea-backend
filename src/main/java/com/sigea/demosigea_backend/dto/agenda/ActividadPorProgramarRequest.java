package com.sigea.demosigea_backend.dto.agenda;

import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Actividad que se registra sin fecha, horario ni sala para que la generación automática la ubique (HU-22,
 * Criterio 1). La fecha, las horas y la sala las propone {@code POST /agenda/generar}.
 *
 * @param nombre          Nombre de la actividad.
 * @param tipoActividadId Tipo de actividad del evento.
 * @param duracionMinutos Cuánto dura (entre 5 minutos y 24 horas).
 * @param ponenteId       Ponente (opcional); debe estar asociado al evento. Su disponibilidad limita dónde se ubica.
 * @param propuestaId     Propuesta que se presenta (opcional).
 * @param lineaTematicaId Línea temática (opcional).
 * @param modalidad       presencial o hibrida (va a una sala física), virtual (va a una sala virtual) o vacía
 *                        (cualquier sala).
 * @param simultanea      Si puede coincidir en horario con otras actividades simultáneas (HU-21 Criterio 3).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Actividad por programar (sin fecha, horario ni sala)")
public record ActividadPorProgramarRequest(
        @NotBlank(message = "El nombre de la actividad es obligatorio.")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres.")
        @Schema(example = "Ponencia: Visión por computador")
        String nombre,

        @NotNull(message = "El tipo de actividad es obligatorio.")
        @Schema(example = "2")
        Long tipoActividadId,

        @NotNull(message = "La duración de la actividad es obligatoria.")
        @Min(value = 5, message = "La duración mínima es de 5 minutos.")
        @Max(value = 1440, message = "La duración no puede superar las 24 horas.")
        @Schema(example = "60")
        Integer duracionMinutos,

        @Schema(example = "7")
        Long ponenteId,

        Long propuestaId,

        Long lineaTematicaId,

        @Schema(example = "presencial")
        ModalidadEvento modalidad,

        @Schema(description = "Puede coincidir con otras actividades simultáneas", example = "false")
        Boolean simultanea
) {

    /** @return true si la actividad se marcó como simultánea. */
    public boolean esSimultanea() {
        return Boolean.TRUE.equals(simultanea);
    }
}
