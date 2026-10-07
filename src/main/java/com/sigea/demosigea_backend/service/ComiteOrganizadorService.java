package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.comite.MiembroComiteRequest;
import com.sigea.demosigea_backend.dto.comite.MiembroComiteResponse;
import com.sigea.demosigea_backend.exception.MiembroComiteDuplicadoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.ComiteOrganizador;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ComiteOrganizadorRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para el registro de responsables y comité organizador de un evento
 * (HU-06, RF05).
 * <ul>
 *   <li><b>Criterio 1:</b> asociar una persona al evento con su rol dentro del comité.</li>
 *   <li><b>Criterio 2:</b> impedir que una persona con participación vigente se agregue de nuevo.</li>
 *   <li><b>Criterio 3:</b> retirar a un miembro de la lista vigente conservando su historial
 *       (borrado lógico: {@code activo = false} + {@code fecha_retiro}).</li>
 *   <li><b>Criterio 4:</b> el comité solo se modifica mientras el evento está en
 *       {@code en_configuracion} o {@code habilitado}.</li>
 * </ul>
 * Las altas y retiros se registran en la auditoría (RF56) dentro de la misma transacción.
 * <p>
 * <b>Regla de estado (decisión del PO).</b> Los datos generales del evento solo cambian en
 * {@code en_configuracion} (HU-04), porque son lo que aprueban el comité curricular y el Rector.
 * El comité organizador es el equipo de trabajo y cambia durante la organización, que ocurre
 * principalmente en {@code habilitado} (convocatoria, evaluación, agenda, inscripciones); por eso
 * también se permite modificarlo en ese estado. Desde {@code en_ejecucion} queda congelado, para que
 * los certificados de organizador (RF47) y la memoria histórica (RF55) se generen sobre un comité
 * estable. Ver {@link #ESTADOS_COMITE_MODIFICABLE}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComiteOrganizadorService {

    /** Nombre de la entidad usado en el log de auditoría. */
    static final String ENTIDAD_COMITE = "comite_organizador";

    /** Índice único parcial: una participación vigente por persona y evento (changeset hu06-002). */
    static final String INDICE_MIEMBRO_ACTIVO = "ux_comite_organizador_miembro_activo";

    /** SQLState estándar de violación de unicidad en PostgreSQL. */
    static final String SQLSTATE_VIOLACION_UNICIDAD = "23505";

    /**
     * Estados del evento en los que el comité organizador puede modificarse (Criterio 4).
     * En {@code en_ejecucion} y {@code cerrado} el comité queda congelado.
     */
    static final Set<EstadoEvento> ESTADOS_COMITE_MODIFICABLE =
            EnumSet.of(EstadoEvento.en_configuracion, EstadoEvento.habilitado);

    private final ComiteOrganizadorRepository comiteRepository;
    private final EventoRepository eventoRepository;
    private final PersonaRepository personaRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Consulta
    // ------------------------------------------------------------------

    /**
     * Lista el comité organizador de un evento.
     *
     * @param eventoId         ID del evento.
     * @param incluirHistorial {@code false} = solo miembros vigentes; {@code true} = vigentes y retirados.
     * @return Participaciones del comité.
     */
    @Transactional(readOnly = true)
    public List<MiembroComiteResponse> listar(Long eventoId, boolean incluirHistorial) {
        buscarEvento(eventoId);
        List<ComiteOrganizador> miembros = incluirHistorial
                ? comiteRepository.findByEvento_IdOrderByActivoDescFechaAsignacionAscIdAsc(eventoId)
                : comiteRepository.findByEvento_IdAndActivoTrueOrderByFechaAsignacionAscIdAsc(eventoId);
        return miembros.stream().map(MiembroComiteResponse::fromEntity).toList();
    }

    // ------------------------------------------------------------------
    // Criterios 1 y 2: agregar miembro
    // ------------------------------------------------------------------

    /**
     * Vincula una persona al comité organizador del evento con el rol indicado.
     *
     * @param eventoId ID del evento (debe existir y estar en un estado de {@link #ESTADOS_COMITE_MODIFICABLE}).
     * @param request  Persona (por ID o documento) y rol dentro del comité.
     * @return Participación creada.
     * @throws RecursoNoEncontradoException     si el evento o la persona no existen.
     * @throws OperacionNoPermitidaException    si el evento está en ejecución o cerrado (Criterio 4).
     * @throws MiembroComiteDuplicadoException  si la persona ya es miembro vigente del evento (Criterio 2).
     */
    @Transactional
    public MiembroComiteResponse agregar(Long eventoId, MiembroComiteRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarEventoModificable(evento);

        Persona persona = resolverPersona(request);

        if (comiteRepository.existsByEvento_IdAndPersona_IdAndActivoTrue(eventoId, persona.getId())) {
            throw duplicado(persona, evento);
        }

        ComiteOrganizador miembro = ComiteOrganizador.builder()
                .evento(evento)
                .persona(persona)
                .rolComite(request.rolComite().trim())
                .activo(true)
                .build();

        ComiteOrganizador guardado = guardarControlandoDuplicado(miembro, persona, evento);

        auditoriaService.registrar(TipoOperacionAuditoria.MIEMBRO_COMITE_AGREGADO, ENTIDAD_COMITE, guardado.getId(),
                detalleAuditoria(evento, persona, guardado, false));

        log.info("Miembro agregado al comité: participación {}, evento {}, persona {}, rol '{}'",
                guardado.getId(), evento.getId(), persona.getId(), guardado.getRolComite());
        return MiembroComiteResponse.fromEntity(guardado);
    }

    // ------------------------------------------------------------------
    // Criterio 3: retirar miembro
    // ------------------------------------------------------------------

    /**
     * Retira a un miembro de la lista vigente del comité sin eliminar su registro, de modo que su
     * participación queda en el historial.
     *
     * @param eventoId   ID del evento.
     * @param miembroId  ID de la participación ({@code comite_id}).
     * @return Participación retirada (con {@code activo = false} y {@code fechaRetiro}).
     * @throws RecursoNoEncontradoException  si la participación no existe en ese evento.
     * @throws OperacionNoPermitidaException si ya estaba retirada o si el evento está en ejecución o cerrado.
     */
    @Transactional
    public MiembroComiteResponse retirar(Long eventoId, Long miembroId) {
        Evento evento = buscarEvento(eventoId);
        validarEventoModificable(evento);

        ComiteOrganizador miembro = comiteRepository.findByIdAndEvento_Id(miembroId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el miembro con ID %d en el comité del evento %d.", miembroId, eventoId)));

        if (!miembro.isActivo()) {
            throw new OperacionNoPermitidaException(String.format(
                    "%s ya fue retirado del comité organizador el %s.",
                    nombreCompleto(miembro.getPersona()), miembro.getFechaRetiro()));
        }

        miembro.retirar();
        ComiteOrganizador retirado = comiteRepository.save(miembro);

        auditoriaService.registrar(TipoOperacionAuditoria.MIEMBRO_COMITE_RETIRADO, ENTIDAD_COMITE, retirado.getId(),
                detalleAuditoria(evento, retirado.getPersona(), retirado, true));

        log.info("Miembro retirado del comité: participación {}, evento {}", retirado.getId(), evento.getId());
        return MiembroComiteResponse.fromEntity(retirado);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    /**
     * Datos afectados que se guardan en la auditoría (RF56).
     * <p>
     * Se guardan los <b>nombres</b> de la persona y del evento, no solo sus IDs: el registro de
     * auditoría es una fotografía del momento y debe poder leerse aunque después la persona cambie
     * de nombre o el evento se renombre, y sin consultar otras tablas. El ID de la participación ya
     * queda en {@code auditoria.entidad_id}.
     * </p>
     *
     * @param evento  Evento del comité.
     * @param persona Persona agregada o retirada.
     * @param miembro Participación en el comité.
     * @param retiro  {@code true} si la operación es un retiro (agrega la fecha de retiro).
     */
    private static Map<String, Object> detalleAuditoria(Evento evento, Persona persona,
                                                        ComiteOrganizador miembro, boolean retiro) {
        Map<String, Object> detalle = DetalleAuditoria.de(
                "evento", evento.getNombre(),
                "persona", nombreCompleto(persona),
                "numeroDocumento", persona != null ? persona.getNumeroDocumento() : null,
                "rolComite", miembro.getRolComite());
        if (retiro) {
            detalle.put("fechaRetiro", miembro.getFechaRetiro());
        }
        return detalle;
    }

    private Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    /**
     * Criterio 4: el comité solo se modifica en los estados de {@link #ESTADOS_COMITE_MODIFICABLE}.
     */
    private void validarEventoModificable(Evento evento) {
        if (!ESTADOS_COMITE_MODIFICABLE.contains(evento.getEstado())) {
            String permitidos = ESTADOS_COMITE_MODIFICABLE.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(" o "));
            throw new OperacionNoPermitidaException(String.format(
                    "El comité organizador del evento '%s' ya no puede modificarse (estado actual: %s). "
                            + "Solo se permiten cambios mientras el evento está en %s.",
                    evento.getNombre(), evento.getEstado(), permitidos));
        }
    }

    /**
     * Busca la persona por ID o, si no se envió, por número de documento.
     */
    private Persona resolverPersona(MiembroComiteRequest request) {
        if (request.personaId() != null) {
            return personaRepository.findById(request.personaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró la persona con ID: " + request.personaId()));
        }
        String documento = request.numeroDocumento().trim();
        return personaRepository.findByNumeroDocumento(documento)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una persona registrada con el documento: " + documento));
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas pasaron la validación del
     * Criterio 2, el índice {@value #INDICE_MIEMBRO_ACTIVO} lo detecta aquí y se responde 409 en vez de 500.
     */
    private ComiteOrganizador guardarControlandoDuplicado(ComiteOrganizador miembro, Persona persona, Evento evento) {
        try {
            return comiteRepository.saveAndFlush(miembro);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionMiembroActivo(ex)) {
                log.warn("El índice {} rechazó un miembro duplicado: evento {}, persona {}. Se responde 409.",
                        INDICE_MIEMBRO_ACTIVO, evento.getId(), persona.getId());
                throw duplicado(persona, evento);
            }
            throw ex;
        }
    }

    /**
     * Determina si la excepción corresponde al índice {@value #INDICE_MIEMBRO_ACTIVO}, usando el
     * SQLState y el nombre de la restricción (no el texto del mensaje).
     */
    static boolean esViolacionMiembroActivo(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                String restriccion = violacion.getConstraintName();
                return SQLSTATE_VIOLACION_UNICIDAD.equals(violacion.getSQLState())
                        && restriccion != null
                        && INDICE_MIEMBRO_ACTIVO.equalsIgnoreCase(restriccion.replace("\"", ""));
            }
        }
        return false;
    }

    private static MiembroComiteDuplicadoException duplicado(Persona persona, Evento evento) {
        return new MiembroComiteDuplicadoException(String.format(
                "%s ya es miembro vigente del comité organizador del evento '%s'.",
                nombreCompleto(persona), evento.getNombre()));
    }

    private static String nombreCompleto(Persona persona) {
        return MiembroComiteResponse.nombreCompleto(persona);
    }
}
