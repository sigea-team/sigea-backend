package com.sigea.demosigea_backend.dto.presupuesto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Petición para registrar o actualizar el presupuesto aprobado formalmente para un evento (HU-08).
 * <ul>
 *   <li><b>Criterio 1:</b> El monto aprobado puede diferir del presupuesto preliminar.</li>
 *   <li><b>Criterio 3:</b> La evidencia de aprobación es obligatoria para confirmar el registro.</li>
 * </ul>
 *
 * @param valorTotalAprobado Monto oficial autorizado para la ejecución del evento (>= 0).
 * @param evidenciaUrl       Enlace o soporte documental de aprobación (acta, resolución, acuerdo formal).
 * @param aprobadoPorId      ID del usuario institucional que autoriza (opcional; si es nulo, se infiere del JWT autenticado).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Petición para registrar o actualizar el presupuesto aprobado formalmente para un evento")
public record PresupuestoAprobadoRequest(

        @Schema(description = "Monto oficial autorizado para la ejecución del evento", example = "50000000.00")
        @NotNull(message = "El valor total aprobado es obligatorio.")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor total aprobado debe ser mayor o igual a cero.")
        BigDecimal valorTotalAprobado,

        @Schema(description = "Soporte o URL de la evidencia documental de aprobación (acta, resolución, acuerdo)", example = "https://archivos.ufps.edu.co/presupuestos/acta-aprobacion-2026.pdf")
        @NotBlank(message = "La evidencia de aprobación es obligatoria.")
        @Size(max = 255, message = "La URL de evidencia no puede superar los 255 caracteres.")
        String evidenciaUrl,

        @Schema(description = "ID del usuario que aprueba formalmente (opcional si se infiere del token JWT autenticado)", example = "1")
        Long aprobadoPorId
) {}
