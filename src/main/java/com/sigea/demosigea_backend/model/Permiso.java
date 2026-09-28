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
 * Entidad JPA que representa un permiso atómico sobre un módulo funcional en la plataforma SIGEA.
 * <p>
 * Mapea la tabla {@code permisos} de la base de datos PostgreSQL según el esquema canónico.
 * Los permisos definen privilegios específicos (ej. "ROLES_CREAR", "USUARIOS_VER", "EVENTOS_EDITAR")
 * agrupados por módulo funcional ("ROLES", "USUARIOS", "EVENTOS", etc.) que pueden ser asignados
 * a los diferentes perfiles o roles a través de la tabla asociativa {@code roles_permisos}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 * @see Rol
 */
@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permiso {

    /**
     * Identificador único del permiso en la base de datos (Clave Primaria Autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permiso_id")
    private Long id;

    /**
     * Código único del permiso en formato UPPER_SNAKE_CASE (ej. "ROLES_CREAR", "EVENTOS_EDITAR").
     * Utilizado para validaciones de autorización a nivel de método con {@code @PreAuthorize}.
     */
    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    /**
     * Nombre del módulo funcional al que pertenece el permiso (ej. "ROLES", "USUARIOS", "EVENTOS").
     */
    @Column(name = "modulo", length = 50, nullable = false)
    private String modulo;

    /**
     * Descripción textual legible y detallada sobre la acción o facultad que autoriza este permiso.
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
