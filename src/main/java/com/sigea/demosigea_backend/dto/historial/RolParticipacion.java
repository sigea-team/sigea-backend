package com.sigea.demosigea_backend.dto.historial;

/**
 * Rol con el que una persona participó en un evento o actividad (HU-27, Criterio 1).
 * <p>
 * No corresponde a una columna de la base de datos: se deriva de las tablas de las que sale el historial.
 * </p>
 * <ul>
 *   <li>{@link #PARTICIPANTE}: inscrito al evento ({@code inscripciones_evento}) o a sus actividades
 *       ({@code inscripciones_actividad}).</li>
 *   <li>{@link #CONFERENCISTA}: asociado al evento como conferencista ({@code participaciones_conferencista}).</li>
 *   <li>{@link #PONENTE}: asociado como ponente ({@code participaciones_conferencista}), ponente asignado a una
 *       actividad ({@code actividades.ponente_id}) o ponente de una propuesta aprobada ({@code propuestas_autores}).</li>
 *   <li>{@link #AUTOR}: autor o coautor de una propuesta postulada a una convocatoria del evento.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
public enum RolParticipacion {
    PARTICIPANTE,
    CONFERENCISTA,
    PONENTE,
    AUTOR
}
