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
import java.math.RoundingMode;

/**
 * Entidad JPA que representa un rubro del presupuesto preliminar de un evento (HU-07, RF06).
 * <p>
 * Mapea la tabla {@code rubros_presupuestales}. El presupuesto preliminar no es una tabla propia:
 * es el conjunto de rubros <b>vigentes</b> ({@code activo = true}) del evento, y su total es la suma
 * de {@code cantidad × valor_unitario_proyectado} de cada uno (Criterio 1).
 * </p>
 * <p>
 * <b>Eliminar = borrado lógico.</b> Un rubro eliminado queda con {@code activo = false}: no cuenta
 * en el total, pero el registro se conserva para el historial ({@link HistorialRubro}) y para no
 * arrastrar en cascada los {@code gastos_ejecutados} asociados (Criterio 2). El índice único parcial
 * {@code ux_rubros_presupuestales_nombre_activo} permite volver a crear un rubro con el mismo nombre
 * después de eliminarlo.
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

    /** Escala monetaria usada por las columnas {@code NUMERIC(p, 2)}. */
    public static final int ESCALA = 2;

    /** Identificador único del rubro. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rubro_id")
    private Long id;

    /** Evento al que pertenece el rubro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre del rubro (ej. "Transporte de conferencistas", "Refrigerios"). */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    /** Cantidad proyectada (unidades, días, personas...). */
    @Column(name = "cantidad", precision = 10, scale = 2, nullable = false)
    private BigDecimal cantidad;

    /** Valor unitario estimado en pesos. */
    @Column(name = "valor_unitario_proyectado", precision = 14, scale = 2, nullable = false)
    private BigDecimal valorUnitarioProyectado;

    /** {@code true} = rubro vigente; {@code false} = eliminado (se conserva para el historial). */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /**
     * Subtotal del rubro: {@code cantidad × valorUnitarioProyectado}, redondeado a 2 decimales.
     * No se persiste; se calcula siempre a partir de los valores guardados.
     *
     * @return Subtotal del rubro (0 si falta algún valor).
     */
    public BigDecimal getSubtotal() {
        return calcularSubtotal(cantidad, valorUnitarioProyectado);
    }

    /**
     * Retira el rubro del presupuesto vigente sin borrar el registro (Criterio 2).
     */
    public void desactivar() {
        this.activo = false;
    }

    /**
     * Calcula {@code cantidad × valorUnitario} con la escala monetaria del sistema.
     *
     * @param cantidad      Cantidad proyectada.
     * @param valorUnitario Valor unitario proyectado.
     * @return Subtotal redondeado a 2 decimales (0 si alguno es {@code null}).
     */
    public static BigDecimal calcularSubtotal(BigDecimal cantidad, BigDecimal valorUnitario) {
        if (cantidad == null || valorUnitario == null) {
            return BigDecimal.ZERO.setScale(ESCALA);
        }
        return cantidad.multiply(valorUnitario).setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
