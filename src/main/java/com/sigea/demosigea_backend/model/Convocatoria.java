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

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una convocatoria pública para presentación de propuestas académicas.
 * <p>
 * Mapea la tabla {@code convocatorias}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "convocatorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convocatoria {

    /** Identificador único de la convocatoria (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "convocatoria_id")
    private Long id;

    /** Evento al cual está asociada la convocatoria. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Título de la convocatoria. */
    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    /** Descripción de la convocatoria. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** Requisitos o términos de referencia. */
    @Column(name = "requisitos", columnDefinition = "TEXT")
    private String requisitos;

    /** Fecha y hora de apertura de la recepción de propuestas. */
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    /** Fecha y hora de cierre de la recepción de propuestas. */
    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    /** Estado del ciclo de vida de la convocatoria. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoConvocatoria estado = EstadoConvocatoria.borrador;
}
