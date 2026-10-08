package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO para registrar o editar un rubro del presupuesto preliminar (HU-07).
 * <p>
 * <b>Criterio 3:</b> se rechaza el rubro sin nombre, con valor vacío o con valores negativos.
 * El valor 0 sí se acepta (decisión del PO), igual que en los {@code CHECK (>= 0)} de la tabla.
 * Los límites de dígitos corresponden a {@code NUMERIC(10,2)} y {@code NUMERIC(14,2)}: sin ellos un
 * valor demasiado grande llegaría a la base de datos y se respondería 500 en vez de 400.
 * </p>
 *
 * @param nombre                  Nombre del rubro (obligatorio, máx. 100).
 * @param cantidad                Cantidad proyectada (opcional, por defecto 1).
 * @param valorUnitarioProyectado Valor unitario estimado (obligatorio, &ge; 0).
 * @param motivo                  Justificación opcional que se guarda en el historial.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos de un rubro del presupuesto preliminar")
public record RubroRequest(
        @Schema(description = "Nombre del rubro", example = "Transporte de conferencistas")
        @NotBlank(message = "El nombre del rubro es obligatorio.")
        @Size(max = 100, message = "El nombre del rubro no puede superar los 100 caracteres.")
        String nombre,

        @Schema(description = "Cantidad proyectada (unidades, días, personas). Si se omite se toma 1.",
                example = "2", defaultValue = "1")
        @PositiveOrZero(message = "La cantidad no puede ser negativa.")
        @Digits(integer = 8, fraction = 2,
                message = "La cantidad admite máximo 8 dígitos enteros y 2 decimales.")
        BigDecimal cantidad,

        @Schema(description = "Valor unitario estimado en pesos (se acepta 0)", example = "350000")
        @NotNull(message = "El valor estimado del rubro es obligatorio.")
        @PositiveOrZero(message = "El valor estimado del rubro no puede ser negativo.")
        @Digits(integer = 12, fraction = 2,
                message = "El valor estimado admite máximo 12 dígitos enteros y 2 decimales.")
        BigDecimal valorUnitarioProyectado,

        @Schema(description = "Justificación del cambio (opcional, queda en el historial)",
                example = "Se agregó un conferencista internacional")
        @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.")
        String motivo
) {
}
