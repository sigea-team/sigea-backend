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

import java.time.LocalDateTime;

/**
 * Persona que firma los certificados de un evento (HU-26, changeset hu26-007). No tiene que ser usuario del sistema
 * (p. ej. el decano). Mapea la tabla {@code firmantes_certificado}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "firmantes_certificado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirmanteCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "firmante_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "cargo", length = 150, nullable = false)
    private String cargo;

    /** Ruta de la imagen de la firma (PNG normalizado) en el almacenamiento de archivos. */
    @Column(name = "firma_url", length = 255, nullable = false)
    private String firmaUrl;

    /** Posición en el pie, de izquierda a derecha (desde 1). */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
