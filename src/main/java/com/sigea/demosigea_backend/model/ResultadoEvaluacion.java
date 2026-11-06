package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entidad JPA que representa el resultado de evaluación calculado para una propuesta (HU-17).
 * <p>
 * Mapea la tabla {@code resultados_evaluacion}. El resultado se genera automáticamente cuando todas
 * las asignaciones de la propuesta han sido completadas, calculando la puntuación ponderada con base
 * en los pesos de los criterios de la rúbrica y el promedio de calificaciones de todos los evaluadores.
 * La clasificación se obtiene con los umbrales de la convocatoria y se aplica a la propuesta en HU-18.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "resultados_evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoEvaluacion {

    /** Identificador único del resultado (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resultado_id")
    private Long id;

    /** Propuesta a la que corresponde el resultado (relación única). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_id", nullable = false)
    private Propuesta propuesta;

    /** Puntuación ponderada calculada a partir de los pesos de la rúbrica y los promedios por criterio. */
    @Column(name = "puntuacion_ponderada", precision = 5, scale = 2, nullable = false)
    private BigDecimal puntuacionPonderada;

    /** Clasificación final de la propuesta en función de la puntuación ponderada. */
    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion", length = 30, nullable = false)
    private ClasificacionResultado clasificacion;

    /** Fecha y hora en que se calculó o actualizó el resultado. */
    @Column(name = "fecha_calculo", nullable = false)
    private LocalDateTime fechaCalculo;

    /**
     * HU-18, Criterio 2: la clasificación la fijó manualmente el administrador (con motivo en el historial).
     * El recálculo automático de HU-17 actualiza la puntuación pero no reemplaza esta clasificación.
     */
    @Builder.Default
    @Column(name = "ajuste_manual", nullable = false)
    private Boolean ajusteManual = false;

    /** HU-18: fecha en que la clasificación se aplicó a la propuesta ({@code null} si aún no se aplicó). */
    @Column(name = "fecha_clasificacion")
    private LocalDateTime fechaClasificacion;

    /** Establece la fecha de cálculo automáticamente si no fue suministrada antes de persistir. */
    @PrePersist
    void antesDeInsertar() {
        if (fechaCalculo == null) {
            fechaCalculo = LocalDateTime.now();
        }
        if (ajusteManual == null) {
            ajusteManual = false;
        }
    }
}
