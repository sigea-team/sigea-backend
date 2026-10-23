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

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la asignación de una propuesta a un evaluador del comité (HU-17).
 * <p>
 * Mapea la tabla {@code asignaciones_evaluacion}. Cada asignación tiene un estado que evoluciona
 * desde {@code pendiente} hasta {@code completada} cuando el evaluador ha registrado todas sus
 * calificaciones por criterio.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "asignaciones_evaluacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignacionEvaluacion {

    /** Identificador único de la asignación (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asignacion_id")
    private Long id;

    /** Propuesta asignada para evaluación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_id", nullable = false)
    private Propuesta propuesta;

    /** Evaluador al que se le asignó la propuesta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comite_evaluador_id", nullable = false)
    private ComiteEvaluador comiteEvaluador;

    /** Fecha y hora en que se realizó la asignación. */
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    /** Estado actual de la asignación en el flujo de evaluación. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoAsignacion estado = EstadoAsignacion.pendiente;

    /** Establece la fecha de asignación automáticamente si no fue suministrada antes de persistir. */
    @PrePersist
    void antesDeInsertar() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }
    }
}
