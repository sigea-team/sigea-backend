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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA de un registro del historial de modificaciones de un rubro del presupuesto
 * preliminar (HU-07, Criterio 2).
 * <p>
 * Mapea la tabla {@code historial_rubros} (changeset {@code 8-hu07-presupuesto-preliminar.sql}).
 * Cada creación, edición o eliminación de un rubro deja un registro con los valores anteriores y
 * nuevos, el total del presupuesto antes y después del cambio, el usuario y la fecha.
 * </p>
 * <ul>
 *   <li>{@code creacion}: solo valores nuevos.</li>
 *   <li>{@code edicion}: valores anteriores y nuevos.</li>
 *   <li>{@code eliminacion}: solo valores anteriores (lo que se eliminó).</li>
 * </ul>
 * <p>
 * Es de solo inserción: no tiene setters, se marca {@link Immutable} y el trigger
 * {@code trg_historial_rubros_inmutable} rechaza cualquier {@code UPDATE} en la base de datos.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Immutable
@Table(name = "historial_rubros")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HistorialRubro {

    /** Identificador único del registro de historial. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long id;

    /** Rubro modificado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubro_id", nullable = false, updatable = false)
    private RubroPresupuestal rubro;

    /** Evento del presupuesto (permite consultar el historial completo del presupuesto). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false, updatable = false)
    private Evento evento;

    /** Tipo de modificación. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_operacion", length = 20, nullable = false, updatable = false)
    private TipoOperacionRubro tipoOperacion;

    @Column(name = "nombre_anterior", length = 100, updatable = false)
    private String nombreAnterior;

    @Column(name = "cantidad_anterior", precision = 10, scale = 2, updatable = false)
    private BigDecimal cantidadAnterior;

    @Column(name = "valor_unitario_anterior", precision = 14, scale = 2, updatable = false)
    private BigDecimal valorUnitarioAnterior;

    @Column(name = "nombre_nuevo", length = 100, updatable = false)
    private String nombreNuevo;

    @Column(name = "cantidad_nueva", precision = 10, scale = 2, updatable = false)
    private BigDecimal cantidadNueva;

    @Column(name = "valor_unitario_nuevo", precision = 14, scale = 2, updatable = false)
    private BigDecimal valorUnitarioNuevo;

    /** Total del presupuesto preliminar antes del cambio. */
    @Column(name = "total_presupuesto_anterior", precision = 18, scale = 2, nullable = false, updatable = false)
    private BigDecimal totalPresupuestoAnterior;

    /** Total del presupuesto preliminar después del cambio. */
    @Column(name = "total_presupuesto_nuevo", precision = 18, scale = 2, nullable = false, updatable = false)
    private BigDecimal totalPresupuestoNuevo;

    /** Justificación opcional del cambio indicada por el usuario. */
    @Column(name = "motivo", length = 255, updatable = false)
    private String motivo;

    /** Usuario que hizo el cambio ({@code null} si no se pudo identificar). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    /** Fecha y hora del cambio. */
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void antesDeInsertar() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }
}
