package com.sigea.demosigea_backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Perfil del conferencista: su trayectoria académica y profesional (HU-12, RF16 y RF17).
 * <p>
 * Mapea la tabla {@code perfiles_conferencista}. Hay <b>un perfil por persona</b>: los datos
 * personales y de contacto siguen en {@link Persona} y la trayectoria (biografía, estudios y
 * publicaciones) vive aquí. Como todos los eventos en los que participa la persona leen este
 * mismo registro, al actualizar la trayectoria el cambio se refleja de inmediato en la vista
 * pública de cada evento (Criterio 4).
 * </p>
 * <p>
 * Los estudios y publicaciones se reemplazan completos al actualizar la trayectoria
 * ({@code orphanRemoval}), en el orden en que los envía el administrador.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "perfiles_conferencista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilConferencista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "perfil_id")
    private Long id;

    /** Persona dueña del perfil (datos personales y de contacto). */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    /** Reseña biográfica que se publica en la agenda y el material de difusión. */
    @Column(name = "biografia", columnDefinition = "TEXT", nullable = false)
    private String biografia;

    /** Cargo actual (ej. "Profesor titular", "Director de investigación"). */
    @Column(name = "cargo_actual", length = 150)
    private String cargoActual;

    /** Institución o empresa donde trabaja actualmente. */
    @Column(name = "institucion", length = 150)
    private String institucion;

    /** Enlace a su perfil académico o profesional (CvLAC, ORCID, Google Scholar, LinkedIn). */
    @Column(name = "enlace_perfil", length = 300)
    private String enlacePerfil;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Última modificación de la trayectoria; se muestra en la vista pública (Criterio 4). */
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Builder.Default
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = 50)
    private List<EstudioConferencista> estudios = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    @BatchSize(size = 50)
    private List<PublicacionConferencista> publicaciones = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    /**
     * Reemplaza los estudios del perfil conservando el orden recibido.
     *
     * @param nuevos Estudios nuevos (sin perfil asignado).
     */
    public void reemplazarEstudios(List<EstudioConferencista> nuevos) {
        estudios.clear();
        int orden = 1;
        for (EstudioConferencista estudio : nuevos) {
            estudio.setPerfil(this);
            estudio.setOrden(orden++);
            estudios.add(estudio);
        }
    }

    /**
     * Reemplaza las publicaciones del perfil conservando el orden recibido.
     *
     * @param nuevas Publicaciones nuevas (sin perfil asignado).
     */
    public void reemplazarPublicaciones(List<PublicacionConferencista> nuevas) {
        publicaciones.clear();
        int orden = 1;
        for (PublicacionConferencista publicacion : nuevas) {
            publicacion.setPerfil(this);
            publicacion.setOrden(orden++);
            publicaciones.add(publicacion);
        }
    }
}
