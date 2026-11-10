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
 * HU-22 (changeset 22): una actividad en estado {@value #ESTADO_POR_PROGRAMAR} está registrada sin fecha, horario
 * ni sala, solo con su duración, para que la generación automática de la agenda la ubique. En cualquier otro
 * estado la base de datos exige fecha, horas y sala ({@code actividades_programacion_completa_check}).
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

    /** HU-21: actividad con fecha, horario y sala. */
    public static final String ESTADO_PROGRAMADA = "programada";

    /** HU-22: actividad registrada sin fecha, horario ni sala, pendiente de ubicar. */
    public static final String ESTADO_POR_PROGRAMAR = "por_programar";

    /** Actividad cancelada: no ocupa espacio en la agenda. */
    public static final String ESTADO_CANCELADA = "cancelada";

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

    /** Fecha de realización (null mientras la actividad está por programar, HU-22). */
    @Column(name = "fecha")
    private LocalDate fecha;

    /** Hora de inicio, columna {@code TIME} (null mientras está por programar). */
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    /** Hora de finalización, mayor que la de inicio (null mientras está por programar). */
    @Column(name = "hora_fin")
    private LocalTime horaFin;

    /** HU-22: duración en minutos; la usa la generación automática para ubicar la actividad. */
    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    /**
     * Sala asignada a la actividad (FK a {@code salas}); null mientras está por programar (HU-22).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id")
    private Sala sala;

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
    private String estado = ESTADO_PROGRAMADA;

    /** HU-22: true si la ubicó la generación automática y nadie la ha ajustado a mano todavía. */
    @Builder.Default
    @Column(name = "asignacion_automatica", nullable = false)
    private boolean asignacionAutomatica = false;

    /** @return true si la actividad está registrada sin fecha, horario ni sala (HU-22). */
    public boolean estaPorProgramar() {
        return ESTADO_POR_PROGRAMAR.equalsIgnoreCase(estado);
    }

    /** @return true si la actividad está cancelada. */
    public boolean estaCancelada() {
        return ESTADO_CANCELADA.equalsIgnoreCase(estado);
    }

    /** @return true si tiene fecha, horas y sala (puede aparecer en la agenda). */
    public boolean tieneHorarioCompleto() {
        return fecha != null && horaInicio != null && horaFin != null && sala != null;
    }
}
