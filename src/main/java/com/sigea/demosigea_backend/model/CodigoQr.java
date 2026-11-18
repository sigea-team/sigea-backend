package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Código QR único de una inscripción al evento (HU-24, RF44). Mapea la tabla {@code codigos_qr}.
 * <p>
 * Hay un solo código por inscripción ({@code UNIQUE (inscripcion_id)}) y el código es único en todo el
 * sistema ({@code UNIQUE (codigo)}). El mismo código sirve para registrar la asistencia en todas las
 * actividades del evento a las que la persona esté inscrita, y el reenvío nunca genera uno distinto.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "codigos_qr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoQr {

    /** Identificador único autoincremental del código. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_id")
    private Long id;

    /** Inscripción al evento a la que pertenece el código. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscripcion_id", nullable = false, unique = true)
    private InscripcionEvento inscripcion;

    /** Valor que se codifica en el QR. Aleatorio y sin datos personales. */
    @Column(name = "codigo", length = 100, nullable = false, unique = true)
    private String codigo;

    /** Fecha y hora de generación. */
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    /** Si es {@code false} el código no se acepta para registrar asistencia. */
    @Builder.Default
    @Column(name = "vigente", nullable = false)
    private Boolean vigente = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (fechaGeneracion == null) {
            fechaGeneracion = LocalDateTime.now();
        }
        if (vigente == null) {
            vigente = Boolean.TRUE;
        }
    }

    /** @return {@code true} si el código se puede usar para registrar asistencia. */
    public boolean estaVigente() {
        return Boolean.TRUE.equals(vigente);
    }
}
