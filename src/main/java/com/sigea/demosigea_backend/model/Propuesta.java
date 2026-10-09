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

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una propuesta académica postulada a una convocatoria.
 * <p>
 * Mapea la tabla {@code propuestas} tal como existe en la base de datos. Se creó en la HU-05
 * únicamente para consultar si una línea temática está en uso (Criterio 2); <b>la lógica de
 * negocio de las propuestas (envío, versiones, evaluación) pertenece a HU-14 y siguientes</b>.
 * </p>
 * <p>
 * {@code convocatoria_id} se mapea como columna escalar porque la entidad {@code Convocatoria}
 * aún no existe. Cuando se implemente (HU-10), puede reemplazarse por
 * {@code @ManyToOne Convocatoria convocatoria} con el mismo {@code @JoinColumn}, sin tocar la tabla.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "propuestas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Propuesta {

    /** Identificador único de la propuesta. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "propuesta_id")
    private Long id;

    /** ID de la convocatoria a la que se postula (FK a {@code convocatorias}). */
    @Column(name = "convocatoria_id", nullable = false)
    private Long convocatoriaId;

    /** Título de la propuesta. */
    @Column(name = "titulo", length = 200, nullable = false)
    private String titulo;

    /** Resumen de la propuesta. */
    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    /** Palabras clave separadas por comas. */
    @Column(name = "palabras_clave", length = 255)
    private String palabrasClave;

    /** Línea temática en la que se enmarca la propuesta (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_tematica_id")
    private LineaTematica lineaTematica;

    /** Estado del proceso de la propuesta. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoPropuesta estado = EstadoPropuesta.recibida;

    /** Fecha y hora de envío. */
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    void antesDeInsertar() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
    }
}
