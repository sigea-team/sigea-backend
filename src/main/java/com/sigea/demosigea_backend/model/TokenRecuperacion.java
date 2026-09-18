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
 * Entitas JPA yang merepresentasikan token verifikasi dan pemulihan akun di platform SIGEA.
 * <p>
 * Memetakan tabel {@code tokens_recuperacion} dan mengelola siklus hidup token sementara
 * yang digunakan untuk verifikasi email saat pendaftaran atau pemulihan kata sandi.
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
     * Identifikator unik untuk token di basis data (Primary Key Autoincrement).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    /**
     * Pengguna yang terhubung dengan token ini.
     * Menggunakan relasi {@link FetchType#LAZY} untuk optimasi performa query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /**
     * Kode unik token (UUID atau string acak) yang dikirimkan ke email pengguna.
     */
    @Column(name = "token", length = 255, nullable = false, unique = true)
    private String token;

    /**
     * Jenis token yang menentukan tujuan penggunaannya (misalnya: VERIFICACION_CORREO, RECUPERACION_PASSWORD).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 20, nullable = false)
    private TipoToken tipo;

    /**
     * Tanggal dan waktu saat token ini dibuat.
     * Secara default diisi dengan waktu sistem saat ini.
     */
    @Builder.Default
    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion = LocalDateTime.now();

    /**
     * Tanggal dan waktu batas kedaluwarsa token.
     */
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /**
     * Status penggunaan token. Value {@code true} menandakan token telah digunakan
     * dan tidak dapat dipakai kembali.
     */
    @Builder.Default
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;
}