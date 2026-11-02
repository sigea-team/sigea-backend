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
 * Documento adjunto a una versión de propuesta (HU-14, RF20).
 * <p>
 * Mapea la tabla {@code documentos_propuesta} creada por el changeset {@code 16-hu14-versiones-propuesta.sql}.
 * El archivo físico vive en el almacenamiento configurado; aquí solo se guarda su ruta y sus metadatos.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "documentos_propuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoPropuesta {

    /** Identificador único del documento. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_id")
    private Long id;

    /** Versión de la propuesta a la que pertenece el documento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, updatable = false)
    private VersionPropuesta version;

    /** Nombre del archivo tal como lo subió el autor. */
    @Column(name = "nombre_original", length = 255, nullable = false)
    private String nombreOriginal;

    /** Ruta relativa del archivo dentro del almacenamiento. */
    @Column(name = "ruta_almacenamiento", length = 255, nullable = false)
    private String rutaAlmacenamiento;

    /** Tipo MIME del archivo (ej. {@code application/pdf}). */
    @Column(name = "tipo_contenido", length = 100)
    private String tipoContenido;

    /** Tamaño del archivo en bytes. */
    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    /** Indica si es el documento principal de la versión. */
    @Builder.Default
    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    /** Fecha y hora de carga del archivo. */
    @Column(name = "fecha_carga", nullable = false, updatable = false)
    private LocalDateTime fechaCarga;

    @PrePersist
    void antesDeInsertar() {
        if (fechaCarga == null) {
            fechaCarga = LocalDateTime.now();
        }
        if (principal == null) {
            principal = false;
        }
    }
}
