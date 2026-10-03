package com.sigea.demosigea_backend.dto.auditoria;

import com.sigea.demosigea_backend.model.Auditoria;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Representación de un registro de auditoría para el cliente (HU-03).
 */
@Schema(description = "Registro del log de auditoría de operaciones críticas")
public record AuditoriaResponse(
        @Schema(example = "15") Long id,
        @Schema(example = "2026-09-28T10:15:30") LocalDateTime fechaHora,
        @Schema(example = "3") Long usuarioId,
        @Schema(example = "admin@ufps.edu.co") String usuarioCorreo,
        @Schema(example = "Ana Pérez") String usuarioNombre,
        @Schema(example = "ROL_ACTUALIZADO") String accion,
        @Schema(example = "Modificación de un rol o de sus permisos") String accionDescripcion,
        @Schema(example = "roles") String entidad,
        @Schema(example = "4") Long entidadId,
        @Schema(description = "Datos afectados en formato JSON",
                example = "{\"antes\":{\"nombre\":\"EVALUADOR\"},\"despues\":{\"nombre\":\"EVALUADOR_PAR\"}}")
        String detalle
) {

    public static AuditoriaResponse fromEntity(Auditoria a) {
        Usuario u = a.getUsuario();
        Persona p = u != null ? u.getPersona() : null;
        String descripcion = Arrays.stream(TipoOperacionAuditoria.values())
                .filter(t -> t.name().equals(a.getAccion()))
                .map(TipoOperacionAuditoria::getDescripcion)
                .findFirst()
                .orElse(a.getAccion());
        return new AuditoriaResponse(
                a.getId(),
                a.getFechaHora(),
                u != null ? u.getId() : null,
                p != null ? p.getCorreo() : null,
                p != null ? (p.getNombres() + " " + p.getApellidos()).trim() : "Sistema",
                a.getAccion(),
                descripcion,
                a.getEntidad(),
                a.getEntidadId(),
                a.getDetalle()
        );
    }
}
