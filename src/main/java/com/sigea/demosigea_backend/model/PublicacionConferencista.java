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
 * Publicación que forma parte de la trayectoria de un conferencista (HU-12, Criterio 1).
 * Mapea la tabla {@code publicaciones_conferencista}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "publicaciones_conferencista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionConferencista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publicacion_id")
    private Long id;

    /** Perfil (trayectoria) al que pertenece la publicación. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private PerfilConferencista perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoPublicacion tipo;

    @Column(name = "titulo", length = 300, nullable = false)
    private String titulo;

    /** Revista, editorial, libro o evento donde se publicó (opcional). */
    @Column(name = "medio", length = 200)
    private String medio;

    /** Año de publicación (opcional). */
    @Column(name = "anio")
    private Integer anio;

    /** Enlace a la publicación: DOI, URL, repositorio (opcional). */
    @Column(name = "enlace", length = 300)
    private String enlace;

    /** Posición en la que se muestra dentro de la trayectoria (1 = primero). */
    @Builder.Default
    @Column(name = "orden", nullable = false)
    private Integer orden = 1;
}
