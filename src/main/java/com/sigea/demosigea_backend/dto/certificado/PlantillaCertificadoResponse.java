package com.sigea.demosigea_backend.dto.certificado;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Plantilla de los certificados del evento: fondo y firmantes (HU-26).
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Plantilla de los certificados del evento")
public record PlantillaCertificadoResponse(
        @Schema(description = "ID del evento", example = "3") Long eventoId,
        @Schema(description = "Si tiene imagen de fondo; sin fondo se usa el diseño predeterminado", example = "true")
        boolean tieneFondo,
        @Schema(description = "Ruta de la imagen de fondo (JPEG, requiere sesión), o null",
                example = "/api/v1/eventos/3/certificados/plantilla/fondo") String urlFondo,
        @Schema(description = "Firmantes en el orden en que se imprimen") List<FirmanteCertificadoResponse> firmantes,
        @Schema(description = "Máximo de firmantes", example = "4") int maximoFirmantes,
        @Schema(description = "Si la plantilla se puede modificar (no hay certificados emitidos y el evento no está archivado)",
                example = "true") boolean editable,
        @Schema(description = "Motivo por el que no se puede modificar, o null",
                example = "Ya se emitieron 12 certificados con esta plantilla; todos deben ser iguales.")
        String motivoNoEditable,
        @Schema(description = "Ruta de la vista previa en PDF", example = "/api/v1/eventos/3/certificados/plantilla/vista-previa")
        String urlVistaPrevia,
        @Schema(description = "Si tiene lo necesario para habilitar los certificados (fondo y al menos una firma)",
                example = "false") boolean completa,
        @Schema(description = "Lo que falta para habilitar; vacío si está completa",
                example = "[\"Suba la imagen de fondo del certificado.\"]") List<String> faltantes,
        @Schema(description = "Si los certificados del evento ya fueron habilitados", example = "false") boolean habilitada,
        @Schema(description = "Fecha y hora de habilitación, o null", example = "2026-12-04T10:15:00")
        LocalDateTime fechaHabilitacion
) {
}
