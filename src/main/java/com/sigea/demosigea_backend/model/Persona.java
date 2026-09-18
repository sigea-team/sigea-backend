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
 * Entidad JPA que representa a una persona natural en el sistema SIGEA.
 * <p>
 * Mapea la tabla {@code personas} y almacena los datos personales, de contacto e identificación
 * de todos los actores del sistema (participantes, ponentes, organizadores, evaluadores, etc.).
 * </p>
 * 
 * @author SIGEA Team
 * @version 1.0
 */
@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona {

    /**
     * Identificador único autoincremental de la persona (clave primaria).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "persona_id")
    private Long id;

    /**
     * Tipo de documento de identidad (ej. CC, TI, CE, PASAPORTE).
     */
    @Column(name = "tipo_documento", length = 20, nullable = false)
    private String tipoDocumento;

    /**
     * Número único del documento de identidad.
     */
    @Column(name = "numero_documento", length = 30, nullable = false, unique = true)
    private String numeroDocumento;

    /**
     * Nombres de la persona.
     */
    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    /**
     * Apellidos de la persona.
     */
    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    /**
     * Correo electrónico único de contacto y vinculación con la cuenta de usuario.
     */
    @Column(name = "correo", length = 150, nullable = false, unique = true)
    private String correo;

    /**
     * Número de teléfono de contacto (opcional).
     */
    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Nombre de la institución, empresa o universidad a la que pertenece la persona (opcional).
     */
    @Column(name = "afiliacion_institucional", length = 150)
    private String afiliacionInstitucional;
}
