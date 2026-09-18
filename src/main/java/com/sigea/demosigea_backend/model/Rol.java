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
 * Entidad JPA que representa los roles del sistema dentro de la plataforma SIGEA.
 * <p>
 * Mapea la tabla {@code roles} y define los perfiles de acceso o privilegios que
 * pueden ser asignados a los usuarios (ej. ROLE_ADMIN, ROLE_DOCENTE, ROLE_ESTUDIANTE).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    /**
     * Identificador único del rol en la base de datos (Clave Primaria Autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rol_id")
    private Long id;

    /**
     * Nombre único del rol dentro del sistema (ej. "ROLE_ADMIN").
     */
    @Column(name = "nombre", length = 50, nullable = false, unique = true)
    private String nombre;

    /**
     * Descripción detallada sobre las facultades y alcances asignados a este rol.
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}