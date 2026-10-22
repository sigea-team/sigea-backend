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

    // ----- Planeación y configuración de eventos (HU-06) -----
    MIEMBRO_COMITE_AGREGADO("EVENTOS", "Vinculación de una persona al comité organizador de un evento"),
    MIEMBRO_COMITE_RETIRADO("EVENTOS", "Retiro de un miembro del comité organizador vigente de un evento"),

    // ----- Parámetros del evento: tipos de actividad y líneas temáticas (HU-05) -----
    TIPO_ACTIVIDAD_CREADO("EVENTOS", "Creación de un tipo de actividad del evento"),
    TIPO_ACTIVIDAD_ACTUALIZADO("EVENTOS", "Modificación de un tipo de actividad del evento"),
    TIPO_ACTIVIDAD_ELIMINADO("EVENTOS", "Eliminación de un tipo de actividad del evento"),
    LINEA_TEMATICA_CREADA("EVENTOS", "Creación de una línea temática del evento"),
    LINEA_TEMATICA_ACTUALIZADA("EVENTOS", "Modificación de una línea temática del evento"),
    LINEA_TEMATICA_ELIMINADA("EVENTOS", "Eliminación de una línea temática del evento"),

    // ----- Catálogo de tipos de evento y ciclo de vida del evento -----
    TIPO_EVENTO_CREADO("EVENTOS", "Creación de un tipo de evento en el catálogo"),
    TIPO_EVENTO_ACTUALIZADO("EVENTOS", "Modificación de un tipo de evento del catálogo"),
    TIPO_EVENTO_ELIMINADO("EVENTOS", "Eliminación de un tipo de evento del catálogo"),
    EVENTO_PUBLICADO("EVENTOS", "Publicación de un evento (pasa a habilitado)"),
    EVENTO_INICIADO("EVENTOS", "Paso automático de un evento a en ejecución al llegar su fecha de inicio"),

    // ----- Conferencistas y ponentes (HU-12) -----
    CONFERENCISTA_INVITADO("CONFERENCISTAS", "Envío o reenvío de la invitación por correo a un conferencista nuevo (HU-12)"),
    INVITACION_CONFERENCISTA_CANCELADA("CONFERENCISTAS", "Cancelación de una invitación a un conferencista (HU-12)"),
    CONFERENCISTA_REGISTRADO("CONFERENCISTAS", "Registro de un conferencista desde su invitación, con su trayectoria (HU-12)"),
    CONFERENCISTA_ASOCIADO("CONFERENCISTAS", "Asociación de un conferencista ya registrado a un evento (HU-12)"),
    CONFERENCISTA_DESASOCIADO("CONFERENCISTAS", "Retiro de un conferencista de un evento (HU-12)"),
    CONFERENCISTA_ACTUALIZADO("CONFERENCISTAS", "Actualización de los datos o la trayectoria de un conferencista (HU-12)"),

    // ----- Presupuesto y Finanzas (HU-08) -----
    // ----- Reservadas: se registran cuando exista la historia correspondiente -----
    PRESUPUESTO_APROBADO("PRESUPUESTO", "Registro del presupuesto aprobado del evento (HU-08)"),
    PRESUPUESTO_ACTUALIZADO("PRESUPUESTO", "Actualización del presupuesto aprobado del evento (HU-08)"),

    // ----- Ejecución presupuestal (HU-09) -----
    GASTO_REGISTRADO("PRESUPUESTO", "Registro de un gasto ejecutado contra un rubro del evento (HU-09)"),

    // ----- Convocatorias y Propuestas (HU-11, HU-13) -----
    PROPUESTA_REGISTRADA("PROPUESTAS", "Registro o envío de una propuesta académica (HU-13)"),
    AUTOR_PROPUESTA_REGISTRADO("PROPUESTAS", "Registro o vinculación de un autor o ponente a una propuesta (HU-13)"),
    CONVOCATORIA_PUBLICADA("CONVOCATORIAS", "Publicación de una convocatoria (HU-11)"),
    CONVOCATORIA_FECHA_CIERRE_MODIFICADA("CONVOCATORIAS", "Modificación de la fecha límite de una convocatoria publicada (HU-11)"),

    // ----- Reservadas: se registran cuando exista la historia correspondiente -----
    RESULTADOS_PUBLICADOS("EVALUACION", "Publicación de resultados de evaluación de propuestas (HU-18)");

    /** Módulo funcional al que pertenece la operación. */
    private final String modulo;

    /** Descripción legible para mostrar en el frontend. */
    private final String descripcion;
}
