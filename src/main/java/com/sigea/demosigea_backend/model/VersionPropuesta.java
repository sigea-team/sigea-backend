package com.sigea.demosigea_backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Versión de una propuesta académica (HU-14, RF21).
 * <p>
 * Mapea la tabla {@code versiones_propuesta}. Cada vez que el autor envía o modifica la propuesta
 * se crea una fila nueva: las anteriores nunca se editan, de modo que quedan como evidencia del
 * historial de cambios (Criterio 3). Además de los documentos, cada versión guarda una copia de los
 * datos de la propuesta en ese momento y el usuario que hizo el cambio (Criterio 4).
 * </p>
 * <p>
 * {@code documentoUrl} conserva la ruta del documento principal de la versión (columna existente,
 * NOT NULL); la lista completa de archivos está en {@link #documentos}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "versiones_propuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VersionPropuesta {

    /** Identificador único de la versión. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long id;

    /** Propuesta a la que pertenece la versión. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propuesta_id", nullable = false, updatable = false)
    private Propuesta propuesta;

    /** Número consecutivo de la versión dentro de la propuesta (1, 2, 3...). */
    @Column(name = "numero_version", nullable = false, updatable = false)
    private Integer numeroVersion;

    /** Ruta de almacenamiento del documento principal de la versión. */
    @Column(name = "documento_url", length = 255, nullable = false)
    private String documentoUrl;

    /** Nota del autor que describe el cambio realizado en esta versión (opcional). */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    /** Fecha y hora en que se creó la versión. */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Usuario que creó la versión (autor del cambio). Puede ser nulo en registros históricos. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por", updatable = false)
    private Usuario creadoPor;

    /** Título de la propuesta en esta versión. */
    @Column(name = "titulo", length = 200)
    private String titulo;

    /** Resumen de la propuesta en esta versión. */
    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    /** Palabras clave de la propuesta en esta versión. */
    @Column(name = "palabras_clave", length = 255)
    private String palabrasClave;

    /** Línea temática de la propuesta en esta versión (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linea_tematica_id")
    private LineaTematica lineaTematica;

    /** Indica si es la versión definitiva aprobada (RF29). Solo una por propuesta. */
    @Builder.Default
    @Column(name = "es_definitiva", nullable = false)
    private Boolean esDefinitiva = false;

    /** Documentos adjuntos a la versión. */
    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("principal DESC, id ASC")
    @Builder.Default
    private List<DocumentoPropuesta> documentos = new ArrayList<>();

    @PrePersist
    void antesDeInsertar() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
        if (esDefinitiva == null) {
            esDefinitiva = false;
        }
    }

    /**
     * Agrega un documento a la versión manteniendo la relación bidireccional.
     *
     * @param documento Documento a agregar.
     */
    public void agregarDocumento(DocumentoPropuesta documento) {
        if (this.documentos == null) {
            this.documentos = new ArrayList<>();
        }
        this.documentos.add(documento);
        documento.setVersion(this);
    }
}
