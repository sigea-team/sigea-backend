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

import java.time.LocalDateTime;

/**
 * Invitación enviada por correo a un conferencista nuevo (HU-12).
 * <p>
 * Mapea la tabla {@code invitaciones_conferencista}. El administrador solo indica nombre y correo; el
 * invitado abre el enlace con el {@code token} y registra él mismo su documento, teléfono y
 * trayectoria. Al completarla se crea (o completa) la {@link Persona}, su perfil y la participación en
 * el evento, y se guarda aquí la persona resultante.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "invitaciones_conferencista")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitacionConferencista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invitacion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    /** Nombre con el que el administrador invitó a la persona. */
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    /** Correo al que se envió el enlace; será el correo de la persona registrada. */
    @Column(name = "correo", length = 150, nullable = false)
    private String correo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participacion", length = 20, nullable = false)
    private TipoParticipacionConferencista tipoParticipacion = TipoParticipacionConferencista.conferencista;

    /** Token aleatorio del enlace; se reemplaza al reenviar la invitación. */
    @Column(name = "token", length = 64, nullable = false, unique = true)
    private String token;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoInvitacion estado = EstadoInvitacion.pendiente;

    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "fecha_completada")
    private LocalDateTime fechaCompletada;

    /** Persona creada o completada al aceptar la invitación. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    /** Usuario administrador que envió la invitación (puede ser nulo). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitado_por")
    private Usuario invitadoPor;

    /** {@code true} si está pendiente y todavía no ha vencido. */
    public boolean estaVigente(LocalDateTime ahora) {
        return estado == EstadoInvitacion.pendiente && fechaExpiracion != null && fechaExpiracion.isAfter(ahora);
    }

    /** {@code true} si está pendiente pero su enlace ya venció. */
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoInvitacion.pendiente && (fechaExpiracion == null || !fechaExpiracion.isAfter(ahora));
    }

    /** Asigna un enlace nuevo con su vigencia (al crear o al reenviar). */
    public void renovar(String nuevoToken, LocalDateTime ahora, long horasVigencia) {
        this.token = nuevoToken;
        this.fechaEnvio = ahora;
        this.fechaExpiracion = ahora.plusHours(horasVigencia);
    }

    /** Marca la invitación como completada por la persona indicada. */
    public void completar(Persona personaRegistrada, LocalDateTime ahora) {
        this.estado = EstadoInvitacion.completada;
        this.persona = personaRegistrada;
        this.fechaCompletada = ahora;
    }
}
