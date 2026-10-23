package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la calificación de un criterio específico dentro de una evaluación (HU-17).
 * <p>
 * Mapea la tabla {@code evaluaciones}. Cada registro vincula una asignación con un criterio de la
 * rúbrica y almacena la calificación numérica y las observaciones del evaluador.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "evaluaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluacion {

    /** Identificador único de la evaluación (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluacion_id")
    private Long id;

    /** Asignación de evaluación a la que pertenece esta calificación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private AsignacionEvaluacion asignacion;

    /** Criterio de la rúbrica que se está calificando. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterio_id", nullable = false)
    private CriterioRubrica criterio;

    /** Calificación numérica otorgada al criterio (de 0.0 a 10.0). */
    @Column(name = "calificacion", precision = 5, scale = 2, nullable = false)
    private BigDecimal calificacion;

    /** Observaciones o comentarios del evaluador sobre el criterio (opcional). */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /** Fecha y hora en que se registró la evaluación. */
    @Column(name = "fecha_evaluacion", nullable = false)
    private LocalDateTime fechaEvaluacion;

    /** Establece la fecha de evaluación automáticamente si no fue suministrada antes de persistir. */
    @PrePersist
    void antesDeInsertar() {
        if (fechaEvaluacion == null) {
            fechaEvaluacion = LocalDateTime.now();
        }
    }
}
