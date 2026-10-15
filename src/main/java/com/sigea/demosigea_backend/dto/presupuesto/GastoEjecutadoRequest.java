package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petición para registrar un gasto ejecutado contra un rubro del evento (HU-09, RF11).
 * <p>
 * <b>Criterio 3:</b> un gasto sin valor (o con valor negativo) se rechaza con 400
 * {@code VALIDACION_FALLIDA}, indicando el campo con error. El valor 0 se acepta y el soporte es opcional.
 * </p>
 *
 * @param rubroId     Rubro vigente del evento al que se imputa el gasto.
 * @param valor       Valor ejecutado en pesos (mayor o igual a cero, máx. 12 enteros y 2 decimales).
 * @param fechaGasto  Fecha del gasto (no puede ser futura).
 * @param soporteUrl  Enlace, ruta o referencia del soporte (opcional).
 * @param descripcion Concepto del gasto (opcional).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de un gasto ejecutado")
public record GastoEjecutadoRequest(

        @Schema(description = "ID del rubro vigente al que se imputa el gasto", example = "7")
        @NotNull(message = "El rubro del gasto es obligatorio.")
        Long rubroId,

        @Schema(description = "Valor ejecutado en pesos", example = "350000.00")
        @NotNull(message = "El valor del gasto es obligatorio.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor del gasto no puede ser negativo.")
        @Digits(integer = 12, fraction = 2, message = "El valor admite máximo 12 enteros y 2 decimales.")
        BigDecimal valor,

        @Schema(description = "Fecha en que se realizó el gasto", example = "2026-10-14")
        @NotNull(message = "La fecha del gasto es obligatoria.")
        @PastOrPresent(message = "La fecha del gasto no puede ser futura.")
        LocalDate fechaGasto,

        @Schema(description = "Enlace, ruta o referencia del soporte del gasto (opcional)", example = "https://archivos.ufps.edu.co/gastos/factura-001.pdf")
        @Size(max = 255, message = "El soporte no puede superar los 255 caracteres.")
        String soporteUrl,

        @Schema(description = "Concepto del gasto", example = "Tiquetes aéreos del conferencista invitado")
        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres.")
        String descripcion
) {}
