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

/**
 * Entidad JPA que representa a un miembro del comité evaluador de una convocatoria (HU-17).
 * <p>
 * Mapea la tabla {@code comites_evaluadores}. Un evaluador está vinculado a una convocatoria
 * y puede tener asignadas propuestas para revisar y calificar.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "comites_evaluadores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComiteEvaluador {

    /** Identificador único del miembro evaluador (PK autoincremental). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comite_evaluador_id")
    private Long id;

    /** Convocatoria en la que participa el evaluador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "convocatoria_id", nullable = false)
    private Convocatoria convocatoria;

    /** Persona natural que actúa como evaluador. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    /** Área de experticia del evaluador (opcional). */
    @Column(name = "area_experticia", length = 150)
    private String areaExperticia;
}
