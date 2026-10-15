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
import java.time.LocalDate;

/**
 * Entidad JPA que representa un gasto real ejecutado contra un rubro presupuestal del evento
 * (HU-09, RF11).
 * <p>
 * Mapea la tabla {@code gastos_ejecutados}. El acumulado ejecutado de un rubro es la suma de
 * {@code valor} de sus gastos; el porcentaje de ejecución presupuestal del evento se calcula sobre
 * el presupuesto aprobado vigente (RF12). Ninguno de los dos se persiste: se calculan siempre a
 * partir de los gastos guardados, por lo que no pueden quedar desactualizados.
 * </p>
 * <p>
 * El soporte ({@code soporte_url}) es opcional: puede ser un enlace, una ruta o una referencia en texto.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "gastos_ejecutados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GastoEjecutado {

    /** Identificador único del gasto. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gasto_id")
    private Long id;

    /** Rubro presupuestal contra el que se ejecuta el gasto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rubro_id", nullable = false)
    private RubroPresupuestal rubro;

    /** Descripción del gasto (concepto, proveedor, factura...). */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /** Valor ejecutado en pesos (mayor o igual a cero; CHECK valor >= 0). */
    @Column(name = "valor", precision = 14, scale = 2, nullable = false)
    private BigDecimal valor;

    /** Fecha en que se realizó el gasto. */
    @Column(name = "fecha_gasto", nullable = false)
    private LocalDate fechaGasto;

    /** Enlace o ruta al soporte del gasto (factura, cuenta de cobro, recibo). */
    @Column(name = "soporte_url", length = 255)
    private String soporteUrl;

    /** Usuario que registró el gasto. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;
}
