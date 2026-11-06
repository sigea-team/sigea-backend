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

import java.math.BigDecimal;

/**
 * Entidad JPA que representa un criterio de evaluación dentro de una rúbrica (HU-17).
 * <p>
 * Mapea la tabla {@code criterios_rubrica}. Cada criterio tiene un peso porcentual que determina
 * su contribución a la puntuación ponderada final de la propuesta.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "criterios_rubrica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriterioRubrica {

    /** Límite inferior permitido para cualquier escala (coincide con el mínimo de calificación de HU-17). */
    public static final BigDecimal ESCALA_MINIMA_POR_DEFECTO = new BigDecimal("0.00");

    /** Límite superior permitido para cualquier escala (coincide con el máximo de calificación de HU-17). */
    public static final BigDecimal ESCALA_MAXIMA_POR_DEFECTO = new BigDecimal("10.00");

    /** Identificador único del criterio (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "criterio_id")
    private Long id;

    /** Rúbrica a la que pertenece el criterio. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubrica_id", nullable = false)
    private RubricaEvaluacion rubrica;

    /** Nombre descriptivo del criterio (ej. "Originalidad", "Pertinencia"). */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    /**
     * Peso porcentual del criterio en la puntuación ponderada (debe ser > 0 y <= 100).
     * La suma de todos los pesos de la rúbrica debe ser 100.
     */
    @Column(name = "peso_porcentual", precision = 5, scale = 2, nullable = false)
    private BigDecimal pesoPorcentual;

    /** Valor mínimo de la escala de calificación del criterio (HU-15). Entre 0 y 10. */
    @Builder.Default
    @Column(name = "escala_minima", precision = 5, scale = 2, nullable = false)
    private BigDecimal escalaMinima = ESCALA_MINIMA_POR_DEFECTO;

    /** Valor máximo de la escala de calificación del criterio (HU-15). Entre 0 y 10 y mayor que el mínimo. */
    @Builder.Default
    @Column(name = "escala_maxima", precision = 5, scale = 2, nullable = false)
    private BigDecimal escalaMaxima = ESCALA_MAXIMA_POR_DEFECTO;
}
