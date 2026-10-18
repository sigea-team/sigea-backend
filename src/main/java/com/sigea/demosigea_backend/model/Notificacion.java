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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Notificación dirigida a un usuario, visible en la campana del frontend (HU-11, Criterio 2).
 * <p>
 * Mapea la tabla {@code notificaciones} (changeset Liquibase {@code 12-hu11-notificaciones.sql}).
 * {@code entidad}/{@code entidadId} indican el recurso relacionado (p. ej. {@code convocatorias}, 10) para que
 * el frontend pueda llevar al usuario a su detalle.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notificacion_id")
    private Long id;

    /** Usuario destinatario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 50, nullable = false)
    private TipoNotificacion tipo;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    /** Tabla/recurso relacionado (ej. {@code convocatorias}). */
    @Column(name = "entidad", length = 50)
    private String entidad;

    /** ID del recurso relacionado. */
    @Column(name = "entidad_id")
    private Long entidadId;

    @Builder.Default
    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_lectura")
    private LocalDateTime fechaLectura;
}
