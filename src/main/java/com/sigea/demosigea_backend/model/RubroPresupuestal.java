package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;

/**
 * Entidad JPA que representa un rubro presupuestal proyectado o estimado
 * para la planificación financiera de un evento académico.
 * <p>
 * Mapea la tabla {@code rubros_presupuestales}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "rubros_presupuestales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubroPresupuestal {

    /** Identificador único del rubro (PK autonumérico). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rubro_id")
    private Long id;

    /** Evento al cual pertenece la proyección del rubro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre descriptivo del rubro presupuestal proyectado. */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    /** Cantidad proyectada requerida (>= 0). */
    @Column(name = "cantidad", precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    /** Costo unitario proyectado por ítem (>= 0). */
    @Column(name = "valor_unitario_proyectado", precision = 14, scale = 2, nullable = false)
    private BigDecimal valorUnitarioProyectado;

    /** Indica si el rubro está activo en la planeación presupuestal del evento. */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /**
     * Calcula el subtotal estimado de este rubro (cantidad * valorUnitarioProyectado).
     *
     * @return Subtotal proyectado del rubro.
     */
    public BigDecimal getSubtotalProyectado() {
        if (cantidad == null || valorUnitarioProyectado == null) {
            return BigDecimal.ZERO;
        }
        return cantidad.multiply(valorUnitarioProyectado);
    }
}
