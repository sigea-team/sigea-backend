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
 * Entidad JPA que representa los tokens de verificación y recuperación de cuenta en la plataforma SIGEA.
 * <p>
 * Mapea la tabla {@code tokens_recuperacion} y gestiona el ciclo de vida de los tokens temporales
 * utilizados para la verificación de correo electrónico durante el registro o la recuperación de contraseña.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "tokens_recuperacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRecuperacion {

    /**
     * Identificador único para el token en la base de datos (Clave Primaria Autoincrementable).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    /**
     * Usuario asociado a este token.
     * Utiliza una relación de carga perezosa {@link FetchType#LAZY} para la optimización del rendimiento de las consultas.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Código único del token (UUID o cadena aleatoria) enviado al correo electrónico del usuario.
     */
    @Column(name = "token", length = 255, nullable = false, unique = true)
    private String token;

    /**
     * Tipo de token que determina su propósito de uso (por ejemplo: VERIFICACION_CORREO, RECUPERACION_PASSWORD).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoToken tipo;

    /**
     * Fecha y hora en la que se generó este token.
     * Por defecto se inicializa con la fecha y hora actual del sistema.
     */
    @Builder.Default
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    /**
     * Fecha y hora límite para la expiración del token.
     */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Estado de uso del token. El valor {@code true} indica que el token ya ha sido utilizado
     * y no se puede volver a emplear.
     */
    @Builder.Default
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;
}