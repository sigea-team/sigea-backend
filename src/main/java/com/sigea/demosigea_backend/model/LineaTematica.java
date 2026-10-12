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
 * Entidad JPA que representa una línea temática (eje o área de conocimiento) de un evento
 * — HU-05, RF07.
 * <p>
 * Mapea la tabla {@code lineas_tematicas}. El catálogo es propio de cada evento y es referenciado
 * por {@code propuestas.linea_tematica_id} (convocatorias) y {@code actividades.linea_tematica_id}
 * (agenda), además de usarse en reportes.
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

    /** Identificador único de la línea temática. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "linea_tematica_id")
    private Long id;

    /** Evento al que pertenece la línea temática. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre de la línea temática (único por evento, sin distinguir mayúsculas). */
    @Column(name = "nombre", length = 120, nullable = false)
    private String nombre;

    /** Descripción del alcance de la línea temática. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
