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
 * Asociación de un conferencista a un evento (HU-12, RF16 y RF17).
 * <p>
 * Mapea la tabla {@code participaciones_conferencista}, que ya existía en el esquema base.
 * El conjunto de participaciones de una persona es su historial en los distintos eventos del
 * Programa (RF17). Una persona solo puede estar una vez en cada evento
 * (índice {@code ux_participaciones_conferencista_evento_persona}, Criterio 3).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "participaciones_conferencista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipacionConferencista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participacion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participacion", length = 20, nullable = false)
    private TipoParticipacionConferencista tipoParticipacion;

    /** Tema o título de la conferencia en este evento (opcional). */
    @Column(name = "tema", length = 200)
    private String tema;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void antesDeInsertar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
