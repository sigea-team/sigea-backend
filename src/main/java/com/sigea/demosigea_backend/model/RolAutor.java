package com.sigea.demosigea_backend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeración que representa el rol específico que desempeña un autor o ponente
 * en una propuesta académica (HU-13, Criterio 2).
 * <p>
 * Valores permitidos:
 * <ul>
 *   <li>{@link #autor_principal}: autor principal o primer autor responsable del trabajo.</li>
 *   <li>{@link #coautor}: colaborador o coautor de la propuesta.</li>
 *   <li>{@link #ponente}: autor designado como expositor o disertante en el evento.</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
@RequiredArgsConstructor
public enum RolAutor {

    /** Autor principal o líder del trabajo postulado. */
    autor_principal("Autor Principal"),

    /** Coautor o colaborador del trabajo. */
    coautor("Coautor"),

    /** Ponente o expositor que presentará el trabajo en el evento. */
    ponente("Ponente");

    private final String descripcion;

    @JsonValue
    public String getValor() {
        return this.name();
    }

    /**
     * Parsea un valor de texto al rol correspondiente de forma tolerante a mayúsculas,
     * minúsculas, espacios y guiones bajos.
     *
     * @param valor Texto ingresado por el cliente.
     * @return {@link RolAutor} correspondiente.
     * @throws IllegalArgumentException si el valor no corresponde a ninguno de los roles válidos.
     */
    @JsonCreator
    public static RolAutor fromString(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String normalizado = valor.trim().toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");

        for (RolAutor rol : RolAutor.values()) {
            if (rol.name().equalsIgnoreCase(normalizado)) {
                return rol;
            }
        }
        throw new IllegalArgumentException(String.format(
                "Rol de autor inválido: '%s'. Valores permitidos: autor principal (autor_principal), coautor, ponente.",
                valor));
    }
}
