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

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad JPA que representa una franja horaria de disponibilidad de una sala — HU-Salas, Criterio 1.
 * <p>
 * Mapea la tabla {@code disponibilidad_salas}. Una sala puede tener múltiples franjas; una actividad
 * programada en la sala está cubierta si existe al menos una franja que contenga completamente
 * su período (mismo día, hora_inicio ≤ actividad.hora_inicio y hora_fin ≥ actividad.hora_fin).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "disponibilidad_salas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadSala {

    /** Identificador único de la franja. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disponibilidad_id")
    private Long id;

    /** Sala a la que pertenece la franja horaria. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    /** Fecha de disponibilidad. */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Hora de inicio de la franja (debe ser anterior a hora_fin). */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /** Hora de fin de la franja (debe ser posterior a hora_inicio). */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
