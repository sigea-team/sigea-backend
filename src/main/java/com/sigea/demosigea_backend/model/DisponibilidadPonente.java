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
 * Entidad JPA que representa una franja horaria de disponibilidad de un ponente en un evento
 * — HU-Salas, Criterio 2.
 * <p>
 * Mapea la tabla {@code disponibilidad_ponentes}. Un ponente puede tener múltiples franjas
 * por evento; cuando se actualiza la disponibilidad, las franjas anteriores se reemplazan por
 * las nuevas y el sistema verifica si alguna actividad programada queda en conflicto (Criterio 5).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "disponibilidad_ponentes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadPonente {

    /** Identificador único de la franja. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disponibilidad_id")
    private Long id;

    /** Ponente cuya disponibilidad se registra. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    /** Evento en el que aplica la disponibilidad. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

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
