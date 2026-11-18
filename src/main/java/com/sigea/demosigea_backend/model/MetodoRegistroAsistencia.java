package com.sigea.demosigea_backend.model;

/**
 * Método con el que se registró una asistencia (HU-24). Corresponde al CHECK
 * {@code asistencias_metodo_registro_check} de la tabla {@code asistencias}.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum MetodoRegistroAsistencia {
    /** Lectura del código QR de la inscripción. */
    qr,
    /** Registro manual del organizador (por ejemplo, el participante no tiene su QR a mano). */
    manual
}
