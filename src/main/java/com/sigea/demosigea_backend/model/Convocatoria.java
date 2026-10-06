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
 * Entidad JPA que representa una convocatoria pública para recepción de propuestas
 * académicas asociadas a un evento.
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

    /** Evento al que se encuentra asociada la convocatoria. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Título descriptivo de la convocatoria. */
    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    /** Descripción detallada o alcances de la convocatoria. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** Requisitos o términos de referencia para la postulación. */
    @Column(name = "requisitos", columnDefinition = "TEXT")
    private String requisitos;

    /** Fecha y hora de inicio del periodo de recepción de propuestas. */
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;

    /** Fecha y hora límite para el envío de propuestas (debe ser mayor a la de apertura). */
    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    /** Estado del ciclo de vida de la convocatoria. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoConvocatoria estado = EstadoConvocatoria.borrador;

    /**
     * Evalúa si en el momento especificado la convocatoria está abierta para la recepción de propuestas.
     *
     * @param momento Fecha/hora a evaluar (si es {@code null}, usa la hora actual).
     * @return {@code true} si la convocatoria está publicada y el momento se encuentra en el rango [apertura, cierre].
     */
    public boolean estaAbiertaParaPropuestas(LocalDateTime momento) {
        if (momento == null) {
            momento = LocalDateTime.now();
        }
        if (estado != EstadoConvocatoria.publicada) {
            return false;
        }
        // El momento debe ser >= fechaApertura
        if (fechaApertura != null && momento.isBefore(fechaApertura)) {
            return false;
        }
        // El momento debe ser <= fechaCierre (inclusivo: exactamente en fechaCierre sigue abierta).
        // Se usa !isBefore en lugar de isAfter para incluir el instante exacto de cierre.
        if (fechaCierre != null && !momento.isBefore(fechaCierre.plusSeconds(1))) {
            return false;
        }
        return true;
    }
}
