package com.sigea.demosigea_backend.dto.evento;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO inmutable para crear (HU-04, Criterio 1) o actualizar (HU-04, Criterio 2) la configuración
 * general de un evento.
 * <p>
 * Los datos obligatorios del criterio 1 son nombre, tipo, fechas y modalidad; si alguno falta el
 * sistema rechaza el registro indicando los campos faltantes (HU-04, Criterio 5).
 * </p>
 *
 * @param nombre      Nombre del evento (máx. 200).
 * @param objetivo    Objetivo general (opcional).
 * @param descripcion Descripción (opcional).
 * @param tipo        Tipo de evento, texto libre (máx. 50).
 * @param modalidad   presencial | virtual | hibrida.
 * @param fechaInicio Fecha de inicio.
 * @param fechaFin    Fecha de fin (≥ fecha de inicio).
 * @param semestre    Semestre académico (opcional, formato AAAA-1 / AAAA-2). Si no se envía se deriva de la fecha de inicio.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos generales y de configuración de un evento")
public record EventoRequest(
        @Schema(description = "Nombre del evento", example = "Congreso de Ingeniería de Sistemas")
        @NotBlank(message = "El nombre del evento es obligatorio.")
        @Size(max = 200, message = "El nombre no puede superar los 200 caracteres.")
        String nombre,

        @Schema(description = "Objetivo general del evento")
        String objetivo,

        @Schema(description = "Descripción del evento")
        String descripcion,

        @Schema(description = "Tipo de evento", example = "Congreso")
        @NotBlank(message = "El tipo de evento es obligatorio.")
        @Size(max = 50, message = "El tipo no puede superar los 50 caracteres.")
        String tipo,

        @Schema(description = "Modalidad del evento", example = "presencial")
        @NotNull(message = "La modalidad es obligatoria (presencial, virtual o hibrida).")
        ModalidadEvento modalidad,

        @Schema(description = "Fecha de inicio", example = "2026-10-20")
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de finalización", example = "2026-10-22")
        @NotNull(message = "La fecha de fin es obligatoria.")
        LocalDate fechaFin,

        @Schema(description = "Semestre académico (si se omite se calcula desde la fecha de inicio)", example = "2026-2")
        @Pattern(regexp = "^\\d{4}-[12]$", message = "El semestre debe tener el formato AAAA-1 o AAAA-2.")
        String semestre
) {
    /**
     * Regla de consistencia: la fecha de fin no puede ser anterior a la de inicio
     * (equivalente al CHECK {@code fecha_fin >= fecha_inicio} de la tabla).
     *
     * @return {@code true} si el rango es válido o si alguna fecha es nula (lo reporta @NotNull).
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "La fecha de fin no puede ser anterior a la fecha de inicio.")
    public boolean isRangoFechasValido() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
