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
 * Entidad JPA que representa un permiso atómico sobre un módulo en SIGEA.
 * <p>
 * Mapea la tabla {@code permisos} y define las acciones que un rol puede ejecutar
 * (ej. ROLES_CREAR, USUARIOS_VER, EVENTOS_CREAR).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
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
     * Código único del permiso en formato UPPER_SNAKE_CASE (ej. "ROLES_CREAR").
     */
    @Column(name = "codigo", length = 50, nullable = false, unique = true)
    private String codigo;

    /**
     * Nombre del módulo funcional al que pertenece el permiso (ej. "ROLES", "USUARIOS", "EVENTOS").
     */
    @Column(name = "modulo", length = 50, nullable = false)
    private String modulo;

    /**
     * Descripción textual de la acción permitida.
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
