package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * Observación o ajuste requerido sobre una versión de una propuesta (HU-19, RF28).
 * <p>
 * Mapea la tabla {@code observaciones_propuesta} (completada en el changeset 23):
 * <ul>
 *   <li>{@code version}: versión de la propuesta sobre la que se hizo la observación (la que la originó).</li>
 *   <li>{@code requiereAjuste}: si es {@code true}, es un ajuste requerido y bloquea la aprobación de la versión
 *       definitiva hasta que se marque como resuelto (Criterio 4). Si es {@code false}, es un comentario.</li>
 *   <li>{@code versionAtencion}: versión que subió el autor para atenderla (Criterio 2). Si el autor sube otra
 *       versión antes de que se resuelva, el vínculo pasa a la más reciente.</li>
 * </ul>
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "observaciones_propuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservacionPropuesta {

    /** Identificador único de la observación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observacion_id")
    private Long id;

    /** Versión observada (la que originó la observación). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private VersionPropuesta version;

    /** Miembro del comité evaluador que la registró; {@code null} si la registró el administrador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comite_evaluador_id", updatable = false)
    private ComiteEvaluador comiteEvaluador;

    /** Usuario que registró la observación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    /** Texto de la observación o del ajuste requerido. */
    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    /** {@code true} si es un ajuste requerido que debe resolverse antes de aprobar la versión definitiva. */
    @Builder.Default
    @Column(name = "requiere_ajuste", nullable = false)
    private Boolean requiereAjuste = true;

    /** Fecha y hora de registro. */
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    /** Versión subida por el autor para atender la observación (Criterio 2). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_atencion_id")
    private VersionPropuesta versionAtencion;

    /** Indica si la observación se dio por resuelta. */
    @Builder.Default
    @Column(name = "resuelto", nullable = false)
    private Boolean resuelto = false;

    /** Fecha y hora en que se dio por resuelta. */
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    /** Usuario que la dio por resuelta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resuelto_por")
    private Usuario resueltoPor;

    @PrePersist
    void antesDeInsertar() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
        if (requiereAjuste == null) {
            requiereAjuste = true;
        }
        if (resuelto == null) {
            resuelto = false;
        }
    }

    /** Un ajuste requerido que todavía no se ha resuelto (bloquea la aprobación, Criterio 4). */
    public boolean esAjustePendiente() {
        return Boolean.TRUE.equals(requiereAjuste) && !Boolean.TRUE.equals(resuelto);
    }
}
