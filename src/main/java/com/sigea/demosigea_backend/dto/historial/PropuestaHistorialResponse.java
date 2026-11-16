package com.sigea.demosigea_backend.dto.historial;

import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.RolAutor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Propuesta postulada por la persona a una convocatoria del evento (HU-27, Criterio 1).
 *
 * @param propuestaId Identificador de la propuesta.
 * @param titulo      Título de la propuesta.
 * @param estado      Estado de la propuesta.
 * @param rolAutor    Rol de la persona en la propuesta (autor_principal, coautor, ponente).
 */
@Schema(description = "Propuesta en la que figura la persona como autor o ponente")
public record PropuestaHistorialResponse(
        Long propuestaId,
        String titulo,
        EstadoPropuesta estado,
        RolAutor rolAutor
) {
}
