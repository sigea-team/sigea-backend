package com.sigea.demosigea_backend.dto.asistencia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Registro manual de asistencia por el organizador, cuando el participante no tiene su QR a mano (HU-24).
 *
 * @param inscripcionId inscripción al evento del participante.
 */
@Schema(description = "Registro manual de asistencia de una inscripción")
public record RegistrarAsistenciaManualRequest(
        @Schema(description = "ID de la inscripción al evento del participante", example = "15")
        @NotNull(message = "La inscripción es obligatoria.")
        Long inscripcionId
) {}
