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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la aprobación formal del presupuesto general asignado
 * a un evento con evidencia documental (HU-08, RF07).
 * <p>
 * Mapea la tabla {@code presupuestos_aprobados}.
 * </p>
 * <p>
 * Para garantizar la trazabilidad y el historial de cambios (Criterio 2), cada versión aprobada
 * se conserva como un registro independiente. La versión vigente u oficial tiene
 * {@code estado = 'aprobado'}, mientras que las versiones sustituidas se marcan
 * {@code estado = 'historico'}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "presupuestos_aprobados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresupuestoAprobado {

    /** Identificador único del registro de aprobación (PK autonumérico). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aprobacion_id")
    private Long id;

    /** Evento al que se encuentra asignado el presupuesto aprobado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Monto oficial autorizado para la ejecución del evento (>= 0). */
    @Column(name = "valor_total_aprobado", precision = 14, scale = 2, nullable = false)
    private BigDecimal valorTotalAprobado;

    /** Enlace o ruta al documento que soporta la aprobación formal (acta, resolución, etc.). */
    @Column(name = "evidencia_url", length = 255, nullable = false)
    private String evidenciaUrl;

    /** Fecha y hora en la que se formalizó o registró la aprobación. */
    @Column(name = "fecha_aprobacion", nullable = false)
    private LocalDateTime fechaAprobacion;

    /** Usuario institucional que registró o autorizó la aprobación formal del presupuesto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aprobado_por", nullable = false)
    private Usuario aprobadoPor;

    /** Estado de la versión: 'aprobado' para la versión oficial vigente, 'historico' para versiones anteriores. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoPresupuestoAprobado estado = EstadoPresupuestoAprobado.aprobado;

    @PrePersist
    void antesDeGuardar() {
        if (fechaAprobacion == null) {
            fechaAprobacion = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoPresupuestoAprobado.aprobado;
        }
    }

    /**
     * Marca esta versión como sustituida / histórica para preservar la versión anterior en el historial (Criterio 2).
     */
    public void pasarAHistorico() {
        this.estado = EstadoPresupuestoAprobado.historico;
    }

    /**
     * Indica si esta aprobación corresponde a la versión oficial vigente del evento.
     *
     * @return {@code true} si está aprobada/vigente.
     */
    public boolean esOficial() {
        return this.estado == EstadoPresupuestoAprobado.aprobado;
    }
}
