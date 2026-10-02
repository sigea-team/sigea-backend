package com.sigea.demosigea_backend.model;

import com.sigea.demosigea_backend.exception.RegistroAuditoriaInmutableException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un registro del log de auditoría de operaciones críticas (HU-03, RF56).
 * <p>
 * Mapea la tabla {@code auditoria}. Cada registro guarda quién ejecutó la operación
 * ({@code usuario_id}), qué hizo ({@code accion}), sobre qué recurso ({@code entidad},
 * {@code entidad_id}), cuándo ({@code fecha_hora}) y los datos afectados ({@code detalle}, en JSON).
 * </p>
 * <p>
 * <b>Inmutabilidad (Criterio 3)</b> — se protege en tres capas:
 * <ol>
 *   <li>Java: la clase no expone setters y se marca con {@link Immutable}, por lo que Hibernate
 *       nunca emite {@code UPDATE}; los callbacks {@link PreUpdate}/{@link PreRemove} lanzan excepción.</li>
 *   <li>Repositorio: {@code AuditoriaRepository} no hereda de {@code JpaRepository}, así que no
 *       existen métodos {@code delete*} ni {@code saveAll} para modificarla.</li>
 *   <li>Base de datos: el trigger {@code trg_auditoria_inmutable} rechaza cualquier
 *       {@code UPDATE}, {@code DELETE} o {@code TRUNCATE} (changelog {@code 5-auditoria-hu03.sql}).</li>
 * </ol>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Immutable
@Table(name = "auditoria")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Auditoria {

    /** Identificador único del registro de auditoría. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auditoria_id")
    private Long id;

    /**
     * Usuario que ejecutó la operación. Puede ser {@code null} para operaciones del sistema
     * sin un usuario identificable (la columna admite nulos según el esquema canónico).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Código de la operación crítica ejecutada (ver {@link TipoOperacionAuditoria}). */
    @Column(name = "accion", length = 100, nullable = false)
    private String accion;

    /** Nombre de la tabla/recurso afectado (ej. {@code roles}, {@code usuarios}). */
    @Column(name = "entidad", length = 100, nullable = false)
    private String entidad;

    /** Identificador del registro afectado dentro de {@link #entidad}. */
    @Column(name = "entidad_id")
    private Long entidadId;

    /** Fecha y hora exactas en que se registró la operación. */
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    /** Datos afectados por la operación, serializados como JSON. */
    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;

    @PrePersist
    void antesDeInsertar() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
    }

    @PreUpdate
    void antesDeActualizar() {
        throw new RegistroAuditoriaInmutableException();
    }

    @PreRemove
    void antesDeEliminar() {
        throw new RegistroAuditoriaInmutableException();
    }
}
