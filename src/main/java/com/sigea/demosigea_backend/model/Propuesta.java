package com.sigea.demosigea_backend.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa una propuesta académica postulada a una convocatoria.
 * <p>
 * Mapea la tabla {@code propuestas} tal como existe en la base de datos.
 * Vincula los autores y ponentes que postulan el trabajo (HU-13) y la convocatoria correspondiente.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.1
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

    /** Convocatoria a la que se postula la propuesta (FK a {@code convocatorias}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

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

    /** Autores y ponentes asociados a la propuesta (HU-13). */
    @OneToMany(mappedBy = "propuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<PropuestaAutor> autores = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
    }

    /**
     * ID de la convocatoria para compatibilidad con código existente.
     */
    public Long getConvocatoriaId() {
        return convocatoria != null ? convocatoria.getId() : null;
    }

    /**
     * Agrega un autor a la propuesta manteniendo la consistencia bidireccional.
     *
     * @param autor Relación autor-propuesta a agregar.
     */
    public void agregarAutor(PropuestaAutor autor) {
        if (this.autores == null) {
            this.autores = new ArrayList<>();
        }
        this.autores.add(autor);
        autor.setPropuesta(this);
    }
}

