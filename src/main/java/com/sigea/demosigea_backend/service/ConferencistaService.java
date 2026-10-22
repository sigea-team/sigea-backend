package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.comite.MiembroComiteResponse;
import com.sigea.demosigea_backend.dto.conferencista.ActualizarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.AsociarConferencistaRequest;
import com.sigea.demosigea_backend.dto.conferencista.CoincidenciaConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.CompletarInvitacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaRegistradoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistaPublicoResponse;
import com.sigea.demosigea_backend.dto.conferencista.ConferencistasPublicosEventoResponse;
import com.sigea.demosigea_backend.dto.conferencista.EstudioRequest;
import com.sigea.demosigea_backend.dto.conferencista.EstudioResponse;
import com.sigea.demosigea_backend.dto.conferencista.ParticipacionHistorialResponse;
import com.sigea.demosigea_backend.dto.conferencista.PerfilConferencistaResponse;
import com.sigea.demosigea_backend.dto.conferencista.PublicacionRequest;
import com.sigea.demosigea_backend.dto.conferencista.PublicacionResponse;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaRequest;
import com.sigea.demosigea_backend.dto.conferencista.TrayectoriaResponse;
import com.sigea.demosigea_backend.exception.ConferencistaExistenteException;
import com.sigea.demosigea_backend.exception.ConferencistaYaAsociadoException;
import com.sigea.demosigea_backend.exception.DatosConferencistaInvalidosException;
import com.sigea.demosigea_backend.exception.DatosPersonaDuplicadosException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.EstudioConferencista;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.PerfilConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.PublicacionConferencista;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PerfilConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servicio de negocio del módulo de conferencistas (HU-12, RF16 y RF17).
 * <ul>
 *   <li><b>Criterio 1:</b> registrar los datos personales, de contacto y la trayectoria (biografía,
 *       estudios y publicaciones) y asociarlos al evento y al perfil del conferencista. Los registra el
 *       propio conferencista desde su invitación ({@link InvitacionConferencistaService}).</li>
 *   <li><b>Criterio 2:</b> rechazar el registro con datos obligatorios incompletos (Bean Validation
 *       en los DTO y las reglas de {@link #validarTrayectoria}).</li>
 *   <li><b>Criterio 3:</b> los conferencistas ya registrados se eligen de una lista
 *       ({@link #listarRegistrados}) y se asocian sin pedir de nuevo sus datos ({@link #asociar}); si se
 *       invita un correo que ya es de un conferencista, no se crea un duplicado.</li>
 *   <li><b>Criterio 4:</b> la trayectoria es una sola por persona y la vista pública la lee en cada
 *       consulta, así que al actualizarla el cambio se ve de inmediato en todos los eventos
 *       publicados donde participa.</li>
 * </ul>
 * <p>
 * <b>Modelo.</b> Datos personales y de contacto: {@link Persona} (documento y correo son únicos).
 * Trayectoria: {@link PerfilConferencista} (uno por persona). Asociación con cada evento:
 * {@link ParticipacionConferencista}; el conjunto de participaciones es el historial (RF17).
 * </p>
 * <p>
 * <b>Regla de estado.</b> Igual que el comité organizador (HU-06): los conferencistas se asocian o
 * retiran de un evento mientras está en {@code en_configuracion} o {@code habilitado}. Desde
 * {@code en_ejecucion} la lista queda congelada para la certificación (RF47) y la memoria
 * histórica (RF55). La trayectoria, en cambio, se puede actualizar siempre (Criterio 4).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConferencistaService {

    static final String ENTIDAD_PARTICIPACION = "participaciones_conferencista";
    static final String ENTIDAD_PERFIL = "perfiles_conferencista";

    /** Índice único: una persona una sola vez por evento (changeset hu12-004). */
    static final String INDICE_PARTICIPACION_UNICA = "ux_participaciones_conferencista_evento_persona";

    /** SQLState estándar de violación de unicidad en PostgreSQL. */
    static final String SQLSTATE_VIOLACION_UNICIDAD = "23505";

    /** Estados en los que se pueden asociar o retirar conferencistas del evento. */
    static final Set<EstadoEvento> ESTADOS_ASOCIACION_MODIFICABLE =
            EnumSet.of(EstadoEvento.en_configuracion, EstadoEvento.habilitado);

    /** Estados en los que el evento ya fue publicado y sus conferencistas son visibles al público. */
    static final Set<EstadoEvento> ESTADOS_EVENTO_PUBLICO =
            EnumSet.of(EstadoEvento.habilitado, EstadoEvento.en_ejecucion, EstadoEvento.cerrado);

    static final String COINCIDE_DOCUMENTO = "documento";
    static final String COINCIDE_CORREO = "correo";
    static final String COINCIDE_AMBOS = "documento_y_correo";

    private final PersonaRepository personaRepository;
    private final PerfilConferencistaRepository perfilRepository;
    private final ParticipacionConferencistaRepository participacionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Consultas del módulo
    // ------------------------------------------------------------------

    /**
     * Lista los conferencistas asociados a un evento.
     *
     * @param eventoId ID del evento.
     * @return Conferencistas en el orden en que se registraron.
     */
    @Transactional(readOnly = true)
    public List<ConferencistaEventoResponse> listarPorEvento(Long eventoId) {
        buscarEvento(eventoId);
        List<ParticipacionConferencista> participaciones =
                participacionRepository.findByEvento_IdOrderByFechaRegistroAscIdAsc(eventoId);
        Map<Long, PerfilConferencista> perfiles = perfilesPorPersona(participaciones);
        return participaciones.stream()
                .map(p -> ConferencistaEventoResponse.de(p, perfiles.get(p.getPersona().getId())))
                .toList();
    }

    /**
     * Criterio 3: busca una persona registrada por documento o correo, para avisar antes de
     * registrarla otra vez y ofrecer asociarla al evento.
     *
     * @param numeroDocumento Documento a buscar (opcional).
     * @param correo          Correo a buscar (opcional).
     * @param eventoId        Evento actual, para saber si ya está asociada (opcional).
     * @return La persona encontrada, o vacío si no existe.
     * @throws SolicitudInvalidaException si no se envía documento ni correo.
     */
    @Transactional(readOnly = true)
    public Optional<CoincidenciaConferencistaResponse> buscarCoincidencia(String numeroDocumento, String correo,
                                                                         Long eventoId) {
        if (estaVacio(numeroDocumento) && estaVacio(correo)) {
            throw new SolicitudInvalidaException("Indique el número de documento o el correo a buscar.");
        }
        return coincidencia(textoONulo(numeroDocumento), normalizarCorreo(correo), eventoId);
    }

    /**
     * Perfil completo del conferencista con su historial de participaciones (RF17).
     *
     * @param personaId ID de la persona.
     * @return Datos, trayectoria e historial.
     * @throws RecursoNoEncontradoException si la persona no existe o no es conferencista.
     */
    @Transactional(readOnly = true)
    public PerfilConferencistaResponse obtenerPerfil(Long personaId) {
        Persona persona = buscarPersona(personaId);
        Optional<PerfilConferencista> perfil = perfilRepository.findByPersona_Id(personaId);
        List<ParticipacionConferencista> historial =
                participacionRepository.historialDePersona(personaId);
        if (perfil.isEmpty() && historial.isEmpty()) {
            throw noEsConferencista(persona);
        }
        return construirPerfil(persona, perfil.orElse(null), historial);
    }

    // ------------------------------------------------------------------
    // Criterio 3: lista de conferencistas registrados para vincular
    // ------------------------------------------------------------------

    /** Máximo de conferencistas registrados que se devuelven en una búsqueda. */
    static final int MAXIMO_REGISTRADOS = 50;

    /**
     * Conferencistas ya registrados (con trayectoria), para que el administrador elija a quién vincular
     * a un evento sin volver a pedir sus datos (Criterio 3).
     *
     * @param busqueda Texto a buscar en nombre, documento o correo (opcional).
     * @param eventoId Evento actual, para marcar quién ya está asociado (opcional).
     * @return Hasta {@value #MAXIMO_REGISTRADOS} conferencistas, ordenados por nombre.
     */
    @Transactional(readOnly = true)
    public List<ConferencistaRegistradoResponse> listarRegistrados(String busqueda, Long eventoId) {
        String patron = estaVacio(busqueda) ? "%" : "%" + busqueda.trim().toLowerCase() + "%";
        List<PerfilConferencista> perfiles =
                perfilRepository.buscarRegistrados(patron, PageRequest.of(0, MAXIMO_REGISTRADOS));
        if (perfiles.isEmpty()) {
            return List.of();
        }
        List<Long> ids = perfiles.stream().map(p -> p.getPersona().getId()).toList();
        Map<Long, List<ParticipacionConferencista>> participaciones = participacionRepository.findByPersona_IdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(p -> p.getPersona().getId()));
        return perfiles.stream().map(perfil -> {
            Persona persona = perfil.getPersona();
            List<ParticipacionConferencista> suyas = participaciones.getOrDefault(persona.getId(), List.of());
            Boolean yaAsociado = eventoId == null ? null
                    : suyas.stream().anyMatch(p -> eventoId.equals(p.getEvento().getId()));
            return new ConferencistaRegistradoResponse(
                    persona.getId(),
                    nombreCompleto(persona),
                    persona.getTipoDocumento(),
                    persona.getNumeroDocumento(),
                    persona.getCorreo(),
                    perfil.getCargoActual(),
                    perfil.getInstitucion(),
                    suyas.size(),
                    yaAsociado,
                    perfil.getFechaActualizacion());
        }).toList();
    }

    // ------------------------------------------------------------------
    // Criterios 1 y 2: registro hecho por el conferencista desde su invitación
    // ------------------------------------------------------------------

    /**
     * Registra al conferencista con los datos que él mismo envía desde el enlace de la invitación y lo
     * asocia al evento (Criterio 1). Si ya existe una persona con el correo de la invitación (p. ej. un
     * participante con cuenta), se completan sus datos en lugar de crear otra.
     * <p>
     * Lo invoca {@link InvitacionConferencistaService#completar} dentro de su transacción.
     * </p>
     *
     * @param evento  Evento de la invitación (en un estado que admite conferencistas).
     * @param correo  Correo de la invitación (ya normalizado).
     * @param tipo    Tipo de participación de la invitación.
     * @param request Datos personales, tema y trayectoria.
     * @return La participación creada (o la existente si ya estaba asociado).
     * @throws DatosPersonaDuplicadosException      si el documento pertenece a otra persona.
     * @throws DatosConferencistaInvalidosException si un año de la trayectoria es posterior al actual.
     */
    @Transactional
    public ParticipacionConferencista registrarDesdeInvitacion(Evento evento, String correo,
                                                        TipoParticipacionConferencista tipo,
                                                        CompletarInvitacionRequest request) {
        validarAsociacionModificable(evento);
        validarTrayectoria(request.trayectoria());
        Optional<Persona> existente = personaRepository.findByCorreoIgnoreCase(correo);
        // El formulario público no recibe el documento ni el teléfono guardados (van enmascarados):
        // si llegan vacíos, se conservan los que la persona ya tiene.
        String documento = textoONulo(request.numeroDocumento());
        if (documento == null) {
            documento = existente.map(Persona::getNumeroDocumento).map(ConferencistaService::textoONulo).orElse(null);
        }
        if (documento == null) {
            throw new DatosConferencistaInvalidosException("El número de documento es obligatorio",
                    Map.of("numeroDocumento", "El número de documento es obligatorio"));
        }
        String telefono = textoONulo(request.telefono());
        if (telefono == null) {
            telefono = existente.map(Persona::getTelefono).map(ConferencistaService::textoONulo).orElse(null);
        }

        String documentoFinal = documento;
        personaRepository.findByNumeroDocumento(documentoFinal)
                .filter(otra -> existente.map(p -> !p.getId().equals(otra.getId())).orElse(true))
                .ifPresent(otra -> {
                    throw new DatosPersonaDuplicadosException("numeroDocumento",
                            "Este número de documento ya está registrado en SIGEA con otro correo. "
                                    + "Comunícate con el organizador del evento para que te vincule.");
                });

        Persona persona = existente.orElseGet(() -> Persona.builder().correo(correo).build());
        persona.setTipoDocumento(request.tipoDocumento().trim().toUpperCase());
        persona.setNumeroDocumento(documentoFinal);
        persona.setNombres(request.nombres().trim());
        persona.setApellidos(request.apellidos().trim());
        persona.setTelefono(telefono);
        persona = persona.getId() == null ? guardarPersonaNueva(persona) : guardarPersonaExistente(persona);

        Persona duena = persona;
        PerfilConferencista perfil = perfilRepository.findByPersona_Id(persona.getId())
                .orElseGet(() -> PerfilConferencista.builder().persona(duena).build());
        aplicarTrayectoria(perfil, request.trayectoria());
        if (perfil.getId() != null) {
            perfil.setFechaActualizacion(LocalDateTime.now());
        }
        perfilRepository.save(perfil);

        Optional<ParticipacionConferencista> yaAsociada = participacionRepository.historialDePersona(persona.getId())
                .stream()
                .filter(p -> evento.getId().equals(p.getEvento().getId()))
                .findFirst();
        if (yaAsociada.isPresent()) {
            return yaAsociada.get();
        }
        ParticipacionConferencista participacion = guardarParticipacion(ParticipacionConferencista.builder()
                .persona(persona)
                .evento(evento)
                .tipoParticipacion(tipo)
                .tema(textoONulo(request.tema()))
                .build(), persona, evento);

        Map<String, Object> detalle = detalleParticipacion(evento, persona, participacion);
        detalle.put("medio", "invitacion");
        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_REGISTRADO, ENTIDAD_PARTICIPACION,
                participacion.getId(), detalle);
        log.info("Conferencista registrado desde invitación: persona {}, evento {}, participación {}",
                persona.getId(), evento.getId(), participacion.getId());
        return participacion;
    }

    // ------------------------------------------------------------------
    // Criterio 3: asociar a una persona ya registrada
    // ------------------------------------------------------------------

    /**
     * Criterio 3: asocia al evento a una persona que ya existe, sin duplicar sus datos.
     * Si la persona no tiene trayectoria, la petición debe traerla; si la trae y ya tenía una,
     * la reemplaza.
     *
     * @param eventoId ID del evento.
     * @param request  Persona, tipo de participación, tema y (si hace falta) trayectoria.
     * @return El conferencista asociado al evento.
     * @throws ConferencistaYaAsociadoException     si ya está asociado al evento.
     * @throws DatosConferencistaInvalidosException si falta la trayectoria o un año es posterior al actual.
     */
    @Transactional
    public ConferencistaEventoResponse asociar(Long eventoId, AsociarConferencistaRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarAsociacionModificable(evento);
        Persona persona = buscarPersona(request.personaId());

        if (participacionRepository.existsByEvento_IdAndPersona_Id(eventoId, persona.getId())) {
            throw yaAsociado(persona, evento);
        }

        Optional<PerfilConferencista> perfilExistente = perfilRepository.findByPersona_Id(persona.getId());
        TrayectoriaRequest trayectoria = request.trayectoria();
        if (perfilExistente.isEmpty() && trayectoria == null) {
            throw new DatosConferencistaInvalidosException(
                    nombreCompleto(persona) + " todavía no tiene trayectoria registrada. "
                            + "Complete la biografía para asociarlo como conferencista.",
                    Map.of("biografia", "La biografía del conferencista es obligatoria"));
        }

        PerfilConferencista perfil = perfilExistente.orElseGet(() -> PerfilConferencista.builder().persona(persona).build());
        if (trayectoria != null) {
            validarTrayectoria(trayectoria);
            aplicarTrayectoria(perfil, trayectoria);
            if (perfil.getId() != null) {
                perfil.setFechaActualizacion(LocalDateTime.now());
            }
            perfil = perfilRepository.save(perfil);
        }

        ParticipacionConferencista participacion = guardarParticipacion(ParticipacionConferencista.builder()
                .persona(persona)
                .evento(evento)
                .tipoParticipacion(request.tipoParticipacion())
                .tema(textoONulo(request.tema()))
                .build(), persona, evento);

        Map<String, Object> detalle = detalleParticipacion(evento, persona, participacion);
        detalle.put("trayectoriaActualizada", trayectoria != null);
        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_ASOCIADO, ENTIDAD_PARTICIPACION,
                participacion.getId(), detalle);

        log.info("Conferencista asociado: persona {}, evento {}, participación {}",
                persona.getId(), evento.getId(), participacion.getId());
        return ConferencistaEventoResponse.de(participacion, perfil);
    }

    /**
     * Retira a un conferencista del evento. Su persona y su trayectoria se conservan para otros eventos.
     *
     * @param eventoId        ID del evento.
     * @param participacionId ID de la asociación.
     * @throws RecursoNoEncontradoException  si la asociación no existe en ese evento.
     * @throws OperacionNoPermitidaException si el evento está en ejecución o cerrado.
     */
    @Transactional
    public void desasociar(Long eventoId, Long participacionId) {
        Evento evento = buscarEvento(eventoId);
        validarAsociacionModificable(evento);
        ParticipacionConferencista participacion = participacionRepository.findByIdAndEvento_Id(participacionId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el conferencista con participación %d en el evento %d.", participacionId, eventoId)));

        Map<String, Object> detalle = detalleParticipacion(evento, participacion.getPersona(), participacion);
        participacionRepository.delete(participacion);

        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_DESASOCIADO, ENTIDAD_PARTICIPACION,
                participacionId, detalle);
        log.info("Conferencista retirado del evento {}: participación {}", eventoId, participacionId);
    }

    // ------------------------------------------------------------------
    // Criterio 4: actualizar datos y trayectoria
    // ------------------------------------------------------------------

    /**
     * Actualiza los datos personales, de contacto y la trayectoria del conferencista. Como la vista
     * pública lee el mismo perfil, el cambio se refleja de inmediato en los eventos publicados.
     *
     * @param personaId ID de la persona.
     * @param request   Datos y trayectoria completos.
     * @return Perfil actualizado con su historial (indica en qué eventos es visible públicamente).
     * @throws DatosPersonaDuplicadosException      si el documento o el correo son de otra persona.
     * @throws DatosConferencistaInvalidosException si se cambia el correo de una persona con cuenta o
     *                                              un año es posterior al actual.
     */
    @Transactional
    public PerfilConferencistaResponse actualizar(Long personaId, ActualizarConferencistaRequest request) {
        Persona persona = buscarPersona(personaId);
        PerfilConferencista perfil = perfilRepository.findByPersona_Id(personaId)
                .orElseThrow(() -> noEsConferencista(persona));

        String documento = request.numeroDocumento().trim();
        String correo = normalizarCorreo(request.correo());
        validarDatosUnicos(persona, documento, correo);
        validarTrayectoria(request.trayectoria());

        // Se comparan ANTES de modificar las entidades: si no cambió nada no se toca la base de datos
        // (ni se reemplazan estudios/publicaciones) y la fecha de actualización pública se conserva.
        Map<String, Object> nuevos = datosPersonales(request.tipoDocumento().trim().toUpperCase(), documento,
                request.nombres().trim(), request.apellidos().trim(), correo, textoONulo(request.telefono()));
        Map<String, Object> antes = datosPersonales(persona.getTipoDocumento(), persona.getNumeroDocumento(),
                persona.getNombres(), persona.getApellidos(), persona.getCorreo(), persona.getTelefono());
        List<String> modificados = new ArrayList<>();
        nuevos.forEach((campo, valor) -> {
            if (!Objects.equals(antes.get(campo), valor)) {
                modificados.add(campo);
            }
        });
        boolean trayectoriaModificada = !firmaTrayectoria(perfil).equals(firmaTrayectoria(request.trayectoria()));
        if (trayectoriaModificada) {
            modificados.add("trayectoria");
        }

        List<ParticipacionConferencista> historial = participacionRepository.historialDePersona(personaId);
        if (modificados.isEmpty()) {
            return construirPerfil(persona, perfil, historial);
        }

        persona.setTipoDocumento((String) nuevos.get("tipoDocumento"));
        persona.setNumeroDocumento(documento);
        persona.setNombres((String) nuevos.get("nombres"));
        persona.setApellidos((String) nuevos.get("apellidos"));
        persona.setCorreo(correo);
        persona.setTelefono((String) nuevos.get("telefono"));
        if (trayectoriaModificada) {
            aplicarTrayectoria(perfil, request.trayectoria());
        }
        perfil.setFechaActualizacion(LocalDateTime.now());
        guardarPersonaActualizada(persona);
        perfil = perfilRepository.save(perfil);

        List<String> eventosPublicos = historial.stream()
                .filter(p -> esPublico(p.getEvento()))
                .map(p -> p.getEvento().getNombre())
                .toList();
        auditoriaService.registrar(TipoOperacionAuditoria.CONFERENCISTA_ACTUALIZADO, ENTIDAD_PERFIL, perfil.getId(),
                DetalleAuditoria.de(
                        "persona", nombreCompleto(persona),
                        "numeroDocumento", persona.getNumeroDocumento(),
                        "camposModificados", modificados,
                        "eventosPublicosAfectados", eventosPublicos));

        log.info("Conferencista actualizado: persona {}, campos {}, eventos públicos {}",
                personaId, modificados, eventosPublicos.size());
        return construirPerfil(persona, perfil, historial);
    }

    // ------------------------------------------------------------------
    // Perfil de ingreso "Conferencista": el propio conferencista ve y actualiza su trayectoria
    // ------------------------------------------------------------------

    /**
     * Perfil del conferencista que inició sesión. Se identifica por el correo de su cuenta, que es
     * el mismo de su registro en {@code personas}.
     *
     * @param correoUsuario Correo del usuario autenticado (nombre del principal del JWT).
     * @return Datos, trayectoria e historial de eventos.
     * @throws RecursoNoEncontradoException si el organizador todavía no lo ha registrado como conferencista.
     */
    @Transactional(readOnly = true)
    public PerfilConferencistaResponse obtenerMiPerfil(String correoUsuario) {
        Persona persona = conferencistaDelUsuario(correoUsuario);
        return obtenerPerfil(persona.getId());
    }

    /**
     * El conferencista que inició sesión actualiza sus datos y su trayectoria. Aplican las mismas
     * reglas que cuando lo hace el organizador ({@link #actualizar}): su correo no se puede cambiar
     * porque es su usuario de inicio de sesión, y el cambio se refleja en la vista pública (Criterio 4).
     *
     * @param correoUsuario Correo del usuario autenticado.
     * @param request       Datos y trayectoria completos.
     * @return Perfil actualizado.
     */
    @Transactional
    public PerfilConferencistaResponse actualizarMiPerfil(String correoUsuario, ActualizarConferencistaRequest request) {
        Persona persona = conferencistaDelUsuario(correoUsuario);
        return actualizar(persona.getId(), request);
    }

    /**
     * Persona del usuario autenticado, solo si ya tiene trayectoria registrada. El perfil lo crea el
     * organizador (HU-12); el conferencista no se registra a sí mismo.
     */
    private Persona conferencistaDelUsuario(String correoUsuario) {
        if (estaVacio(correoUsuario)) {
            throw new RecursoNoEncontradoException("No se pudo identificar al usuario de la sesión.");
        }
        Persona persona = personaRepository.findByCorreoIgnoreCase(correoUsuario.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró una persona registrada con el correo " + correoUsuario.trim() + "."));
        if (!perfilRepository.existsByPersona_Id(persona.getId())) {
            throw new RecursoNoEncontradoException(
                    "Aún no estás registrado como conferencista. El organizador del evento debe registrarte "
                            + "con tu correo " + persona.getCorreo() + " para que puedas ver y actualizar tu trayectoria.");
        }
        return persona;
    }

    // ------------------------------------------------------------------
    // Criterio 4: vista pública
    // ------------------------------------------------------------------

    /**
     * Conferencistas de un evento publicado, con su trayectoria vigente y sin datos de contacto.
     *
     * @param eventoId ID del evento.
     * @return Evento y conferencistas.
     * @throws RecursoNoEncontradoException si el evento no existe o todavía no está publicado.
     */
    @Transactional(readOnly = true)
    public ConferencistasPublicosEventoResponse listarPublico(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .filter(ConferencistaService::esPublico)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un evento publicado con ID: " + eventoId));

        List<ParticipacionConferencista> participaciones =
                participacionRepository.findByEvento_IdOrderByFechaRegistroAscIdAsc(eventoId);
        Map<Long, PerfilConferencista> perfiles = perfilesPorPersona(participaciones);

        List<ConferencistaPublicoResponse> conferencistas = participaciones.stream()
                .map(p -> conferencistaPublico(p, perfiles.get(p.getPersona().getId())))
                .toList();
        return new ConferencistasPublicosEventoResponse(evento.getId(), evento.getNombre(), evento.getSemestre(),
                evento.getFechaInicio(), evento.getFechaFin(), conferencistas);
    }

    // ------------------------------------------------------------------
    // Reglas y utilidades
    // ------------------------------------------------------------------

    /** {@code true} si el evento ya fue publicado (sus conferencistas son visibles al público). */
    static boolean esPublico(Evento evento) {
        return evento != null && ESTADOS_EVENTO_PUBLICO.contains(evento.getEstado());
    }

    /**
     * Criterio 2: reglas de la trayectoria que no cubre Bean Validation. Ningún año (de estudios o
     * publicaciones) puede ser posterior al año actual.
     */
    void validarTrayectoria(TrayectoriaRequest trayectoria) {
        if (trayectoria == null) {
            return;
        }
        int anioActual = Year.now().getValue();
        Map<String, String> errores = new LinkedHashMap<>();
        List<EstudioRequest> estudios = listaSegura(trayectoria.estudios());
        for (int i = 0; i < estudios.size(); i++) {
            Integer anio = estudios.get(i).anio();
            if (anio != null && anio > anioActual) {
                errores.put("trayectoria.estudios[" + i + "].anio",
                        "El año del estudio no puede ser posterior a " + anioActual);
            }
        }
        List<PublicacionRequest> publicaciones = listaSegura(trayectoria.publicaciones());
        for (int i = 0; i < publicaciones.size(); i++) {
            Integer anio = publicaciones.get(i).anio();
            if (anio != null && anio > anioActual) {
                errores.put("trayectoria.publicaciones[" + i + "].anio",
                        "El año de la publicación no puede ser posterior a " + anioActual);
            }
        }
        if (!errores.isEmpty()) {
            throw new DatosConferencistaInvalidosException(
                    "Los datos enviados no cumplen con los requisitos o políticas exigidas.", errores);
        }
    }

    /**
     * Al actualizar: el documento y el correo no pueden pertenecer a otra persona, y el correo de
     * una persona con cuenta de usuario no se cambia aquí (es su usuario de inicio de sesión).
     */
    private void validarDatosUnicos(Persona persona, String documento, String correo) {
        if (!documento.equals(persona.getNumeroDocumento())) {
            personaRepository.findByNumeroDocumento(documento)
                    .filter(otra -> !otra.getId().equals(persona.getId()))
                    .ifPresent(otra -> {
                        throw new DatosPersonaDuplicadosException("numeroDocumento",
                                "El documento " + documento + " ya pertenece a otra persona registrada ("
                                        + nombreCompleto(otra) + ").");
                    });
        }
        if (!correo.equalsIgnoreCase(persona.getCorreo())) {
            if (usuarioRepository.existsByPersona_Id(persona.getId())) {
                throw new DatosConferencistaInvalidosException(
                        "El correo de " + nombreCompleto(persona) + " es su usuario de inicio de sesión en SIGEA "
                                + "y no se puede cambiar desde el módulo de conferencistas.",
                        Map.of("correo", "Este correo está vinculado a una cuenta de usuario y no se puede cambiar aquí"));
            }
            personaRepository.findByCorreoIgnoreCase(correo)
                    .filter(otra -> !otra.getId().equals(persona.getId()))
                    .ifPresent(otra -> {
                        throw new DatosPersonaDuplicadosException("correo",
                                "El correo " + correo + " ya pertenece a otra persona registrada ("
                                        + nombreCompleto(otra) + ").");
                    });
        }
    }

    /**
     * Busca una persona por documento y por correo. Si ambos coinciden con personas distintas,
     * se prioriza la del documento (es el identificador oficial).
     */
    public Optional<CoincidenciaConferencistaResponse> coincidencia(String documento, String correo, Long eventoId) {
        Optional<Persona> porDocumento = documento == null ? Optional.empty()
                : personaRepository.findByNumeroDocumento(documento);
        Optional<Persona> porCorreo = correo == null ? Optional.empty()
                : personaRepository.findByCorreoIgnoreCase(correo);
        if (porDocumento.isEmpty() && porCorreo.isEmpty()) {
            return Optional.empty();
        }

        Persona persona = porDocumento.orElseGet(porCorreo::get);
        String coincidePor;
        if (porDocumento.isPresent() && porCorreo.isPresent() && porDocumento.get().getId().equals(porCorreo.get().getId())) {
            coincidePor = COINCIDE_AMBOS;
        } else {
            coincidePor = porDocumento.isPresent() ? COINCIDE_DOCUMENTO : COINCIDE_CORREO;
        }

        Boolean yaAsociado = eventoId == null ? null
                : participacionRepository.existsByEvento_IdAndPersona_Id(eventoId, persona.getId());
        return Optional.of(new CoincidenciaConferencistaResponse(
                persona.getId(),
                nombreCompleto(persona),
                persona.getTipoDocumento(),
                persona.getNumeroDocumento(),
                persona.getCorreo(),
                coincidePor,
                perfilRepository.existsByPersona_Id(persona.getId()),
                yaAsociado,
                participacionRepository.countByPersona_Id(persona.getId())));
    }

    static String mensajeExistente(CoincidenciaConferencistaResponse c) {
        String por = switch (c.coincidePor()) {
            case COINCIDE_AMBOS -> "ese número de documento y correo";
            case COINCIDE_CORREO -> "ese correo";
            default -> "ese número de documento";
        };
        if (Boolean.TRUE.equals(c.yaAsociadoAlEvento())) {
            return String.format("%s ya está registrado con %s y ya es conferencista de este evento.",
                    c.nombreCompleto(), por);
        }
        return String.format("%s ya está registrado con %s. Puede asociarlo directamente a este evento "
                + "sin volver a registrar sus datos.", c.nombreCompleto(), por);
    }

    private void aplicarTrayectoria(PerfilConferencista perfil, TrayectoriaRequest trayectoria) {
        perfil.setBiografia(trayectoria.biografia().trim());
        perfil.setCargoActual(textoONulo(trayectoria.cargoActual()));
        perfil.setInstitucion(textoONulo(trayectoria.institucion()));
        perfil.setEnlacePerfil(textoONulo(trayectoria.enlacePerfil()));
        perfil.reemplazarEstudios(listaSegura(trayectoria.estudios()).stream()
                .map(e -> EstudioConferencista.builder()
                        .nivel(e.nivel())
                        .titulo(e.titulo().trim())
                        .institucion(e.institucion().trim())
                        .anio(e.anio())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new)));
        perfil.reemplazarPublicaciones(listaSegura(trayectoria.publicaciones()).stream()
                .map(p -> PublicacionConferencista.builder()
                        .tipo(p.tipo())
                        .titulo(p.titulo().trim())
                        .medio(textoONulo(p.medio()))
                        .anio(p.anio())
                        .enlace(textoONulo(p.enlace()))
                        .build())
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Representación comparable de la trayectoria guardada, para saber si una actualización cambia
     * algo. Debe producir el mismo texto que {@link #firmaTrayectoria(TrayectoriaRequest)} para los
     * mismos datos.
     */
    static String firmaTrayectoria(PerfilConferencista perfil) {
        StringBuilder sb = new StringBuilder();
        agregarFirma(sb, perfil.getBiografia(), perfil.getCargoActual(), perfil.getInstitucion(), perfil.getEnlacePerfil());
        sb.append("#E");
        perfil.getEstudios().forEach(e -> agregarFirma(sb, e.getNivel(), e.getTitulo(), e.getInstitucion(), e.getAnio()));
        sb.append("#P");
        perfil.getPublicaciones().forEach(p -> agregarFirma(sb, p.getTipo(), p.getTitulo(), p.getMedio(), p.getAnio(),
                p.getEnlace()));
        return sb.toString();
    }

    /** Firma de la trayectoria recibida, normalizada igual que {@link #aplicarTrayectoria}. */
    static String firmaTrayectoria(TrayectoriaRequest t) {
        StringBuilder sb = new StringBuilder();
        agregarFirma(sb, t.biografia().trim(), textoONulo(t.cargoActual()), textoONulo(t.institucion()),
                textoONulo(t.enlacePerfil()));
        sb.append("#E");
        listaSegura(t.estudios()).forEach(e -> agregarFirma(sb, e.nivel(), e.titulo().trim(), e.institucion().trim(),
                e.anio()));
        sb.append("#P");
        listaSegura(t.publicaciones()).forEach(p -> agregarFirma(sb, p.tipo(), p.titulo().trim(), textoONulo(p.medio()),
                p.anio(), textoONulo(p.enlace())));
        return sb.toString();
    }

    private static void agregarFirma(StringBuilder sb, Object... valores) {
        for (Object valor : valores) {
            sb.append(valor).append('\u001F');
        }
        sb.append('\u001E');
    }

    private static Map<String, Object> datosPersonales(String tipoDocumento, String numeroDocumento, String nombres,
                                                       String apellidos, String correo, String telefono) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipoDocumento", tipoDocumento);
        datos.put("numeroDocumento", numeroDocumento);
        datos.put("nombres", nombres);
        datos.put("apellidos", apellidos);
        datos.put("correo", correo);
        datos.put("telefono", telefono);
        return datos;
    }

    private PerfilConferencistaResponse construirPerfil(Persona persona, PerfilConferencista perfil,
                                                        List<ParticipacionConferencista> historial) {
        return new PerfilConferencistaResponse(
                persona.getId(),
                persona.getTipoDocumento(),
                persona.getNumeroDocumento(),
                persona.getNombres(),
                persona.getApellidos(),
                nombreCompleto(persona),
                persona.getCorreo(),
                persona.getTelefono(),
                usuarioRepository.existsByPersona_Id(persona.getId()),
                TrayectoriaResponse.de(perfil),
                historial.stream()
                        .map(p -> ParticipacionHistorialResponse.de(p, esPublico(p.getEvento())))
                        .toList());
    }

    private static ConferencistaPublicoResponse conferencistaPublico(ParticipacionConferencista participacion,
                                                                     PerfilConferencista perfil) {
        Persona persona = participacion.getPersona();
        return new ConferencistaPublicoResponse(
                persona.getId(),
                nombreCompleto(persona),
                participacion.getTipoParticipacion(),
                participacion.getTema(),
                perfil != null ? perfil.getCargoActual() : null,
                perfil != null ? perfil.getInstitucion() : null,
                perfil != null ? perfil.getBiografia() : null,
                perfil != null ? perfil.getEnlacePerfil() : null,
                perfil != null ? perfil.getEstudios().stream().map(EstudioResponse::de).toList() : List.of(),
                perfil != null ? perfil.getPublicaciones().stream().map(PublicacionResponse::de).toList() : List.of(),
                perfil != null ? perfil.getFechaActualizacion() : null);
    }

    private Map<Long, PerfilConferencista> perfilesPorPersona(List<ParticipacionConferencista> participaciones) {
        if (participaciones.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = participaciones.stream().map(p -> p.getPersona().getId()).distinct().toList();
        return perfilRepository.findByPersona_IdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getPersona().getId(), Function.identity(), (a, b) -> a));
    }

    private Persona guardarPersonaNueva(Persona persona) {
        try {
            return personaRepository.saveAndFlush(persona);
        } catch (DataIntegrityViolationException ex) {
            // Otra petición registró el mismo documento o correo entre la validación y el guardado.
            log.warn("Unicidad de personas violada al registrar conferencista {}: {}",
                    persona.getNumeroDocumento(), ex.getMostSpecificCause().getMessage());
            throw new DatosPersonaDuplicadosException("numeroDocumento",
                    "Otra persona se registró con ese documento o correo hace un momento. "
                            + "Búsquela de nuevo para asociarla al evento.");
        }
    }

    private Persona guardarPersonaExistente(Persona persona) {
        try {
            return personaRepository.saveAndFlush(persona);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Unicidad de personas violada al completar invitación de persona {}: {}",
                    persona.getId(), ex.getMostSpecificCause().getMessage());
            throw new DatosPersonaDuplicadosException("numeroDocumento",
                    "Este número de documento ya está registrado en SIGEA con otro correo.");
        }
    }

    private void guardarPersonaActualizada(Persona persona) {
        try {
            personaRepository.saveAndFlush(persona);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Unicidad de personas violada al actualizar persona {}: {}",
                    persona.getId(), ex.getMostSpecificCause().getMessage());
            throw new DatosPersonaDuplicadosException("numeroDocumento",
                    "El documento o el correo ya pertenecen a otra persona registrada.");
        }
    }

    /**
     * Guarda con flush inmediato: si dos peticiones simultáneas asocian a la misma persona, el índice
     * {@value #INDICE_PARTICIPACION_UNICA} lo detecta aquí y se responde 409 en vez de 500.
     */
    private ParticipacionConferencista guardarParticipacion(ParticipacionConferencista participacion,
                                                            Persona persona, Evento evento) {
        try {
            return participacionRepository.saveAndFlush(participacion);
        } catch (DataIntegrityViolationException ex) {
            if (esViolacionParticipacionUnica(ex)) {
                throw yaAsociado(persona, evento);
            }
            throw ex;
        }
    }

    /** Determina si la excepción corresponde al índice {@value #INDICE_PARTICIPACION_UNICA}. */
    static boolean esViolacionParticipacionUnica(Throwable ex) {
        for (Throwable causa = ex; causa != null; causa = causa.getCause() == causa ? null : causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                String restriccion = violacion.getConstraintName();
                return SQLSTATE_VIOLACION_UNICIDAD.equals(violacion.getSQLState())
                        && restriccion != null
                        && INDICE_PARTICIPACION_UNICA.equalsIgnoreCase(restriccion.replace("\"", ""));
            }
        }
        return false;
    }

    public void validarAsociacionModificable(Evento evento) {
        if (!ESTADOS_ASOCIACION_MODIFICABLE.contains(evento.getEstado())) {
            throw new OperacionNoPermitidaException(String.format(
                    "Los conferencistas del evento '%s' ya no se pueden asociar ni retirar (estado actual: %s). "
                            + "Solo se permite mientras el evento está en en_configuracion o habilitado. "
                            + "La trayectoria de cada conferencista sí se puede actualizar.",
                    evento.getNombre(), evento.getEstado()));
        }
    }

    private static Map<String, Object> detalleParticipacion(Evento evento, Persona persona,
                                                            ParticipacionConferencista participacion) {
        return DetalleAuditoria.de(
                "evento", evento.getNombre(),
                "persona", nombreCompleto(persona),
                "numeroDocumento", persona.getNumeroDocumento(),
                "tipoParticipacion", participacion.getTipoParticipacion() != null
                        ? participacion.getTipoParticipacion().name() : null,
                "tema", participacion.getTema());
    }

    public Evento buscarEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + id));
    }

    private Persona buscarPersona(Long id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la persona con ID: " + id));
    }

    private static RecursoNoEncontradoException noEsConferencista(Persona persona) {
        return new RecursoNoEncontradoException(
                nombreCompleto(persona) + " no está registrado como conferencista.");
    }

    private static ConferencistaYaAsociadoException yaAsociado(Persona persona, Evento evento) {
        return new ConferencistaYaAsociadoException(String.format(
                "%s ya es conferencista del evento '%s'.", nombreCompleto(persona), evento.getNombre()));
    }

    static String nombreCompleto(Persona persona) {
        return MiembroComiteResponse.nombreCompleto(persona);
    }

    static String normalizarCorreo(String correo) {
        return estaVacio(correo) ? null : correo.trim().toLowerCase();
    }

    static String textoONulo(String valor) {
        return estaVacio(valor) ? null : valor.trim();
    }

    static boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static <T> List<T> listaSegura(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
