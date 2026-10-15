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
 * Entidad JPA del catálogo de tipos de evento (congreso, seminario, taller, hackatón, etc.).
 * <p>
 * Mapea la tabla {@code tipos_evento} (changeset tipos-evento-001). A diferencia de los tipos de
 * actividad (HU-05), este catálogo es <b>global</b>: lo comparten todos los eventos y ediciones.
 * Cada evento lo referencia mediante {@code eventos.tipo_evento_id}, lo que permite incorporar nuevos
 * tipos de evento sin modificar el software (RNF15).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "tipos_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoEvento {

    /** Identificador único del tipo de evento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_evento_id")
    private Long id;

    /** Nombre del tipo (único sin distinguir mayúsculas). */
    @Column(name = "nombre", length = 50, nullable = false)
    private String nombre;

    /** Descripción o características del tipo de evento. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
