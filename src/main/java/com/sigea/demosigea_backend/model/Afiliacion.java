package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa una afiliación institucional en el sistema SIGEA.
 * <p>
 * Mapea la tabla {@code afiliaciones} y actúa como catálogo de opciones de procedencia
 * institucional que pueden ser seleccionadas por una {@link Persona} al registrarse.
 * Ejemplos: 'Estudiante UFPS', 'Particular', 'Estudiante Internacional', 'Estudiante Externo UFPS'.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "afiliaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Afiliacion {

    /**
     * Identificador único autoincremental de la afiliación (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Nombre descriptivo único de la afiliación institucional.
     * Ej: 'Estudiante UFPS', 'Particular', 'Estudiante Internacional'.
     */
    @Column(name = "nombre_afiliacion", length = 100, nullable = false, unique = true)
    private String nombreAfiliacion;
}
