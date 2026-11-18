package com.sigea.demosigea_backend.dto.asistencia;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resultado del reenvío del código QR (HU-24, Criterio 4). El código es el mismo que ya tenía la inscripción.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Resultado del reenvío del código QR de una inscripción")
public record ReenvioQrResponse(
        @Schema(description = "ID de la inscripción al evento", example = "15")
        Long inscripcionId,

        @Schema(description = "Correo al que se reenvió el QR", example = "maria.perez@ufps.edu.co")
        String correoDestino,

        @Schema(description = "Código reenviado (no cambia)", example = "SIGEA-9f1c2a7be04d4c6b8a3e51d7c2f09b44")
        String codigo,

        @Schema(description = "Si el servicio de correo aceptó el envío", example = "true")
        boolean enviado,

        @Schema(description = "Fecha y hora del reenvío", example = "2026-11-18T08:05:00")
        LocalDateTime fechaReenvio,

        @Schema(description = "Mensaje para mostrar al usuario")
        String mensaje
) {}
