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
    USUARIO_ROLES_ACTUALIZADOS("USUARIOS", "Asignación o retiro de roles de un usuario (HU-02)"),
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
    EVENTO_CERRADO("EVENTOS", "Paso de un evento a cerrado: automático al pasar su fecha de fin o en su cierre formal (HU-28)"),
    EVENTO_FECHAS_MODIFICADAS("EVENTOS", "Modificación de las fechas de un evento publicado o en ejecución"),

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
    PROPUESTA_VERSION_CREADA("PROPUESTAS", "Creación de una versión de la propuesta con sus documentos (HU-14)"),
    PROPUESTA_EDICION_AUTORIZADA("PROPUESTAS", "Autorización explícita para modificar una propuesta bloqueada (HU-14)"),
    CONVOCATORIA_PUBLICADA("CONVOCATORIAS", "Publicación de una convocatoria (HU-11)"),
    CONVOCATORIA_FECHA_CIERRE_MODIFICADA("CONVOCATORIAS", "Modificación de la fecha límite de una convocatoria publicada (HU-11)"),

    // ----- Rúbrica y comité evaluador (HU-15) -----
    RUBRICA_CREADA("EVALUACION", "Creación de la rúbrica de evaluación de una convocatoria (HU-15)"),
    RUBRICA_ACTUALIZADA("EVALUACION", "Modificación de una rúbrica sin evaluaciones registradas (HU-15)"),
    RUBRICA_REEMPLAZADA("EVALUACION", "Reemplazo confirmado de una rúbrica con evaluaciones registradas (HU-15)"),
    EVALUADOR_REGISTRADO("EVALUACION", "Registro de un miembro del comité evaluador de una convocatoria (HU-15)"),
    EVALUADOR_ACTUALIZADO("EVALUACION", "Modificación del área de experticia de un evaluador (HU-15)"),
    EVALUADOR_RETIRADO("EVALUACION", "Retiro de un evaluador sin propuestas asignadas del comité (HU-15)"),

    // ----- Asignación de propuestas a evaluadores (HU-16) -----
    PROPUESTA_ASIGNADA("EVALUACION", "Asignación de una propuesta a un evaluador del comité (HU-16)"),
    PROPUESTA_REASIGNADA("EVALUACION", "Reasignación de una propuesta a otro evaluador del comité (HU-16)"),

    // ----- Evaluaciones y puntuación ponderada (HU-17) -----
    EVALUACION_REGISTRADA("EVALUACION", "Registro de calificaciones de evaluación de una propuesta (HU-17)"),
    PUNTUACION_CALCULADA("EVALUACION", "Cálculo automático de puntuación ponderada de una propuesta (HU-17)"),

    // ----- Clasificación de propuestas según resultado (HU-18) -----
    UMBRALES_CLASIFICACION_ACTUALIZADOS("EVALUACION", "Modificación de los umbrales de clasificación de una convocatoria (HU-18)"),
    PROPUESTA_CLASIFICADA("EVALUACION", "Aplicación automática de la clasificación de una propuesta según los umbrales (HU-18)"),
    CLASIFICACION_AJUSTADA("EVALUACION", "Ajuste manual, con motivo, de la clasificación de una propuesta (HU-18)"),

    // ----- Observaciones y versión definitiva (HU-19) -----
    OBSERVACIONES_PROPUESTA_REGISTRADAS("EVALUACION", "Registro de observaciones y ajustes requeridos a una propuesta (HU-19)"),
    OBSERVACION_PROPUESTA_RESUELTA("EVALUACION", "Una observación o ajuste requerido de una propuesta se dio por resuelto (HU-19)"),
    VERSION_DEFINITIVA_APROBADA("EVALUACION", "Aprobación de la versión definitiva de una propuesta (HU-19)"),

    // ----- Reservadas: se registran cuando exista la historia correspondiente -----
    RESULTADOS_PUBLICADOS("EVALUACION", "Publicación de resultados de evaluación de propuestas (HU-18)"),

    // ----- Salas y disponibilidad (HU-Salas) -----
    SALA_CREADA("SALAS", "Creación de una sala del evento"),
    SALA_ACTUALIZADA("SALAS", "Actualización de una sala del evento"),
    SALA_ELIMINADA("SALAS", "Eliminación de una sala del evento"),
    DISPONIBILIDAD_SALA_ACTUALIZADA("SALAS", "Actualización de disponibilidad horaria de una sala"),
    DISPONIBILIDAD_PONENTE_ACTUALIZADA("SALAS", "Actualización de disponibilidad horaria de un ponente"),

    // HU-21: programación de actividades de la agenda
    ACTIVIDAD_PROGRAMADA("AGENDA", "Programación de una actividad en la agenda del evento (HU-21)"),
    ACTIVIDAD_REPROGRAMADA("AGENDA", "Cambio de horario, sala, ponente u otros datos de una actividad (HU-21)"),
    ACTIVIDAD_ELIMINADA("AGENDA", "Eliminación de una actividad de la agenda (HU-21)"),

    // HU-22: generación y publicación de la agenda
    ACTIVIDAD_REGISTRADA_POR_PROGRAMAR("AGENDA", "Registro o modificación de una actividad sin horario para la generación automática (HU-22)"),
    ACTIVIDAD_DESPROGRAMADA("AGENDA", "Retiro del horario y la sala de una actividad, que vuelve a por programar (HU-22)"),
    AGENDA_GENERADA("AGENDA", "Aplicación de la propuesta de agenda generada automáticamente (HU-22)"),
    AGENDA_PUBLICADA("AGENDA", "Publicación de la agenda del evento a los participantes (HU-22)"),

    // HU-23: inscripciones a eventos y actividades
    INSCRIPCION_EVENTO_REGISTRADA("INSCRIPCIONES", "Inscripción de un participante al evento (HU-23)"),
    INSCRIPCION_EVENTO_CANCELADA("INSCRIPCIONES", "Cancelación de la inscripción de un participante al evento (HU-23)"),
    INSCRIPCION_ACTIVIDAD_REGISTRADA("INSCRIPCIONES", "Inscripción de un participante a una actividad (HU-23)"),
    INSCRIPCION_ACTIVIDAD_CANCELADA("INSCRIPCIONES", "Cancelación de la inscripción de un participante a una actividad (HU-23)"),

    // HU-24: código QR y registro de asistencia
    CODIGO_QR_REENVIADO("ASISTENCIA", "Reenvío del código QR de una inscripción, sin generar uno distinto (HU-24)"),
    ASISTENCIA_REGISTRADA("ASISTENCIA", "Registro de la asistencia de un participante a una actividad por QR o manual (HU-24)"),

    // HU-25: encuestas de satisfacción
    ENCUESTA_CREADA("ENCUESTAS", "Creación de una encuesta de satisfacción del evento o de una actividad (HU-25)"),
    ENCUESTA_ACTUALIZADA("ENCUESTAS", "Modificación de una encuesta de satisfacción en borrador (HU-25)"),
    ENCUESTA_ELIMINADA("ENCUESTAS", "Eliminación de una encuesta de satisfacción sin respuestas (HU-25)"),
    ENCUESTA_PUBLICADA("ENCUESTAS", "Publicación de una encuesta de satisfacción para los participantes que asistieron (HU-25)"),
    ENCUESTA_CERRADA("ENCUESTAS", "Cierre de una encuesta de satisfacción; no recibe más respuestas (HU-25)"),
    ENCUESTA_RESPONDIDA("ENCUESTAS", "Un participante respondió una encuesta de satisfacción; no incluye sus respuestas (HU-25)"),

    // HU-26: certificados de participación
    CERTIFICADO_GENERADO("CERTIFICADOS", "Emisión del certificado de asistente, ponente o conferencista de una persona en un evento (HU-26)"),
    CERTIFICADOS_GENERADOS_EVENTO("CERTIFICADOS", "Generación de los certificados de todos los participantes que cumplen los requisitos de un evento (HU-26)"),
    PLANTILLA_CERTIFICADO_ACTUALIZADA("CERTIFICADOS", "Cambio del fondo o de los firmantes de los certificados de un evento (HU-26)"),
    CONFIGURACION_CERTIFICADOS_ACTUALIZADA("CERTIFICADOS", "Cambio del porcentaje mínimo de asistencia para el certificado de asistente de un evento (HU-26)"),

    // HU-28: memoria histórica de los eventos
    MEMORIA_HISTORICA_CONSOLIDADA("MEMORIA_HISTORICA", "Cierre formal de un evento: su información queda consolidada en la memoria histórica, solo consulta (HU-28)"),
    MODIFICACION_HISTORICA_RECHAZADA("MEMORIA_HISTORICA", "Intento rechazado de modificar información de un evento cerrado formalmente (HU-28)"),
    ELIMINACION_HISTORICA_RECHAZADA("MEMORIA_HISTORICA", "Intento rechazado de eliminar información de un evento cerrado formalmente (HU-28)"),
    MEMORIA_HISTORICA_ELIMINADA("MEMORIA_HISTORICA", "Eliminación con autorización especial de un evento cerrado formalmente y de su memoria histórica (HU-28)");

    /** Módulo funcional al que pertenece la operación. */
    private final String modulo;

    /** Descripción legible para mostrar en el frontend. */
    private final String descripcion;
}
