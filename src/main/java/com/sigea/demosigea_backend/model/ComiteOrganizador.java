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
 * Entidad JPA que representa la participación de una persona como responsable o miembro del
 * comité organizador de un evento (HU-06, RF05).
 * <p>
 * Mapea la tabla {@code comite_organizador}. Cada registro es <b>una participación</b>: cuando un
 * miembro se retira no se borra, sino que se marca {@code activo = false} y se guarda
 * {@code fecha_retiro} (Criterio 3). Si la misma persona vuelve a vincularse se crea un registro
 * nuevo, de modo que el historial muestra cada periodo de participación.
 * </p>
 * <p>
 * La relación con {@link Evento} y {@link Persona} se maneja solo desde este lado
 * ({@code @ManyToOne}); la entidad {@code Evento} no se modifica, para no chocar con las HU que
 * se desarrollan en paralelo (HU-05, HU-10).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "comite_organizador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComiteOrganizador {

    /** Identificador único de la participación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comite_id")
    private Long id;

    /** Evento al que pertenece el comité. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Persona que integra el comité (no requiere tener cuenta de usuario). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    /** Rol de la persona dentro de la organización del evento (ej. "Coordinador general", "Logística"). */
    @Column(name = "rol_comite", length = 50, nullable = false)
    private String rolComite;

    /** Fecha y hora en que la persona fue vinculada al comité. */
    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    /** Indica si la participación está vigente. {@code false} = retirado (se conserva como historial). */
    @Builder.Default
    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    /** Fecha y hora del retiro del comité vigente; {@code null} mientras la participación esté activa. */
    @Column(name = "fecha_retiro")
    private LocalDateTime fechaRetiro;

    @PrePersist
    void antesDeInsertar() {
        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }
    }

    /**
     * Retira al miembro de la lista vigente conservando el registro (Criterio 3).
     */
    public void retirar() {
        this.activo = false;
        this.fechaRetiro = LocalDateTime.now();
    }
}
