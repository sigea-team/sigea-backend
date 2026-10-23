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
 * Entidad JPA que representa una rúbrica de evaluación configurada para una convocatoria (HU-17).
 * <p>
 * Mapea la tabla {@code rubricas_evaluacion}. Cada convocatoria puede tener una rúbrica activa
 * que define los criterios y sus pesos para calificar las propuestas recibidas.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "rubricas_evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricaEvaluacion {

    /** Identificador único de la rúbrica (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rubrica_id")
    private Long id;

    /** Convocatoria a la que pertenece la rúbrica. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

    /** Nombre descriptivo de la rúbrica. */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    /** Indica si la rúbrica está activa. Solo puede haber una activa por convocatoria. */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
