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
 * Registro de asistencia de un participante a una actividad (HU-24, RF45). Mapea la tabla {@code asistencias}.
 * <p>
 * El índice único {@code asistencias_inscripcion_id_actividad_id_idx (inscripcion_id, actividad_id)} impide
 * registrar dos veces la misma inscripción en la misma actividad (Criterio 3).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "asistencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {

    /** Identificador único autoincremental de la asistencia. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asistencia_id")
    private Long id;

    /** Inscripción al evento del participante que asistió. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private InscripcionEvento inscripcion;

    /** Actividad a la que asistió. La columna admite NULL, pero HU-24 siempre registra la actividad. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Fecha y hora en que se registró la asistencia (Criterio 2). */
    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;

    /** Método con el que se registró: lectura del QR o registro manual. */
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_registro", length = 20, nullable = false)
    private MetodoRegistroAsistencia metodoRegistro;

    @PrePersist
    protected void onCreate() {
        if (fechaHoraRegistro == null) {
            fechaHoraRegistro = LocalDateTime.now();
        }
    }
}
