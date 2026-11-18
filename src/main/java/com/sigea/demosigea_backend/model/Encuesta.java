package com.sigea.demosigea_backend.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encuesta de satisfacción del evento o de una actividad (HU-25, RF46). Mapea la tabla {@code encuestas}.
 * <p>
 * Sin actividad, es la encuesta general del evento y la responden quienes asistieron a alguna de sus
 * actividades; con actividad, solo quienes asistieron a esa actividad (HU-24, RF45).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "encuestas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encuesta {

    /** Identificador único autoincremental de la encuesta. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "encuesta_id")
    private Long id;

    /** Evento al que pertenece la encuesta. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Actividad evaluada; {@code null} si la encuesta es del evento en general. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Título visible para los participantes. */
    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    /** Instrucciones o presentación de la encuesta. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** Si es {@code true}, las respuestas se guardan sin la persona (Criterio 2). */
    @Builder.Default
    @Column(name = "anonima", nullable = false)
    private boolean anonima = false;

    /** Estado de la encuesta. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoEncuesta estado = EstadoEncuesta.borrador;

    /** Fecha de creación. */
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /** Fecha en que se publicó (Criterio 1). */
    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Fecha límite para responder, o fecha en que se cerró. {@code null}: abierta mientras esté publicada. */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** Usuario que creó la encuesta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por")
    private Usuario creadaPor;

    /** Preguntas en el orden en que se muestran. */
    @Builder.Default
    @OneToMany(mappedBy = "encuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    private List<PreguntaEncuesta> preguntas = new ArrayList<>();

    /** Agrega una pregunta y fija la relación inversa. */
    public void agregarPregunta(PreguntaEncuesta pregunta) {
        pregunta.setEncuesta(this);
        preguntas.add(pregunta);
    }

    /** Indica si la encuesta es de una actividad (y no del evento en general). */
    public boolean esDeActividad() {
        return actividad != null;
    }

    /**
     * Indica si en el momento dado la encuesta recibe respuestas: publicada y sin pasar la fecha de cierre.
     */
    public boolean recibeRespuestas(LocalDateTime ahora) {
        return estado == EstadoEncuesta.publicada && (fechaCierre == null || ahora.isBefore(fechaCierre));
    }

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
