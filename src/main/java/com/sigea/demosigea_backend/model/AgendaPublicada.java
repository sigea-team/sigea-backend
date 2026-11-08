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
import java.time.LocalDateTime;

/**
 * Publicación de la agenda de un día del evento (HU-22, RF39, Criterio 3).
 * <p>
 * Mapea la tabla {@code agendas_publicadas}, que ya existía en el esquema base: una fila por evento y día
 * (índice único {@code evento_id, fecha_dia}). Los participantes solo ven las actividades de los días en estado
 * {@link EstadoAgenda#publicada}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "agendas_publicadas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgendaPublicada {

    /** Identificador único. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agenda_id")
    private Long id;

    /** Evento al que pertenece la agenda. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Día de la agenda. */
    @Column(name = "fecha_dia", nullable = false)
    private LocalDate fechaDia;

    /** Última vez que se publicó la agenda de ese día. */
    @Column(name = "fecha_publicacion")
    private LocalDateTime fechaPublicacion;

    /** Estado de publicación del día. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoAgenda estado = EstadoAgenda.no_publicada;
}
