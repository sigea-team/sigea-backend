package com.sigea.demosigea_backend.dto.asistencia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición del escáner: valor leído del código QR (HU-24, Criterio 2).
 *
 * @param codigo texto leído del QR.
 */
@Schema(description = "Código leído del QR del participante")
public record RegistrarAsistenciaQrRequest(
        @Schema(description = "Valor leído del QR", example = "SIGEA-9f1c2a7be04d4c6b8a3e51d7c2f09b44")
        @NotBlank(message = "El código QR es obligatorio.")
        @Size(max = 100, message = "El código QR no puede superar 100 caracteres.")
        String codigo
) {}
