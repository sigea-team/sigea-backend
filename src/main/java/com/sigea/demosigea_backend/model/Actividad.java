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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entidad JPA que representa una actividad programada en la agenda de un evento.
 * <p>
 * Mapea la tabla {@code actividades} tal como existe en la base de datos. Se creó en la HU-05
 * únicamente para consultar si un tipo de actividad o una línea temática está en uso (Criterio 2);
 * <b>la programación y la detección de conflictos pertenecen a HU-21</b>.
 * </p>
 * <p>
 * {@code sala_id} se mapea como columna escalar porque la entidad {@code Sala} aún no existe.
 * Cuando se implemente (HU-20), puede reemplazarse por {@code @ManyToOne Sala sala} con el mismo
 * {@code @JoinColumn}, sin tocar la tabla.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "actividades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Actividad {

    /** Identificador único de la actividad. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actividad_id")
    private Long id;

    /** Evento al que pertenece la actividad. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Propuesta aprobada que origina la actividad (opcional: una conferencia invitada no viene de propuesta). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_id")
    private Propuesta propuesta;

    /** Tipo de actividad (catálogo del evento, HU-05). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_actividad_id", nullable = false)
    private TipoActividad tipoActividad;

    /** Línea temática de la actividad (catálogo del evento, HU-05; opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_tematica_id")
    private LineaTematica lineaTematica;

    /** Nombre de la actividad. */
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    /** Fecha de realización. */
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    /** Hora de inicio (columna {@code TIME}). */
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    /** Hora de finalización (debe ser mayor que la de inicio). */
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    /** ID de la sala asignada (FK a {@code salas}). */
    @Column(name = "sala_id", nullable = false)
    private Long salaId;

    /** Ponente asignado (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ponente_id")
    private Persona ponente;

    /**
     * Modalidad de la actividad. Reutiliza {@link ModalidadEvento} porque la restricción CHECK de
     * {@code actividades.modalidad} admite exactamente los mismos valores.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 20)
    private ModalidadEvento modalidad;

    /** Indica si la actividad puede coincidir intencionalmente en horario con otras. */
    @Builder.Default
    @Column(name = "permite_simultaneidad", nullable = false)
    private boolean permiteSimultaneidad = false;

    /** Estado de la actividad. La columna no tiene restricción CHECK, por eso se mapea como texto. */
    @Builder.Default
    @Column(name = "estado", length = 20, nullable = false)
    private String estado = "programada";
}
