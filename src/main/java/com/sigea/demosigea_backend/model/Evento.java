package com.sigea.demosigea_backend.model;

import java.time.LocalDate;

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

/**
 * Entidad JPA que representa un evento académico, científico o tecnológico (RF03)
 * y, a la vez, cada una de sus ediciones (RF04).
 * <p>
 * Mapea la tabla {@code eventos}. Una edición es un registro independiente de la misma tabla
 * cuyo {@code evento_base_id} apunta al evento raíz de la familia; el evento raíz tiene
 * {@code evento_base_id = NULL}. De esta forma cada edición conserva configuración y datos
 * propios, manteniendo la relación histórica con el evento base.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    /** Identificador único del evento (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evento_id")
    private Long id;

    /** Nombre del evento. */
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    /** Objetivo general del evento. */
    @Column(name = "objetivo", columnDefinition = "TEXT")
    private String objetivo;

    /** Descripción del evento. */
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** Tipo de evento (congreso, seminario, feria, encuentro de semilleros, etc.). Texto libre para garantizar RNF15. */
    @Column(name = "tipo", length = 50)
    private String tipo;

    /** Modalidad del evento. */
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 20)
    private ModalidadEvento modalidad;

    /** Fecha de inicio del evento. */
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    /** Fecha de finalización del evento (debe ser mayor o igual a la de inicio). */
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    /** Semestre académico al que pertenece el evento (ej. "2026-2"). */
    @Column(name = "semestre", length = 10)
    private String semestre;

    /** Estado del ciclo de vida del evento. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoEvento estado = EstadoEvento.en_configuracion;

    /**
     * Evento raíz del cual esta edición forma parte. Es {@code null} cuando el registro es el evento base.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "evento_base_id")
    private Evento eventoBase;

    /**
     * Indica si el registro es una edición derivada de otro evento.
     *
     * @return {@code true} si tiene evento base asociado.
     */
    public boolean esEdicion() {
        return eventoBase != null;
    }
}
