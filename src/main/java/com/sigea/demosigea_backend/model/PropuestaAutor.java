package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa la asociación entre una propuesta académica y una persona
 * en calidad de autor o ponente (HU-13).
 * <p>
 * Mapea la tabla {@code propuestas_autores} con clave primaria compuesta (propuesta_id, persona_id).
 * Almacena el orden de autoría y el rol específico diferenciado (autor principal, coautor, ponente)
 * para identificar con precisión la participación en el evento.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "propuestas_autores")
@IdClass(PropuestaAutorId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropuestaAutor {

    /** Propuesta académica a la que se vincula el autor. */
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "propuesta_id", nullable = false)
    private Propuesta propuesta;

    /** Persona natural registrada como autor o ponente. */
    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    /** Orden de aparición o prelación de autoría (1 para autor principal). */
    @Builder.Default
    @Column(name = "orden", nullable = false)
    private Integer orden = 1;

    /**
     * Rol específico del autor en la propuesta (autor_principal, coautor, ponente).
     * Permite guardar diferenciadamente las participaciones según el Criterio 2.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 20, nullable = false)
    private RolAutor rol = RolAutor.autor_principal;
}
