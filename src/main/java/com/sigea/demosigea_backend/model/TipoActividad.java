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

/**
 * Entidad JPA que representa un tipo de actividad configurable dentro de un evento
 * (conferencia, taller, panel, ponencia, póster, etc.) — HU-05, RF06.
 * <p>
 * Mapea la tabla {@code tipos_actividad}. El catálogo es <b>propio de cada evento</b> (cada edición
 * tiene el suyo), lo que permite que el sistema sirva para cualquier tipo de evento sin modificar
 * el software (RNF15). Es referenciado por {@code actividades.tipo_actividad_id} (HU de agenda).
 * </p>
 * <p>
 * La relación con {@link Evento} se maneja solo desde este lado ({@code @ManyToOne}); la entidad
 * {@code Evento} no se modifica para no chocar con otras HU en desarrollo.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "tipos_actividad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoActividad {

    /** Identificador único del tipo de actividad. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_actividad_id")
    private Long id;

    /** Evento al que pertenece el catálogo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre del tipo de actividad (único por evento, sin distinguir mayúsculas). */
    @Column(name = "nombre", length = 80, nullable = false)
    private String nombre;

    /** Características o descripción del tipo de actividad. */
    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
