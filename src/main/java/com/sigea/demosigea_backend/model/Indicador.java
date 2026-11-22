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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Indicador de gestión de un evento (tabla {@code indicadores}). En HU-28 se calculan y guardan al cierre formal del
 * evento (asistencia, propuestas, encuestas, presupuesto) y quedan como parte de su memoria histórica.
 * {@code codigo} (changeset hu28-002) identifica el indicador sin depender del nombre visible.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Immutable
@Table(name = "indicadores")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Indicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicador_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "codigo", length = 50)
    private String codigo;

    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    @Column(name = "valor", precision = 14, scale = 2, nullable = false)
    private BigDecimal valor;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Column(name = "fecha_calculo", nullable = false)
    private LocalDateTime fechaCalculo;
}
