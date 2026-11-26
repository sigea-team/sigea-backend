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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Certificado de participación emitido a una persona en un evento (HU-26, RF47). Mapea la tabla {@code certificados}.
 * <p>
 * Criterio 3: hay uno solo por persona, evento y tipo ({@code ux_certificados_persona_evento_tipo}, con
 * {@code actividad_id} nulo). Los datos impresos (nombre, documento, evento, fechas y detalle) se guardan al emitir,
 * así que el certificado se entrega igual aunque después cambien la persona o el evento; el PDF guardado en
 * {@code archivo_url} se puede reconstruir con ellos si se pierde del almacenamiento.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "certificados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificado_id")
    private Long id;

    /** Persona certificada. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    /** Evento del certificado. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Actividad, si el certificado es de una sola actividad. HU-26 emite certificados de todo el evento (null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Rol certificado. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_certificado", length = 20, nullable = false)
    private TipoCertificado tipo;

    /** Porcentaje de asistencia con el que se emitió (solo asistente). */
    @Column(name = "porcentaje_asistencia", precision = 5, scale = 2)
    private BigDecimal porcentajeAsistencia;

    /** Fecha y hora de emisión; también es la fecha impresa en el PDF. */
    @Column(name = "fecha_generacion", nullable = false, updatable = false)
    private LocalDateTime fechaGeneracion;

    /** Ruta del PDF en el almacenamiento de archivos ({@link com.sigea.demosigea_backend.service.AlmacenamientoArchivos}). */
    @Column(name = "archivo_url", length = 255, nullable = false)
    private String archivoUrl;

    /** Código para comprobar la autenticidad del certificado en la consulta pública. */
    @Column(name = "codigo_verificacion", length = 40, nullable = false, unique = true, updatable = false)
    private String codigoVerificacion;

    /** Nombre completo de la persona al emitir. */
    @Column(name = "nombre_participante", length = 201)
    private String nombreParticipante;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    /** Nombre del evento al emitir. */
    @Column(name = "nombre_evento", length = 200)
    private String nombreEvento;

    @Column(name = "fecha_inicio_evento")
    private LocalDate fechaInicioEvento;

    @Column(name = "fecha_fin_evento")
    private LocalDate fechaFinEvento;

    /** Detalle impreso: actividades asistidas o actividades a cargo del ponente o conferencista. */
    @Column(name = "detalle", columnDefinition = "TEXT")
    private String detalle;
}
