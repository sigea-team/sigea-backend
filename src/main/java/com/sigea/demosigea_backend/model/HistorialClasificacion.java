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
 * Registro inmutable de una clasificación aplicada a una propuesta (HU-18, Criterio 2).
 * <p>
 * Mapea la tabla {@code historial_clasificaciones} (changeset Liquibase {@code 20-hu18-clasificacion-propuestas.sql}).
 * La base de datos impide modificar un registro mediante un trigger; solo se inserta. En un ajuste manual el
 * motivo es obligatorio (restricción CHECK).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "historial_clasificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialClasificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_clasificacion_id")
    private Long id;

    /** Propuesta clasificada. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propuesta_id", nullable = false, updatable = false)
    private Propuesta propuesta;

    /** Automática (umbrales) o manual (administrador). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false, updatable = false)
    private TipoClasificacion tipo;

    /** Clasificación que tenía la propuesta antes del movimiento ({@code null} si es la primera). */
    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_anterior", length = 30, updatable = false)
    private ClasificacionResultado clasificacionAnterior;

    /** Clasificación que queda aplicada. */
    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_nueva", length = 30, nullable = false, updatable = false)
    private ClasificacionResultado clasificacionNueva;

    /** Puntuación ponderada de la propuesta en el momento del movimiento. */
    @Column(name = "puntuacion_ponderada", precision = 5, scale = 2, nullable = false, updatable = false)
    private BigDecimal puntuacionPonderada;

    /** Umbral de aprobación vigente en el momento del movimiento. */
    @Column(name = "umbral_aceptacion", precision = 4, scale = 2, nullable = false, updatable = false)
    private BigDecimal umbralAceptacion;

    /** Umbral de aprobación con ajustes vigente en el momento del movimiento. */
    @Column(name = "umbral_ajustes", precision = 4, scale = 2, nullable = false, updatable = false)
    private BigDecimal umbralAjustes;

    /** Motivo del ajuste (obligatorio en un ajuste manual). */
    @Column(name = "motivo", columnDefinition = "TEXT", updatable = false)
    private String motivo;

    /** Usuario que aplicó o ajustó la clasificación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    void antesDeInsertar() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }
}
