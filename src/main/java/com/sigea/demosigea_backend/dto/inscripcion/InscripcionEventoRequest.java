package com.sigea.demosigea_backend.dto.inscripcion;

import com.sigea.demosigea_backend.model.CategoriaParticipante;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Petición para registrar una inscripción a un evento (HU-23, Criterio 1).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Datos para la inscripción de un participante a un evento")
public record InscripcionEventoRequest(
        @Schema(description = "Categoría del participante en el evento", example = "estudiante", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La categoría del participante es obligatoria")
        CategoriaParticipante categoria,

        @Schema(description = "ID de la persona a inscribir (opcional, reservado para administradores con permiso de gestión). Si no se envía, se inscribe el usuario autenticado.", example = "10")
        Long personaId
) {
}
