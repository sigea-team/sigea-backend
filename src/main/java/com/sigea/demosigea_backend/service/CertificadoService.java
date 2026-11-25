package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.certificado.CertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.ConfiguracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.EmisionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.ExclusionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.GeneracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.HabilitacionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.MisCertificadosEventoResponse;
import com.sigea.demosigea_backend.dto.certificado.OpcionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.VerificacionCertificadoResponse;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Certificado;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.CertificadoRepository;
import com.sigea.demosigea_backend.repository.CertificadoRepository.ResumenAsistencia;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generación y entrega de los certificados de participación (HU-26, RF47, CU-23).
 * <ul>
 *   <li>Criterio 1: si la persona cumple los requisitos de su rol, se emite el certificado con el rol (asistente, ponente
 *       o conferencista) y los datos del evento, en PDF.</li>
 *   <li>Criterio 2: si no los cumple, se informa el motivo y no se emite nada.</li>
 *   <li>Criterio 3: si ya existe, se entrega el mismo, sin crear otro registro (bloqueo por persona y evento + índice
 *       único {@code ux_certificados_persona_evento_tipo}).</li>
 * </ul>
 * Requisitos por rol:
 * <ul>
 *   <li>Asistente: asistir (HU-24) a un porcentaje de las actividades en las que se inscribió (HU-23), sin contar las
 *       canceladas ni las por programar, igual o mayor al mínimo del evento (RN-01, {@link ReglasCertificado}).</li>
 *   <li>Ponente o conferencista: tener a cargo al menos una actividad realizada del evento. Es conferencista quien está
 *       registrado así en el evento (HU-12); los demás ponentes de actividades reciben el de ponente.</li>
 * </ul>
 * Nadie genera certificados hasta que el organizador los habilita ({@link #habilitar}): exige la plantilla completa
 * (fondo y al menos una firma, {@link PlantillaCertificadoService}), la deja fija y genera los de todos los que cumplen.
 * Después cada participante encuentra el suyo en SIGEA; no se envían por correo.
 * Los certificados se generan cuando el evento terminó (estado {@code cerrado} o pasada su fecha de fin): antes la
 * asistencia todavía puede cambiar. Con el evento cerrado formalmente (HU-28) solo se entregan los ya emitidos.
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificadoService {

    private static final String ENTIDAD = "certificados";
    private static final String PREFIJO_CODIGO = "CERT-";
    private static final int BYTES_CODIGO = 12;
    private static final int INTENTOS_CODIGO = 5;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final String MENSAJE_NO_HABILITADOS =
            "El organizador aún no ha habilitado los certificados de este evento.";

    /** Autoridades que gestionan los certificados de un evento. */
    static final Set<String> AUTORIDADES_GESTION = Set.of("CERTIFICADOS_EMITIR", "ROLE_ADMIN", "ADMIN");

    private final CertificadoRepository certificadoRepository;
    private final EventoRepository eventoRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ParticipacionConferencistaRepository participacionRepository;
    private final GeneradorPdfCertificado generadorPdf;
    private final AlmacenamientoArchivos almacenamiento;
    private final AuditoriaService auditoriaService;
    private final PlantillaCertificadoService plantillaService;
    private final Clock clock;

    /** Base de la página pública de verificación del frontend; el código se agrega al final. */
    @Value("${app.certificados.url-verificacion:http://localhost:5173/#/verificar/}")
    private String urlVerificacionBase;

    // ──────────────────────────────────────────────────────────────────────────────
    // Participante
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Roles del usuario autenticado en el evento, si cumple los requisitos de cada uno y sus certificados emitidos.
     *
     * @param eventoId evento.
     * @return estado de sus certificados en el evento.
     */
    @Transactional(readOnly = true)
    public MisCertificadosEventoResponse consultarMios(Long eventoId) {
        Persona persona = personaAutenticada();
        Evento evento = buscarEvento(eventoId);
        boolean finalizado = estaFinalizado(evento);
        boolean habilitados = plantillaService.fechaHabilitacion(eventoId).isPresent();

        Map<TipoCertificado, Certificado> emitidos = certificadoRepository
                .findByPersona_IdAndEvento_IdOrderByFechaGeneracionAscIdAsc(persona.getId(), eventoId).stream()
                .filter(c -> c.getActividad() == null)
                .collect(Collectors.toMap(Certificado::getTipo, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        List<Evaluacion> evaluaciones = new ArrayList<>();
        Optional<ResumenAsistencia> asistencia = certificadoRepository.resumenAsistencia(eventoId, persona.getId());
        if (asistencia.isPresent() || emitidos.containsKey(TipoCertificado.asistente)) {
            evaluaciones.add(evaluarAsistente(evento, asistencia.orElse(null)));
        }
        List<Actividad> aCargo = certificadoRepository.actividadesComoPonente(eventoId, persona.getId());
        Optional<ParticipacionConferencista> participacion =
                participacionRepository.findByEvento_IdAndPersona_Id(eventoId, persona.getId());
        TipoCertificado rol = rolExpositor(participacion.orElse(null));
        if (!aCargo.isEmpty() || participacion.isPresent()) {
            evaluaciones.add(evaluarExpositor(rol, rol, aCargo));
        }
        // Un certificado emitido de un rol que hoy ya no aplica (p. ej. cambió el registro) se sigue mostrando.
        for (TipoCertificado tipo : emitidos.keySet()) {
            if (evaluaciones.stream().noneMatch(e -> e.tipo() == tipo)) {
                evaluaciones.add(new Evaluacion(tipo, true, null, null, null, null, null, List.of(), null));
            }
        }

        List<OpcionCertificadoResponse> opciones = evaluaciones.stream()
                .map(e -> aOpcion(e, emitidos.get(e.tipo()), evento, finalizado, habilitados))
                .toList();
        return new MisCertificadosEventoResponse(evento.getId(), evento.getNombre(), evento.getFechaFin(), finalizado,
                evento.isArchivado(), porcentajeMinimo(evento), habilitados, opciones);
    }

    /**
     * Genera el certificado del usuario autenticado, o entrega el que ya tenía (Criterio 3).
     *
     * @param eventoId evento.
     * @param tipo     rol del certificado.
     * @return el certificado y si se emitió en esta solicitud.
     * @throws CertificadoException si no cumple los requisitos (Criterio 2), el evento no ha terminado o el tipo no se
     *                              emite todavía.
     */
    @Transactional
    public EmisionCertificadoResponse solicitar(Long eventoId, TipoCertificado tipo) {
        Persona persona = personaAutenticada();
        Evento evento = buscarEvento(eventoId);
        verificarTipoDisponible(tipo);

        // Mismo orden de bloqueos que la generación masiva (plantilla compartida → persona) para evitar interbloqueos.
        // El compartido impide que la plantilla cambie mientras se emite (PlantillaCertificadoService).
        certificadoRepository.bloquearPlantillaParaEmitir(eventoId);
        certificadoRepository.bloquearEmision(persona.getId(), eventoId);
        Optional<Certificado> existente = certificadoRepository
                .findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(persona.getId(), eventoId, tipo);
        if (existente.isPresent()) {
            return new EmisionCertificadoResponse(false,
                    "Tu certificado de " + tipo.getRolEnTexto() + " ya había sido generado; se entrega el mismo.",
                    CertificadoResponse.fromEntity(existente.get()));
        }

        MemoriaHistoricaReglas.verificarEditable(evento);
        verificarFinalizado(evento);
        verificarHabilitados(evento);
        Evaluacion evaluacion = evaluar(evento, persona.getId(), tipo);
        if (!evaluacion.cumple()) {
            throw new CertificadoException(CertificadoException.REQUISITOS_NO_CUMPLIDOS, HttpStatus.CONFLICT,
                    "No cumples los requisitos para el certificado de " + tipo.getRolEnTexto() + ". "
                            + evaluacion.motivo());
        }

        Certificado certificado = emitir(evento, persona, evaluacion, plantillaService.cargarParaPdf(eventoId));
        return new EmisionCertificadoResponse(true,
                "Se generó tu certificado de " + tipo.getRolEnTexto() + ".", CertificadoResponse.fromEntity(certificado));
    }

    /**
     * Certificados del usuario autenticado en todos los eventos (perfil del participante, CU-23).
     */
    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarMios() {
        Persona persona = personaAutenticada();
        return certificadoRepository.findByPersona_IdOrderByFechaGeneracionDescIdDesc(persona.getId()).stream()
                .map(CertificadoResponse::fromEntity)
                .toList();
    }

    /**
     * PDF de un certificado. Lo descarga su titular o quien gestiona certificados. Si el archivo ya no está en el
     * almacenamiento, se reconstruye igual con los datos guardados (Criterio 3), sin modificar el registro.
     *
     * @param certificadoId certificado.
     * @return nombre sugerido y contenido del PDF.
     */
    @Transactional(readOnly = true)
    public ArchivoCertificado descargar(Long certificadoId) {
        Certificado certificado = certificadoRepository.findWithEventoAndPersonaById(certificadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el certificado con ID " + certificadoId + "."));
        if (!tieneAutoridadDeGestion()) {
            Persona persona = personaAutenticada();
            if (certificado.getPersona() == null || !Objects.equals(certificado.getPersona().getId(), persona.getId())) {
                throw new AccessDeniedException("Solo el titular o quien gestiona los certificados puede descargarlo.");
            }
        }
        return new ArchivoCertificado(nombreArchivo(certificado), contenidoPdf(certificado));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Organizador (CU-23)
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Genera los certificados de todos los participantes del evento que cumplen los requisitos. Los que ya tenían
     * certificado no se duplican; los que no cumplen quedan en {@code excluidos} con el motivo (CU-23, flujo alterno).
     *
     * @param eventoId evento.
     * @return emitidos, ya existentes y excluidos.
     */
    @Transactional
    public GeneracionCertificadosResponse generarDelEvento(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        MemoriaHistoricaReglas.verificarEditable(evento);
        verificarFinalizado(evento);
        verificarHabilitados(evento);
        GeneracionCertificadosResponse resultado = generarTodos(evento);
        auditoriaService.registrar(TipoOperacionAuditoria.CERTIFICADOS_GENERADOS_EVENTO, "eventos", eventoId,
                resumenAuditoria(evento, resultado));
        return resultado;
    }

    /**
     * Habilita los certificados del evento (HU-26): exige que el evento haya terminado y que la plantilla tenga fondo y
     * al menos una firma; la plantilla queda fija y se generan de una vez los certificados de quienes cumplen. Desde
     * ese momento cada participante los encuentra en SIGEA (no se envían por correo).
     *
     * @param eventoId evento.
     * @return fecha de habilitación y resultado de la generación.
     */
    @Transactional
    public HabilitacionCertificadosResponse habilitar(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        MemoriaHistoricaReglas.verificarEditable(evento);
        verificarFinalizado(evento);
        LocalDateTime fecha = plantillaService.habilitar(evento);
        GeneracionCertificadosResponse resultado = generarTodos(evento);
        Map<String, Object> detalle = resumenAuditoria(evento, resultado);
        detalle.put("fechaHabilitacion", fecha);
        auditoriaService.registrar(TipoOperacionAuditoria.CERTIFICADOS_HABILITADOS, "eventos", eventoId, detalle);
        return new HabilitacionCertificadosResponse(eventoId, fecha, resultado.generados(), resultado.yaEmitidos(),
                resultado.excluidos());
    }

    private Map<String, Object> resumenAuditoria(Evento evento, GeneracionCertificadosResponse r) {
        return DetalleAuditoria.de(
                "eventoNombre", evento.getNombre(),
                "generados", r.generados().size(),
                "yaEmitidos", r.yaEmitidos(),
                "excluidos", r.excluidos().size(),
                "porcentajeMinimoAsistencia", porcentajeMinimo(evento));
    }

    /** Genera los certificados de todos los que cumplen (sin verificar estado ni registrar auditoría). */
    private GeneracionCertificadosResponse generarTodos(Evento evento) {
        Long eventoId = evento.getId();
        List<Candidato> candidatos = new ArrayList<>();
        for (ResumenAsistencia resumen : certificadoRepository.resumenAsistenciaDelEvento(eventoId)) {
            candidatos.add(new Candidato(resumen.getPersonaId(), TipoCertificado.asistente,
                    evaluarAsistente(evento, resumen)));
        }
        Map<Long, List<Actividad>> porPonente = certificadoRepository.actividadesConPonente(eventoId).stream()
                .collect(Collectors.groupingBy(a -> a.getPonente().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, ParticipacionConferencista> participaciones = participacionRepository
                .findByEvento_IdOrderByFechaRegistroAscIdAsc(eventoId).stream()
                .collect(Collectors.toMap(p -> p.getPersona().getId(), Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        List<Long> expositores = new ArrayList<>(porPonente.keySet());
        participaciones.keySet().stream().filter(id -> !porPonente.containsKey(id)).forEach(expositores::add);
        for (Long personaId : expositores) {
            TipoCertificado rol = rolExpositor(participaciones.get(personaId));
            candidatos.add(new Candidato(personaId, rol,
                    evaluarExpositor(rol, rol, porPonente.getOrDefault(personaId, List.of()))));
        }

        Map<Long, Persona> personas = personaRepository.findAllById(
                        candidatos.stream().map(Candidato::personaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Persona::getId, Function.identity()));

        certificadoRepository.bloquearPlantillaParaEmitir(eventoId);
        PlantillaCertificadoService.PlantillaPdf plantilla = null;

        List<CertificadoResponse> generados = new ArrayList<>();
        List<ExclusionCertificadoResponse> excluidos = new ArrayList<>();
        int yaEmitidos = 0;
        for (Candidato candidato : candidatos) {
            Persona persona = personas.get(candidato.personaId());
            if (persona == null) {
                continue;
            }
            certificadoRepository.bloquearEmision(persona.getId(), eventoId);
            if (certificadoRepository.findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(
                    persona.getId(), eventoId, candidato.tipo()).isPresent()) {
                yaEmitidos++;
            } else if (candidato.evaluacion().cumple()) {
                if (plantilla == null) {
                    plantilla = plantillaService.cargarParaPdf(eventoId);
                }
                generados.add(CertificadoResponse.fromEntity(emitir(evento, persona, candidato.evaluacion(), plantilla)));
            } else {
                excluidos.add(new ExclusionCertificadoResponse(persona.getId(), nombreDe(persona),
                        persona.getNumeroDocumento(), candidato.tipo(), candidato.evaluacion().motivo()));
            }
        }

        log.info("Certificados del evento {}: {} generados, {} ya emitidos, {} excluidos",
                eventoId, generados.size(), yaEmitidos, excluidos.size());
        return new GeneracionCertificadosResponse(eventoId, generados, yaEmitidos, excluidos);
    }

    /** Certificados emitidos en el evento. */
    @Transactional(readOnly = true)
    public List<CertificadoResponse> listarDelEvento(Long eventoId) {
        buscarEvento(eventoId);
        return certificadoRepository.findByEvento_IdOrderByTipoAscNombreParticipanteAscIdAsc(eventoId).stream()
                .map(CertificadoResponse::fromEntity)
                .toList();
    }

    /** Porcentaje mínimo de asistencia del evento y si se puede cambiar. */
    @Transactional(readOnly = true)
    public ConfiguracionCertificadosResponse consultarConfiguracion(Long eventoId) {
        return configuracion(buscarEvento(eventoId));
    }

    /**
     * Cambia el porcentaje mínimo de asistencia del evento (RN-01). Con certificados de asistente ya emitidos no se
     * cambia: los participantes quedarían certificados con reglas distintas.
     *
     * @param eventoId   evento.
     * @param porcentaje nuevo porcentaje (validado en el DTO: mayor que 0 y hasta 100).
     * @return la configuración actualizada.
     */
    @Transactional
    public ConfiguracionCertificadosResponse actualizarConfiguracion(Long eventoId, BigDecimal porcentaje) {
        Evento evento = buscarEvento(eventoId);
        MemoriaHistoricaReglas.verificarEditable(evento);
        BigDecimal nuevo = porcentaje.setScale(2, RoundingMode.HALF_UP);
        BigDecimal anterior = porcentajeMinimo(evento);
        if (anterior.compareTo(nuevo) == 0) {
            return configuracion(evento);
        }
        if (plantillaService.fechaHabilitacion(eventoId).isPresent()) {
            throw new CertificadoException(CertificadoException.CONFIGURACION_BLOQUEADA, HttpStatus.CONFLICT,
                    "Los certificados ya fueron habilitados con el mínimo de " + ReglasCertificado.formatear(anterior)
                            + "; el porcentaje ya no se puede cambiar.");
        }
        long deAsistente = certificadoRepository.countByEvento_IdAndTipo(eventoId, TipoCertificado.asistente);
        if (deAsistente > 0) {
            throw new CertificadoException(CertificadoException.CONFIGURACION_BLOQUEADA, HttpStatus.CONFLICT,
                    "Ya se emitieron " + deAsistente + " certificado(s) de asistente con el mínimo de "
                            + ReglasCertificado.formatear(anterior) + "; el porcentaje ya no se puede cambiar.");
        }
        evento.setPorcentajeMinimoAsistencia(nuevo);
        eventoRepository.save(evento);
        auditoriaService.registrar(TipoOperacionAuditoria.CONFIGURACION_CERTIFICADOS_ACTUALIZADA, "eventos", eventoId,
                DetalleAuditoria.de("porcentajeAnterior", anterior, "porcentajeNuevo", nuevo));
        return configuracion(evento);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Público
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Comprueba la autenticidad de un certificado por su código de verificación (sin sesión).
     *
     * @param codigo código impreso en el certificado.
     * @return datos del certificado con el documento parcialmente oculto.
     */
    @Transactional(readOnly = true)
    public VerificacionCertificadoResponse verificar(String codigo) {
        String limpio = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        Certificado c = certificadoRepository.findByCodigoVerificacion(limpio)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un certificado emitido por SIGEA con el código " + limpio + "."));
        return new VerificacionCertificadoResponse(c.getCodigoVerificacion(), c.getNombreParticipante(),
                documentoOculto(c.getTipoDocumento(), c.getNumeroDocumento()), c.getTipo(),
                c.getTipo().getDescripcion(), c.getNombreEvento(), c.getFechaInicioEvento(), c.getFechaFinEvento(),
                c.getFechaGeneracion());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Requisitos
    // ──────────────────────────────────────────────────────────────────────────────

    /** Resultado de revisar los requisitos de un rol. */
    record Evaluacion(TipoCertificado tipo, boolean cumple, String motivo, Long consideradas, Long asistidas,
                      BigDecimal porcentaje, Long minutos, List<String> actividades, String detalle) {
    }

    private record Candidato(Long personaId, TipoCertificado tipo, Evaluacion evaluacion) {
    }

    private Evaluacion evaluar(Evento evento, Long personaId, TipoCertificado tipo) {
        if (tipo == TipoCertificado.asistente) {
            return evaluarAsistente(evento, certificadoRepository.resumenAsistencia(evento.getId(), personaId)
                    .orElse(null));
        }
        TipoCertificado rol = rolExpositor(participacionRepository
                .findByEvento_IdAndPersona_Id(evento.getId(), personaId).orElse(null));
        return evaluarExpositor(tipo, rol, certificadoRepository.actividadesComoPonente(evento.getId(), personaId));
    }

    Evaluacion evaluarAsistente(Evento evento, ResumenAsistencia resumen) {
        TipoCertificado tipo = TipoCertificado.asistente;
        if (resumen == null) {
            return new Evaluacion(tipo, false, "No tiene inscripción en el evento.", null, null, null, null,
                    List.of(), null);
        }
        long consideradas = valor(resumen.getConsideradas());
        long asistidas = valor(resumen.getAsistidas());
        BigDecimal minimo = porcentajeMinimo(evento);
        if (consideradas == 0) {
            return new Evaluacion(tipo, false,
                    "No estaba inscrito en ninguna actividad realizada del evento; el certificado de asistente se "
                            + "calcula sobre las actividades inscritas.",
                    0L, asistidas, null, null, List.of(), null);
        }
        BigDecimal porcentaje = ReglasCertificado.porcentaje(asistidas, consideradas);
        long minutos = valor(resumen.getMinutosAsistidos());
        if (!ReglasCertificado.cumpleAsistencia(asistidas, consideradas, minimo)) {
            return new Evaluacion(tipo, false,
                    "Asistió a " + asistidas + " de " + actividadesInscritas(consideradas) + " ("
                            + ReglasCertificado.formatear(porcentaje) + "); el mínimo es "
                            + ReglasCertificado.formatear(minimo) + ".",
                    consideradas, asistidas, porcentaje, minutos, List.of(), null);
        }
        return new Evaluacion(tipo, true, null, consideradas, asistidas, porcentaje, minutos, List.of(),
                detalleAsistencia(asistidas, consideradas, porcentaje, minutos));
    }

    /**
     * @param pedido tipo que se revisa (ponente o conferencista).
     * @param rol    tipo que le corresponde según su registro en el evento.
     */
    Evaluacion evaluarExpositor(TipoCertificado pedido, TipoCertificado rol, List<Actividad> aCargo) {
        List<String> nombres = aCargo.stream().map(Actividad::getNombre).toList();
        if (pedido != rol) {
            return new Evaluacion(pedido, false,
                    "Su participación en el evento está registrada como " + rol.getRolEnTexto()
                            + "; le corresponde el certificado de " + rol.getRolEnTexto() + ".",
                    null, null, null, null, nombres, null);
        }
        if (aCargo.isEmpty()) {
            return new Evaluacion(pedido, false,
                    "No tiene actividades realizadas a su cargo en el evento (las canceladas o sin programar no cuentan).",
                    null, null, null, null, List.of(), null);
        }
        long minutos = aCargo.stream().mapToLong(CertificadoService::minutosDe).sum();
        return new Evaluacion(pedido, true, null, null, null, null, minutos, nombres,
                detalleExpositor(nombres, minutos));
    }

    /** Conferencista si así está registrado en el evento (HU-12); si no, ponente. */
    static TipoCertificado rolExpositor(ParticipacionConferencista participacion) {
        return participacion != null
                && participacion.getTipoParticipacion() == TipoParticipacionConferencista.conferencista
                ? TipoCertificado.conferencista : TipoCertificado.ponente;
    }

    static String detalleAsistencia(long asistidas, long consideradas, BigDecimal porcentaje, long minutos) {
        String base;
        if (consideradas == 1) {
            base = "Asistió a la actividad en la que se inscribió";
        } else if (asistidas == consideradas) {
            base = "Asistió a las " + consideradas + " actividades en las que se inscribió";
        } else {
            base = "Asistió a " + asistidas + " de las " + consideradas + " actividades en las que se inscribió";
        }
        return base + " (" + ReglasCertificado.formatear(porcentaje) + ")" + intensidad(minutos) + ".";
    }

    static String detalleExpositor(List<String> nombres, long minutos) {
        String lista = nombres.size() == 1
                ? "la actividad «" + nombres.get(0) + "»"
                : "las actividades " + enumerar(nombres.stream().map(n -> "«" + n + "»").toList());
        return "A cargo de " + lista + intensidad(minutos) + ".";
    }

    private static String intensidad(long minutos) {
        if (minutos <= 0) {
            return "";
        }
        if (minutos < 60) {
            return ", con una intensidad de " + minutos + " minutos";
        }
        BigDecimal horas = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        String texto = horas.stripTrailingZeros().toPlainString().replace('.', ',');
        return ", con una intensidad de " + texto + (horas.compareTo(BigDecimal.ONE) == 0 ? " hora" : " horas");
    }

    private static String enumerar(List<String> partes) {
        if (partes.size() <= 1) {
            return String.join("", partes);
        }
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " y " + partes.get(partes.size() - 1);
    }

    private static String actividadesInscritas(long cantidad) {
        return cantidad == 1 ? "1 actividad en la que se inscribió" : "las " + cantidad + " actividades en las que se inscribió";
    }

    private static long minutosDe(Actividad a) {
        if (a.getHoraInicio() == null || a.getHoraFin() == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.MINUTES.between(a.getHoraInicio(), a.getHoraFin()));
    }

    private static long valor(Long n) {
        return n == null ? 0 : n;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Emisión
    // ──────────────────────────────────────────────────────────────────────────────

    private Certificado emitir(Evento evento, Persona persona, Evaluacion evaluacion,
                               PlantillaCertificadoService.PlantillaPdf plantilla) {
        String codigo = nuevoCodigo();
        LocalDateTime fecha = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Certificado certificado = Certificado.builder()
                .persona(persona)
                .evento(evento)
                .tipo(evaluacion.tipo())
                .porcentajeAsistencia(evaluacion.tipo() == TipoCertificado.asistente ? evaluacion.porcentaje() : null)
                .fechaGeneracion(fecha)
                .codigoVerificacion(codigo)
                .nombreParticipante(recortar(nombreDe(persona), 201))
                .tipoDocumento(persona.getTipoDocumento())
                .numeroDocumento(persona.getNumeroDocumento())
                .nombreEvento(evento.getNombre())
                .fechaInicioEvento(evento.getFechaInicio())
                .fechaFinEvento(evento.getFechaFin())
                .detalle(evaluacion.detalle())
                .build();

        byte[] pdf = generadorPdf.generar(datosPdf(certificado, plantilla));
        String ruta = almacenamiento.guardar("certificados/" + evento.getId(), "pdf", new ByteArrayInputStream(pdf));
        eliminarArchivoSiSeRevierte(ruta);
        certificado.setArchivoUrl(ruta);

        Certificado guardado;
        try {
            guardado = certificadoRepository.saveAndFlush(certificado);
        } catch (DataIntegrityViolationException ex) {
            if (MemoriaHistoricaReglas.desdeErrorBaseDatos(ex).isPresent()) {
                throw ex;
            }
            throw new CertificadoException(CertificadoException.EMISION_SIMULTANEA, HttpStatus.CONFLICT,
                    "El certificado de " + evaluacion.tipo().getRolEnTexto() + " se acaba de generar en otra solicitud. "
                            + "Consúltelo de nuevo.");
        }

        auditoriaService.registrar(TipoOperacionAuditoria.CERTIFICADO_GENERADO, ENTIDAD, guardado.getId(),
                DetalleAuditoria.de(
                        "eventoId", evento.getId(),
                        "personaId", persona.getId(),
                        "tipo", evaluacion.tipo().name(),
                        "porcentajeAsistencia", guardado.getPorcentajeAsistencia(),
                        "codigoVerificacion", codigo
                ));
        log.info("Certificado {} ({}) emitido a la persona {} en el evento {}",
                guardado.getId(), evaluacion.tipo(), persona.getId(), evento.getId());
        return guardado;
    }

    private GeneradorPdfCertificado.DatosCertificado datosPdf(Certificado c,
                                                             PlantillaCertificadoService.PlantillaPdf plantilla) {
        return new GeneradorPdfCertificado.DatosCertificado(c.getTipo(), c.getNombreParticipante(),
                c.getTipoDocumento(), c.getNumeroDocumento(), c.getNombreEvento(), c.getFechaInicioEvento(),
                c.getFechaFinEvento(), c.getDetalle(), c.getFechaGeneracion(), c.getCodigoVerificacion(),
                urlVerificacion(c.getCodigoVerificacion()),
                plantilla != null ? plantilla.fondoJpeg() : null, plantilla != null ? plantilla.firmas() : List.of());
    }

    private byte[] contenidoPdf(Certificado c) {
        if (c.getArchivoUrl() != null && !c.getArchivoUrl().isBlank()) {
            try (InputStream entrada = almacenamiento.cargar(c.getArchivoUrl()).getInputStream()) {
                return entrada.readAllBytes();
            } catch (IOException | RuntimeException ex) {
                log.warn("No se pudo leer el PDF del certificado {} ({}); se reconstruye con los datos guardados.",
                        c.getId(), ex.getMessage());
            }
        }
        // La plantilla no cambia después de emitir el primer certificado, así que la actual es la que se usó.
        Long eventoId = c.getEvento() != null ? c.getEvento().getId() : null;
        return generadorPdf.generar(datosPdf(c, eventoId != null ? plantillaService.cargarParaPdf(eventoId) : null));
    }

    private String urlVerificacion(String codigo) {
        return urlVerificacion(urlVerificacionBase, codigo);
    }

    /**
     * Dirección impresa en el certificado: la página pública del frontend que consulta
     * {@code GET /api/v1/publico/certificados/{codigo}} ({@code app.certificados.url-verificacion} + código).
     *
     * @return la dirección, o null si no hay base configurada (el PDF solo muestra el código).
     */
    static String urlVerificacion(String base, String codigo) {
        if (base == null || base.isBlank()) {
            return null;
        }
        String b = base.trim();
        return (b.endsWith("/") ? b : b + "/") + codigo;
    }

    private String nuevoCodigo() {
        for (int intento = 0; intento < INTENTOS_CODIGO; intento++) {
            String candidato = generarCodigo();
            if (!certificadoRepository.existsByCodigoVerificacion(candidato)) {
                return candidato;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de verificación único. Intente de nuevo.");
    }

    static String generarCodigo() {
        byte[] bytes = new byte[BYTES_CODIGO];
        ALEATORIO.nextBytes(bytes);
        return PREFIJO_CODIGO + HexFormat.of().withUpperCase().formatHex(bytes);
    }

    /** Si la transacción se revierte, el PDF ya guardado se borra para no dejar archivos huérfanos. */
    private void eliminarArchivoSiSeRevierte(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    almacenamiento.eliminar(ruta);
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    /** Nombre del archivo PDF de un certificado. */
    public record ArchivoCertificado(String nombre, byte[] contenido) {
    }

    private OpcionCertificadoResponse aOpcion(Evaluacion e, Certificado emitido, Evento evento, boolean finalizado,
                                              boolean habilitados) {
        String motivo = null;
        boolean puedeGenerar = false;
        if (emitido == null) {
            if (!e.tipo().isEmisionDisponible()) {
                motivo = "Este tipo de certificado todavía no se emite.";
            } else if (!e.cumple()) {
                motivo = e.motivo();
            } else if (evento.isArchivado()) {
                motivo = "El evento fue cerrado formalmente; ya no se emiten certificados nuevos.";
            } else if (!finalizado) {
                motivo = "Disponible cuando termine el evento (" + formatear(evento.getFechaFin()) + ").";
            } else if (!habilitados) {
                motivo = MENSAJE_NO_HABILITADOS;
            } else {
                puedeGenerar = true;
            }
        }
        return new OpcionCertificadoResponse(e.tipo(), e.tipo().getDescripcion(), emitido != null || e.cumple(),
                puedeGenerar, motivo, e.consideradas(), e.asistidas(), e.porcentaje(), e.actividades(),
                emitido != null ? CertificadoResponse.fromEntity(emitido) : null);
    }

    private ConfiguracionCertificadosResponse configuracion(Evento evento) {
        long emitidos = certificadoRepository.countByEvento_Id(evento.getId());
        long deAsistente = certificadoRepository.countByEvento_IdAndTipo(evento.getId(), TipoCertificado.asistente);
        String motivo = null;
        if (evento.isArchivado()) {
            motivo = "El evento fue cerrado formalmente; su información solo se consulta.";
        } else if (plantillaService.fechaHabilitacion(evento.getId()).isPresent()) {
            motivo = "Los certificados ya fueron habilitados con este porcentaje.";
        } else if (deAsistente > 0) {
            motivo = "Ya se emitieron " + deAsistente + " certificado(s) de asistente con este porcentaje.";
        }
        return new ConfiguracionCertificadosResponse(evento.getId(), porcentajeMinimo(evento), motivo == null, motivo,
                emitidos);
    }

    private void verificarTipoDisponible(TipoCertificado tipo) {
        if (tipo == null || !tipo.isEmisionDisponible()) {
            throw new CertificadoException(CertificadoException.TIPO_NO_DISPONIBLE, HttpStatus.CONFLICT,
                    "Por ahora solo se emiten certificados de asistente, ponente y conferencista.");
        }
    }

    /** El evento terminó: estado cerrado o ya pasó su fecha de fin (el cambio automático de estado es a medianoche). */
    boolean estaFinalizado(Evento evento) {
        if (evento.getEstado() == EstadoEvento.cerrado) {
            return true;
        }
        return evento.getEstado() != EstadoEvento.en_configuracion && evento.getFechaFin() != null
                && LocalDate.now(clock).isAfter(evento.getFechaFin());
    }

    private void verificarHabilitados(Evento evento) {
        if (plantillaService.fechaHabilitacion(evento.getId()).isEmpty()) {
            throw new CertificadoException(CertificadoException.NO_HABILITADOS, HttpStatus.CONFLICT, MENSAJE_NO_HABILITADOS);
        }
    }

    private void verificarFinalizado(Evento evento) {
        if (!estaFinalizado(evento)) {
            throw new CertificadoException(CertificadoException.EVENTO_NO_FINALIZADO, HttpStatus.CONFLICT,
                    "Los certificados del evento «" + evento.getNombre() + "» se generan cuando termina ("
                            + formatear(evento.getFechaFin()) + "); antes la asistencia todavía puede cambiar.");
        }
    }

    private static BigDecimal porcentajeMinimo(Evento evento) {
        BigDecimal valor = evento.getPorcentajeMinimoAsistencia();
        return valor != null ? valor : Evento.PORCENTAJE_MINIMO_ASISTENCIA_POR_DEFECTO;
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID " + eventoId + "."));
    }

    private Persona personaAutenticada() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para consultar sus certificados.");
        }
        Usuario usuario = usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
        if (usuario.getPersona() == null) {
            throw new OperacionNoPermitidaException("El usuario autenticado no tiene una persona asociada.");
        }
        return usuario.getPersona();
    }

    private static boolean tieneAutoridadDeGestion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(AUTORIDADES_GESTION::contains);
    }

    static String nombreDe(Persona p) {
        String nombre = ((p.getNombres() != null ? p.getNombres() : "") + " "
                + (p.getApellidos() != null ? p.getApellidos() : "")).trim().replaceAll("\\s+", " ");
        return nombre.isEmpty() ? p.getCorreo() : nombre;
    }

    static String documentoOculto(String tipo, String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        String n = numero.trim();
        int visibles = Math.min(4, Math.max(0, n.length() - 2));
        String oculto = "*".repeat(n.length() - visibles) + n.substring(n.length() - visibles);
        return (tipo == null || tipo.isBlank() ? "" : tipo.trim().toUpperCase(Locale.ROOT) + " ") + oculto;
    }

    static String nombreArchivo(Certificado c) {
        return "certificado-" + c.getTipo().name() + "-" + c.getCodigoVerificacion() + ".pdf";
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }

    private static String formatear(LocalDate fecha) {
        return fecha == null ? "sin fecha" : fecha.format(FORMATO_FECHA);
    }
}
