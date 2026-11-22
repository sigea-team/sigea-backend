package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Memoria histórica de un evento cerrado formalmente (HU-28, RF55, CU-32). Tabla {@code memorias_historicas}.
 * <p>
 * Hay una por evento (índice único {@code memorias_historicas_evento_id_key}). {@code contenido} guarda en JSON la
 * información consolidada al cierre (configuración, comité, presupuesto, convocatorias y propuestas, conferencistas,
 * agenda, participantes, asistencia, encuestas e indicadores). Es inmutable: Hibernate no la actualiza
 * ({@link Immutable}) y la base de datos rechaza cualquier cambio (changeset hu28-004). Mientras exista, la
 * información del evento tampoco se modifica ni se elimina.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Immutable
@Table(name = "memorias_historicas")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoriaHistorica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memoria_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false, unique = true)
    private Evento evento;

    @Column(name = "fecha_consolidacion", nullable = false)
    private LocalDateTime fechaConsolidacion;

    /** Documento final de la memoria (RF53, exportación); hoy no se genera. */
    @Column(name = "archivo_url", length = 255)
    private String archivoUrl;

    /** Resumen o acta de cierre escrita por quien cierra el evento (opcional). */
    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    /** Información consolidada del evento al cierre, en JSON (changeset hu28-001). */
    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    /** Usuario que hizo el cierre formal; NULL si se eliminó el usuario. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrada_por")
    private Usuario cerradaPor;
}
