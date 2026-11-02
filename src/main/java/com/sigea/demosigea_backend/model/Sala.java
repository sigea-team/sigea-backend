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

/**
 * Entidad JPA que representa una sala (física o virtual) disponible para programar
 * actividades dentro de un evento — HU-Salas, Criterio 1.
 * <p>
 * Mapea la tabla {@code salas}. El catálogo es propio de cada evento; dos eventos pueden
 * tener salas con el mismo nombre sin conflicto. El índice único garantiza unicidad
 * de nombre dentro del mismo evento (sin distinguir mayúsculas).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "salas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sala {

    /** Identificador único de la sala. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sala_id")
    private Long id;

    /** Evento al que pertenece la sala. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre de la sala (único por evento, sin distinguir mayúsculas). */
    @Column(name = "nombre", length = 100, nullable = false)
    private String nombre;

    /** Ubicación física o URL de la sala virtual (opcional). */
    @Column(name = "ubicacion", length = 150)
    private String ubicacion;

    /** Tipo de sala: fisica o virtual (valores del CHECK de la tabla). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoSala tipo;

    /** Aforo máximo de la sala (debe ser mayor que 0). */
    @Column(name = "aforo_maximo", nullable = false)
    private Integer aforoMaximo;
}
