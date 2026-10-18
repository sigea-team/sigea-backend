package com.sigea.demosigea_backend.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Clave primaria compuesta para la relación {@link PropuestaAutor}.
 * <p>
 * Mapea la clave primaria (propuesta_id, persona_id) de la tabla {@code propuestas_autores}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PropuestaAutorId implements Serializable {

    private static final long serialVersionUID = 1L;

    /** ID de la propuesta (referencia a {@link Propuesta#getId()}). */
    private Long propuesta;

    /** ID de la persona (referencia a {@link Persona#getId()}). */
    private Long persona;
}
