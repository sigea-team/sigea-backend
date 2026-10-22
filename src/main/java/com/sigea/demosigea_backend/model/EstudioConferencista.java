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
 * Estudio (formación académica) que forma parte de la trayectoria de un conferencista
 * (HU-12, Criterio 1). Mapea la tabla {@code estudios_conferencista}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "estudios_conferencista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudioConferencista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estudio_id")
    private Long id;

    /** Perfil (trayectoria) al que pertenece el estudio. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilConferencista perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", length = 20, nullable = false)
    private NivelEstudio nivel;

    /** Título obtenido (ej. "Doctor en Ingeniería"). */
    @Column(name = "titulo", length = 200, nullable = false)
    private String titulo;

    /** Institución que otorgó el título. */
    @Column(name = "institucion", length = 150, nullable = false)
    private String institucion;

    /** Año de grado (opcional). */
    @Column(name = "anio")
    private Integer anio;

    /** Posición en la que se muestra dentro de la trayectoria (1 = primero). */
    @Builder.Default
    @Column(name = "orden", nullable = false)
    private Integer orden = 1;
}
