package com.sigea.demosigea_backend.model;

import java.math.BigDecimal;
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
import org.hibernate.annotations.Formula;

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

    /**
     * Tipo de evento, elegido del catálogo {@code tipos_evento} (congreso, seminario, taller, etc.).
     * Reemplaza la antigua columna de texto libre {@code tipo} (changeset tipos-evento-003/004).
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_evento_id")
    private TipoEvento tipoEvento;

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

    /** HU-26 (RN-01): porcentaje mínimo de asistencia por defecto para el certificado de asistente. */
    public static final BigDecimal PORCENTAJE_MINIMO_ASISTENCIA_POR_DEFECTO = new BigDecimal("80.00");

    /**
     * HU-26 (RN-01, changeset hu26-001): porcentaje mínimo de las actividades inscritas (no canceladas ni por
     * programar) a las que el participante debe asistir para obtener el certificado de asistente. Entre 0 (excluido)
     * y 100.
     */
    @Builder.Default
    @Column(name = "porcentaje_minimo_asistencia", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeMinimoAsistencia = PORCENTAJE_MINIMO_ASISTENCIA_POR_DEFECTO;

    /**
     * Indica si el registro es una edición derivada de otro evento.
     *
     * @return {@code true} si tiene evento base asociado.
     */
    /**
     * HU-28: cantidad de memorias históricas del evento (0 o 1, índice único). Solo lectura: lo calcula la base de
     * datos al cargar el evento. Con la memoria consolidada la información del evento ya no se modifica.
     */
    @Formula("(SELECT COUNT(m.memoria_id) FROM memorias_historicas m WHERE m.evento_id = evento_id)")
    @Setter(lombok.AccessLevel.NONE)
    private Long memoriasHistoricas;

    /**
     * HU-28: indica si el evento fue cerrado formalmente y su información está en la memoria histórica (solo consulta).
     *
     * @return {@code true} si tiene memoria histórica consolidada.
     */
    public boolean isArchivado() {
        return memoriasHistoricas != null && memoriasHistoricas > 0;
    }

    public boolean esEdicion() {
        return eventoBase != null;
    }
}
