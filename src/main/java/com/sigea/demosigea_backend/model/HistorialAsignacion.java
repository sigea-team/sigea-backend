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
 * Registro inmutable de una asignación o reasignación de una propuesta a un evaluador (HU-16, Criterio 3).
 * <p>
 * Mapea la tabla {@code historial_asignaciones} (changeset Liquibase {@code 13-hu16-asignacion-evaluadores.sql}).
 * La base de datos impide modificar un registro mediante un trigger; solo se inserta.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "historial_asignaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long id;

    /** Asignación a la que pertenece el movimiento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignacion_id", nullable = false, updatable = false)
    private AsignacionEvaluacion asignacion;

    /** Propuesta asignada (se guarda para consultar el historial completo de la propuesta). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_id", nullable = false, updatable = false)
    private Propuesta propuesta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", length = 20, nullable = false, updatable = false)
    private TipoOperacionAsignacion tipoOperacion;

    /** Evaluador que tenía la asignación antes del movimiento ({@code null} en una asignación inicial). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comite_evaluador_anterior_id", updatable = false)
    private ComiteEvaluador evaluadorAnterior;

    /** Evaluador que queda con la asignación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comite_evaluador_nuevo_id", nullable = false, updatable = false)
    private ComiteEvaluador evaluadorNuevo;

    /** Indica que el organizador confirmó la asignación pese a una advertencia de conflicto de interés. */
    @Builder.Default
    @Column(name = "conflicto_advertido", nullable = false, updatable = false)
    private Boolean conflictoAdvertido = false;

    /** Motivo informado por el organizador (opcional). */
    @Column(name = "motivo", length = 255, updatable = false)
    private String motivo;

    /** Usuario que realizó el movimiento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void antesDeInsertar() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }
}
