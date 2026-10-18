package com.sigea.demosigea_backend.dto.propuesta;

import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.RolAutor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de respuesta con la información de un autor o ponente vinculado a una propuesta académica.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Schema(description = "Información de un autor o ponente vinculado a una propuesta académica")
public record AutorResponse(

        @Schema(description = "ID de la persona", example = "1")
        Long personaId,

        @Schema(description = "Nombre(s) del autor", example = "Carlos Andrés")
        String nombres,

        @Schema(description = "Apellidos del autor", example = "Gómez Pérez")
        String apellidos,

        @Schema(description = "Nombre completo del autor", example = "Carlos Andrés Gómez Pérez")
        String nombreCompleto,

        @Schema(description = "Correo electrónico del autor", example = "carlos.gomez@ufps.edu.co")
        String correo,

        @Schema(description = "Filiación o institución académica de procedencia", example = "Universidad Francisco de Paula Santander")
        String filiacion,

        @Schema(description = "ID de la afiliación institucional (si aplica)", example = "1")
        Long afiliacionId,

        @Schema(description = "Rol específico del autor en la propuesta (autor_principal, coautor, ponente)", example = "autor_principal")
        RolAutor rol,

        @Schema(description = "Descripción legible del rol", example = "Autor Principal")
        String rolDescripcion,

        @Schema(description = "Orden de prelación en la autoría", example = "1")
        Integer orden,

        @Schema(description = "Tipo de documento de identidad", example = "CC")
        String tipoDocumento,

        @Schema(description = "Número de documento de identidad", example = "1098765432")
        String numeroDocumento,

        @Schema(description = "Teléfono de contacto", example = "+573001234567")
        String telefono
) {
    /**
     * Mapea una entidad {@link PropuestaAutor} a su correspondiente DTO de respuesta.
     *
     * @param pa Entidad JPA de la relación propuesta-autor.
     * @return DTO {@link AutorResponse}.
     */
    public static AutorResponse fromEntity(PropuestaAutor pa) {
        Persona p = pa.getPersona();
        String nombres = p != null ? p.getNombres() : "";
        String apellidos = p != null ? p.getApellidos() : "";
        String nombreCompleto = (nombres + (apellidos != null && !apellidos.isBlank() ? " " + apellidos : "")).trim();
        String filiacion = "";
        Long afiliacionId = null;
        if (p != null && p.getAfiliacion() != null) {
            filiacion = p.getAfiliacion().getNombreAfiliacion();
            afiliacionId = p.getAfiliacion().getId();
        }

        return new AutorResponse(
                p != null ? p.getId() : null,
                nombres,
                apellidos,
                nombreCompleto,
                p != null ? p.getCorreo() : null,
                filiacion,
                afiliacionId,
                pa.getRol(),
                pa.getRol() != null ? pa.getRol().getDescripcion() : null,
                pa.getOrden(),
                p != null ? p.getTipoDocumento() : null,
                p != null ? p.getNumeroDocumento() : null,
                p != null ? p.getTelefono() : null
        );
    }
}
