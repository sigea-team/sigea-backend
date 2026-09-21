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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad JPA que representa a los usuarios autenticables dentro del sistema SIGEA.
 * <p>
 * Mapea la tabla {@code usuarios} y vincula la información de acceso (credenciales,
 * estado de la cuenta y roles) con los datos personales representados por la entidad {@link Persona}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    /**
     * Identificador único del usuario en la base de datos (Clave Primaria Autoincremental).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario_id")
    private Long id;

    /**
     * Información personal asociada a la cuenta de usuario.
     * Relación uno a uno obligatoria con la entidad {@link Persona}.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", referencedColumnName = "persona_id", nullable = false, unique = true)
    private Persona persona;

    /**
     * Contraseña del usuario cifrada mediante un algoritmo de hashing seguro (ej. BCrypt).
     */
    @Column(name = "contrasena_hash", length = 255, nullable = false)
    private String contrasenaHash;

    /**
     * Bandera que indica si el usuario ha confirmado su dirección de correo electrónico.
     */
    @Builder.Default
    @Column(name = "correo_verificado", nullable = false)
    private Boolean correoVerificado = false;

    /**
     * Estado operativo de la cuenta de usuario (ej. activo, inactivo, bloqueado).
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoUsuario estado = EstadoUsuario.activo;

    /**
     * Fecha y hora exactas en las que se creó la cuenta en la plataforma.
     */
    @Builder.Default
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /**
     * Conjunto de roles asignados al usuario que determinan sus permisos dentro de SIGEA.
     * Carga de tipo {@link FetchType#EAGER} para disponer de las autoridades durante la autenticación.
     */
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();
}