package com.sigea.demosigea_backend.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de operaciones críticas que el sistema audita (HU-03, RF56).
 * <p>
 * El nombre de cada constante es el valor que se guarda en la columna {@code auditoria.accion}
 * y el que el administrador usa para filtrar el log por tipo de operación (Criterio 2).
 * </p>
 * <p>
 * <b>Para agregar una nueva operación crítica</b> (ej. al implementar HU-08 o HU-11):
 * 1) agregue aquí la constante, 2) llame a {@code auditoriaService.registrar(...)} al final
 * del método {@code @Transactional} del servicio correspondiente, después de guardar los cambios.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Getter
@RequiredArgsConstructor
public enum TipoOperacionAuditoria {

    // ----- Administración y seguridad (implementadas en HU-01, HU-02, HU-31, HU-32) -----
    ROL_CREADO("ROLES", "Creación de un rol con sus permisos"),
    ROL_ACTUALIZADO("ROLES", "Modificación de un rol o de sus permisos"),
    ROL_ELIMINADO("ROLES", "Eliminación de un rol"),
    USUARIO_REGISTRADO("USUARIOS", "Registro de una nueva cuenta de usuario"),
    CONTRASENA_RESTABLECIDA("USUARIOS", "Restablecimiento de contraseña mediante token de recuperación"),

    // ----- Planeación y configuración de eventos (HU-05, HU-06) -----
    MIEMBRO_COMITE_AGREGADO("EVENTOS", "Vinculación de una persona al comité organizador de un evento"),
    MIEMBRO_COMITE_RETIRADO("EVENTOS", "Retiro de un miembro del comité organizador vigente de un evento"),
    LINEA_TEMATICA_CREADA("EVENTOS", "Creación de una línea temática en un evento"),
    LINEA_TEMATICA_ACTUALIZADA("EVENTOS", "Modificación de datos de una línea temática"),
    LINEA_TEMATICA_ELIMINADA("EVENTOS", "Eliminación de una línea temática de un evento"),

    // ----- Convocatorias y propuestas -----
    CONVOCATORIA_CREADA("CONVOCATORIAS", "Creación de una convocatoria"),
    CONVOCATORIA_ACTUALIZADA("CONVOCATORIAS", "Modificación de datos de una convocatoria"),
    CONVOCATORIA_ELIMINADA("CONVOCATORIAS", "Eliminación de una convocatoria"),

    // ----- Reservadas: se registran cuando exista la historia correspondiente -----
    PRESUPUESTO_APROBADO("PRESUPUESTO", "Registro del presupuesto aprobado del evento (HU-08)"),
    CONVOCATORIA_PUBLICADA("CONVOCATORIAS", "Publicación de una convocatoria (HU-11)"),
    RESULTADOS_PUBLICADOS("EVALUACION", "Publicación de resultados de evaluación de propuestas (HU-18)");

    /** Módulo funcional al que pertenece la operación. */
    private final String modulo;

    /** Descripción legible para mostrar en el frontend. */
    private final String descripcion;
}
