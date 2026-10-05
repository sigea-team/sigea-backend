package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa una línea temática o eje disciplinar de un evento.
 * <p>
 * Mapea la tabla {@code lineas_tematicas}. Cada línea temática pertenece a un evento y
 * sirve para clasificar convocatorias, propuestas y actividades.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "lineas_tematicas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaTematica {

    /** Identificador único de la línea temática (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linea_tematica_id")
    private Long id;

    /** Evento al cual pertenece la línea temática. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre del eje temático. */
    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;

    /** Descripción detallada o alcances de la línea temática. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
