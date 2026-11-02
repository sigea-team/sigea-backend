package com.sigea.demosigea_backend.dto.actividad;

import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos para programar o reprogramar una actividad de la agenda (HU-21).
 * <p>
 * Criterio 4: la fecha, las horas y la sala son obligatorias; si faltan, la API responde 400 indicando los campos.
 * </p>
 *
 * @param nombre                Nombre de la actividad.
 * @param tipoActividadId       Tipo de actividad del evento (conferencia, taller, etc.).
 * @param fecha                 Fecha de la actividad (dentro de las fechas del evento).
 * @param horaInicio            Hora de inicio.
 * @param horaFin               Hora de fin (posterior a la de inicio).
 * @param salaId                Sala del evento donde se realiza.
 * @param ponenteId             Persona ponente (opcional); debe estar asociada al evento como conferencista.
 * @param propuestaId           Propuesta académica que se presenta (opcional); debe ser de una convocatoria del evento.
 * @param lineaTematicaId       Línea temática del evento (opcional).
 * @param modalidad             Modalidad: presencial, virtual o hibrida (opcional).
 * @param simultanea            Criterio 3: marca la actividad como simultánea intencionalmente (p. ej. talleres paralelos).
 * @param confirmarAdvertencias Confirma que se acepta programar con advertencias (fuera de la disponibilidad registrada).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de una actividad de la agenda")
public record ActividadRequest(
        @NotBlank(message = "El nombre de la actividad es obligatorio.")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres.")
        @Schema(example = "Taller de Inteligencia Artificial")
        String nombre,

        @NotNull(message = "El tipo de actividad es obligatorio.")
        @Schema(example = "2")
        Long tipoActividadId,

        @NotNull(message = "La fecha de la actividad es obligatoria.")
        @Schema(example = "2027-03-10")
        LocalDate fecha,

        @NotNull(message = "La hora de inicio es obligatoria.")
        @Schema(example = "09:00")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria.")
        @Schema(example = "11:00")
        LocalTime horaFin,

        @NotNull(message = "La sala es obligatoria.")
        @Schema(example = "3")
        Long salaId,

        @Schema(example = "7")
        Long ponenteId,

        Long propuestaId,

        Long lineaTematicaId,

        @Schema(example = "presencial")
        ModalidadEvento modalidad,

        @Schema(description = "Marca la actividad como simultánea intencionalmente", example = "false")
        Boolean simultanea,

        @Schema(description = "Acepta programar pese a las advertencias de disponibilidad", example = "false")
        Boolean confirmarAdvertencias
) {

    /** @return true si la actividad se marcó como simultánea. */
    public boolean esSimultanea() {
        return Boolean.TRUE.equals(simultanea);
    }

    /** @return true si se confirmó programar pese a las advertencias. */
    public boolean advertenciasConfirmadas() {
        return Boolean.TRUE.equals(confirmarAdvertencias);
    }
}
