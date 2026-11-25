package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.memoria.CierreFormalRequest;
import com.sigea.demosigea_backend.dto.memoria.EliminacionHistoricaRequest;
import com.sigea.demosigea_backend.dto.memoria.EstadoCierreResponse;
import com.sigea.demosigea_backend.dto.memoria.IndicadorResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResponse;
import com.sigea.demosigea_backend.dto.memoria.MemoriaHistoricaResumenResponse;
import com.sigea.demosigea_backend.dto.memoria.PendienteCierreResponse;
import com.sigea.demosigea_backend.exception.CierreConPendientesException;
import com.sigea.demosigea_backend.exception.MemoriaHistoricaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Indicador;
import com.sigea.demosigea_backend.model.MemoriaHistorica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.IndicadorRepository;
import com.sigea.demosigea_backend.repository.MemoriaHistoricaRepository;
import com.sigea.demosigea_backend.repository.MemoriaHistoricaRepository.ConteoPendientes;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Memoria histórica de los eventos (HU-28, RF55, CU-32).
 * <ul>
 *   <li><b>Criterio 1:</b> el cierre formal de un evento finalizado consolida toda su información (configuración,
 *       comité, presupuesto, convocatorias y propuestas, conferencistas, agenda, participantes, asistencia, encuestas
 *       e indicadores) en {@code memorias_historicas}. Si hay procesos pendientes, se advierten y se exige
 *       confirmación explícita (CU-32, flujo alterno). Las encuestas publicadas se cierran y los indicadores se
 *       calculan y guardan en ese momento.</li>
 *   <li><b>Criterio 2:</b> la memoria se consulta en cualquier momento posterior (listado por semestre, por familia
 *       de ediciones o por nombre, y detalle). No hay ninguna operación que la modifique y, desde el cierre, la
 *       información del evento tampoco se puede modificar (triggers del changeset hu28-004).</li>
 *   <li><b>Criterio 3:</b> eliminar información de un evento cerrado se rechaza y queda en la auditoría
 *       ({@link RegistroIntentosHistoricosService}). La única forma de eliminarlo es la eliminación con autorización
 *       especial: permiso {@code MEMORIA_HISTORICA_ELIMINAR} (no se asigna a ningún rol por defecto), el nombre exacto
 *       del evento como confirmación y un motivo, que quedan en la auditoría.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoriaHistoricaService {

    /** Entidad usada en la auditoría: las operaciones se registran sobre el evento. */
    static final String ENTIDAD = "eventos";

    /** Indicadores que se muestran en el listado de la memoria histórica. */
    static final List<String> INDICADORES_RESUMEN =
            List.of("INSCRITOS", "ASISTENTES", "ACTIVIDADES_REALIZADAS", "PROPUESTAS_RECIBIDAS");

    /** Estados de un evento publicado que todavía no pasó a cerrado. */
    private static final EnumSet<EstadoEvento> ESTADOS_PUBLICADOS =
            EnumSet.of(EstadoEvento.habilitado, EstadoEvento.en_ejecucion);

    private final MemoriaHistoricaRepository memoriaRepository;
    private final IndicadorRepository indicadorRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EncuestaService encuestaService;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 1: cierre formal
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Revisión previa al cierre formal: si el evento ya está en la memoria, si se puede cerrar y qué procesos quedan
     * pendientes (CU-32, flujo alterno 1.1).
     */
    @Transactional(readOnly = true)
    public EstadoCierreResponse revisarCierre(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        Optional<MemoriaHistorica> memoria = memoriaRepository.findByEventoId(eventoId);
        if (memoria.isPresent()) {
            return new EstadoCierreResponse(evento.getId(), evento.getNombre(), evento.getEstado(),
                    evento.getFechaFin(), true, memoria.get().getFechaConsolidacion(), false,
                    "El evento ya fue cerrado formalmente; su información está en la memoria histórica.", List.of());
        }
        String motivo = motivoNoCerrable(evento);
        return new EstadoCierreResponse(evento.getId(), evento.getNombre(), evento.getEstado(), evento.getFechaFin(),
                false, null, motivo == null, motivo, pendientes(eventoId));
    }

    /**
     * Cierra formalmente un evento finalizado y consolida su memoria histórica (Criterio 1).
     * <p>
     * Se puede cerrar un evento {@code cerrado} (pasó su fecha de fin) o uno publicado cuyo último día es hoy o ya
     * pasó; en ese caso pasa a {@code cerrado}. El evento se bloquea durante la operación, así que dos cierres
     * simultáneos no generan dos memorias. El orden importa: primero se guardan los cambios del evento (estado,
     * encuestas que se cierran, indicadores) y al final la memoria, porque desde ese momento la base de datos ya no
     * acepta cambios del evento.
     * </p>
     *
     * @param eventoId evento a cerrar.
     * @param request  confirmación de pendientes y resumen opcional.
     * @return memoria histórica consolidada.
     * @throws MemoriaHistoricaException     409 EVENTO_NO_FINALIZADO o MEMORIA_YA_CONSOLIDADA.
     * @throws CierreConPendientesException 409 CIERRE_CON_PENDIENTES si hay pendientes sin confirmar.
     */
    @Transactional
    public MemoriaHistoricaResponse cerrarFormalmente(Long eventoId, CierreFormalRequest request) {
        Evento evento = eventoRepository.findByIdParaProgramar(eventoId)
                .orElseThrow(() -> eventoNoEncontrado(eventoId));
        if (memoriaRepository.existsByEvento_Id(eventoId)) {
            throw new MemoriaHistoricaException(MemoriaHistoricaException.MEMORIA_YA_CONSOLIDADA, HttpStatus.CONFLICT,
                    "El evento «" + evento.getNombre() + "» ya fue cerrado formalmente; su información está en la "
                            + "memoria histórica.");
        }
        String motivo = motivoNoCerrable(evento);
        if (motivo != null) {
            throw new MemoriaHistoricaException(MemoriaHistoricaException.EVENTO_NO_FINALIZADO, HttpStatus.CONFLICT,
                    motivo);
        }

        List<PendienteCierreResponse> pendientes = pendientes(eventoId);
        boolean confirmado = request != null && request.pendientesConfirmados();
        if (!pendientes.isEmpty() && !confirmado) {
            throw new CierreConPendientesException("El evento «" + evento.getNombre() + "» tiene procesos pendientes. "
                    + "Revíselos o confirme el cierre formal para consolidar la memoria histórica de todas formas.",
                    pendientes);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario usuario = usuarioAutenticado();

        if (evento.getEstado() != EstadoEvento.cerrado) {
            EstadoEvento anterior = evento.getEstado();
            evento.setEstado(EstadoEvento.cerrado);
            eventoRepository.saveAndFlush(evento);
            auditoriaService.registrar(TipoOperacionAuditoria.EVENTO_CERRADO, ENTIDAD, evento.getId(),
                    DetalleAuditoria.de("nombre", evento.getNombre(), "estadoAnterior", anterior.name(),
                            "fechaFin", evento.getFechaFin().toString(), "cierreFormal", true),
                    usuario);
        }

        // Todo ocurre en esta misma transacción (EncuestaService.cerrar es REQUIRED y la auditoría MANDATORY): si algo
        // falla, se revierten también el cambio de estado, el cierre de las encuestas y los indicadores. La memoria va
        // al final porque, una vez guardada, los triggers ya no aceptan cambios del evento.
        // Encuestas publicadas: se cierran para que sus resultados queden definitivos en la memoria.
        List<Long> encuestas = memoriaRepository.idsEncuestasPublicadas(eventoId);
        for (Long encuestaId : encuestas) {
            encuestaService.cerrar(eventoId, encuestaId);
        }
        memoriaRepository.flush();

        int indicadores = memoriaRepository.insertarIndicadores(eventoId, ahora);
        String contenido = memoriaRepository.construirContenido(eventoId,
                DetalleAuditoria.aJson(pendientes.stream().map(MemoriaHistoricaService::comoMapa).toList()));

        MemoriaHistorica memoria = memoriaRepository.saveAndFlush(MemoriaHistorica.builder()
                .evento(evento)
                .fechaConsolidacion(ahora)
                .resumen(textoOpcional(request != null ? request.resumen() : null))
                .contenido(contenido)
                .cerradaPor(usuario)
                .build());

        auditoriaService.registrar(TipoOperacionAuditoria.MEMORIA_HISTORICA_CONSOLIDADA, ENTIDAD, evento.getId(),
                DetalleAuditoria.de(
                        "nombre", evento.getNombre(),
                        "memoriaId", memoria.getId(),
                        "pendientesConfirmados", pendientes.stream().map(MemoriaHistoricaService::comoMapa).toList(),
                        "encuestasCerradas", encuestas.size(),
                        "indicadores", indicadores),
                usuario);
        log.info("Evento {} cerrado formalmente: memoria {}, {} indicadores, {} encuestas cerradas, {} pendientes",
                evento.getId(), memoria.getId(), indicadores, encuestas.size(), pendientes.size());

        return aResponse(memoria, evento, usuario, indicadorRepository.findByEvento_IdOrderByIdAsc(eventoId));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 2: consulta
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Eventos de la memoria histórica.
     *
     * @param semestre        semestre exacto (opcional).
     * @param familiaEventoId cualquier evento de una familia de ediciones: devuelve las ediciones cerradas de esa
     *                        familia (opcional).
     * @param texto           parte del nombre del evento (opcional).
     */
    @Transactional(readOnly = true)
    public List<MemoriaHistoricaResumenResponse> listar(String semestre, Long familiaEventoId, String texto) {
        Long familiaId = null;
        if (familiaEventoId != null) {
            Evento evento = buscarEvento(familiaEventoId);
            familiaId = evento.getEventoBase() != null ? evento.getEventoBase().getId() : evento.getId();
        }
        String semestreFiltro = textoOpcional(semestre);
        String textoFiltro = textoOpcional(texto);
        String patron = textoFiltro == null ? null : "%" + textoFiltro.toLowerCase(Locale.ROOT) + "%";

        List<MemoriaHistorica> memorias = memoriaRepository.buscar(semestreFiltro, familiaId, patron);
        if (memorias.isEmpty()) {
            return List.of();
        }
        List<Long> eventoIds = memorias.stream().map(m -> m.getEvento().getId()).toList();
        Map<Long, List<IndicadorResponse>> indicadores = indicadorRepository
                .findByEvento_IdInAndCodigoInOrderByIdAsc(eventoIds, INDICADORES_RESUMEN).stream()
                .collect(Collectors.groupingBy(i -> i.getEvento().getId(), LinkedHashMap::new,
                        Collectors.mapping(IndicadorResponse::fromEntity, Collectors.toList())));

        return memorias.stream().map(m -> {
            Evento e = m.getEvento();
            return new MemoriaHistoricaResumenResponse(m.getId(), e.getId(), e.getNombre(),
                    e.getTipoEvento() != null ? e.getTipoEvento().getNombre() : null, e.getModalidad(),
                    e.getFechaInicio(), e.getFechaFin(), e.getSemestre(),
                    e.getEventoBase() != null ? e.getEventoBase().getId() : null,
                    m.getFechaConsolidacion(), nombre(m.getCerradaPor()),
                    indicadores.getOrDefault(e.getId(), List.of()));
        }).toList();
    }

    /**
     * Memoria histórica de un evento, solo consulta.
     *
     * @throws MemoriaHistoricaException 404 MEMORIA_NO_ENCONTRADA si el evento no se ha cerrado formalmente.
     */
    @Transactional(readOnly = true)
    public MemoriaHistoricaResponse obtener(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        MemoriaHistorica memoria = memoriaRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new MemoriaHistoricaException(MemoriaHistoricaException.MEMORIA_NO_ENCONTRADA,
                        HttpStatus.NOT_FOUND, "El evento «" + evento.getNombre() + "» todavía no tiene memoria "
                        + "histórica: se consolida con su cierre formal."));
        return aResponse(memoria, evento, memoria.getCerradaPor(),
                indicadorRepository.findByEvento_IdOrderByIdAsc(eventoId));
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Criterio 3: eliminación con autorización especial
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Elimina un evento cerrado formalmente, con su memoria histórica y toda su información (Criterio 3, autorización
     * especial). Exige el permiso {@code MEMORIA_HISTORICA_ELIMINAR} (en el controlador), el nombre exacto del evento y
     * un motivo. La auditoría guarda quién, cuándo, el motivo y los datos principales del evento eliminado.
     *
     * @throws MemoriaHistoricaException 409 MEMORIA_NO_ENCONTRADA si el evento no está en la memoria histórica,
     *                                   400 CONFIRMACION_INVALIDA si el nombre no coincide, 409
     *                                   ELIMINACION_NO_PERMITIDA si es el evento base de otras ediciones u otra
     *                                   información depende de él.
     */
    @Transactional
    public void eliminarConAutorizacionEspecial(Long eventoId, EliminacionHistoricaRequest request) {
        Evento evento = eventoRepository.findByIdParaProgramar(eventoId)
                .orElseThrow(() -> eventoNoEncontrado(eventoId));
        MemoriaHistorica memoria = memoriaRepository.findByEventoId(eventoId)
                .orElseThrow(() -> new MemoriaHistoricaException(MemoriaHistoricaException.MEMORIA_NO_ENCONTRADA,
                        HttpStatus.CONFLICT, "El evento «" + evento.getNombre() + "» no está en la memoria "
                        + "histórica; esta eliminación solo aplica a eventos cerrados formalmente."));

        String nombreConfirmado = request.nombreEvento() == null ? "" : request.nombreEvento().trim();
        if (!nombreConfirmado.equals(evento.getNombre().trim())) {
            throw new MemoriaHistoricaException(MemoriaHistoricaException.CONFIRMACION_INVALIDA,
                    HttpStatus.BAD_REQUEST, "El nombre escrito no coincide con el del evento. Escriba exactamente «"
                    + evento.getNombre() + "» para confirmar la eliminación.");
        }
        long ediciones = eventoRepository.countByEventoBase_Id(eventoId);
        if (ediciones > 0) {
            throw new MemoriaHistoricaException(MemoriaHistoricaException.ELIMINACION_NO_PERMITIDA,
                    HttpStatus.CONFLICT, "El evento «" + evento.getNombre() + "» es el evento base de " + ediciones
                    + " edición(es); eliminarlo rompería su historial.");
        }

        Map<String, Object> resumenIndicadores = new LinkedHashMap<>();
        for (Indicador indicador : indicadorRepository.findByEvento_IdOrderByIdAsc(eventoId)) {
            resumenIndicadores.put(indicador.getCodigo() != null ? indicador.getCodigo() : indicador.getNombre(),
                    indicador.getValor());
        }
        auditoriaService.registrar(TipoOperacionAuditoria.MEMORIA_HISTORICA_ELIMINADA, ENTIDAD, evento.getId(),
                DetalleAuditoria.de(
                        "nombre", evento.getNombre(),
                        "semestre", evento.getSemestre(),
                        "fechaInicio", evento.getFechaInicio(),
                        "fechaFin", evento.getFechaFin(),
                        "memoriaId", memoria.getId(),
                        "fechaConsolidacion", memoria.getFechaConsolidacion(),
                        "motivo", request.motivo().trim(),
                        "indicadores", resumenIndicadores));

        // La auditoría va en la misma transacción a propósito: si el borrado falla y se revierte, no debe quedar
        // registrado un evento «eliminado» que sigue existiendo.
        try {
            memoriaRepository.autorizarEliminacionHistorica(eventoId);
            memoriaRepository.eliminarAsistencias(eventoId);
            memoriaRepository.eliminarCertificados(eventoId);
            memoriaRepository.eliminarEncuestas(eventoId);
            memoriaRepository.eliminarActividades(eventoId);
            memoriaRepository.eliminarPropuestas(eventoId);
            memoriaRepository.eliminarEvento(eventoId);
            memoriaRepository.revocarEliminacionHistorica();
        } catch (DataIntegrityViolationException ex) {
            log.warn("No se pudo eliminar el evento {} de la memoria histórica: {}", eventoId, ex.getMessage());
            throw new MemoriaHistoricaException(MemoriaHistoricaException.ELIMINACION_NO_PERMITIDA,
                    HttpStatus.CONFLICT, "No se puede eliminar el evento «" + evento.getNombre() + "»: otra "
                    + "información del sistema depende de él (por ejemplo, salas usadas por otros eventos).");
        }
        log.warn("Evento {} ('{}') eliminado de la memoria histórica con autorización especial", eventoId,
                evento.getNombre());
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Por qué no se puede cerrar formalmente todavía; {@code null} si se puede. Se cierra un evento que ya finalizó:
     * en estado {@code cerrado}, o publicado con su fecha de fin hoy o en el pasado.
     */
    String motivoNoCerrable(Evento evento) {
        if (evento.getEstado() == EstadoEvento.cerrado) {
            return null;
        }
        if (evento.getEstado() == EstadoEvento.en_configuracion) {
            return "El evento «" + evento.getNombre() + "» está en configuración: todavía no se ha realizado. "
                    + "El cierre formal se hace cuando el evento finaliza.";
        }
        LocalDate hoy = LocalDate.now(clock);
        if (ESTADOS_PUBLICADOS.contains(evento.getEstado()) && !evento.getFechaFin().isAfter(hoy)) {
            return null;
        }
        return "El evento «" + evento.getNombre() + "» termina el " + evento.getFechaFin()
                + ". El cierre formal se hace a partir de su último día.";
    }

    /** Procesos pendientes del evento (CU-32, flujo alterno 1.1). */
    List<PendienteCierreResponse> pendientes(Long eventoId) {
        ConteoPendientes conteo = memoriaRepository.contarPendientes(eventoId);
        List<PendienteCierreResponse> pendientes = new ArrayList<>();
        if (conteo == null) {
            return pendientes;
        }
        agregar(pendientes, "EVALUACIONES_SIN_FINALIZAR", conteo.getEvaluacionesSinFinalizar(),
                "Asignaciones de evaluación pendientes o en proceso");
        agregar(pendientes, "PROPUESTAS_SIN_DECISION", conteo.getPropuestasSinDecision(),
                "Propuestas recibidas o en evaluación, sin clasificación aplicada");
        agregar(pendientes, "PROPUESTAS_CON_AJUSTES_PENDIENTES", conteo.getPropuestasConAjustesPendientes(),
                "Propuestas aprobadas con ajustes que no tienen versión definitiva");
        agregar(pendientes, "ACTIVIDADES_POR_PROGRAMAR", conteo.getActividadesPorProgramar(),
                "Actividades que quedaron sin programar");
        agregar(pendientes, "ENCUESTAS_ABIERTAS", conteo.getEncuestasAbiertas(),
                "Encuestas de satisfacción publicadas; se cerrarán con el cierre formal");
        agregar(pendientes, "CERTIFICADOS_SIN_GENERAR", conteo.getCertificadosSinGenerar(),
                "Participantes que cumplen los requisitos y aún no tienen su certificado (HU-26)");
        return pendientes;
    }

    private static void agregar(List<PendienteCierreResponse> lista, String codigo, Long cantidad, String descripcion) {
        if (cantidad != null && cantidad > 0) {
            lista.add(new PendienteCierreResponse(codigo, cantidad, descripcion));
        }
    }

    private static Map<String, Object> comoMapa(PendienteCierreResponse p) {
        return DetalleAuditoria.de("codigo", p.codigo(), "cantidad", p.cantidad(), "descripcion", p.descripcion());
    }

    private static MemoriaHistoricaResponse aResponse(MemoriaHistorica memoria, Evento evento, Usuario cerradaPor,
                                                      List<Indicador> indicadores) {
        return new MemoriaHistoricaResponse(memoria.getId(), evento.getId(), evento.getNombre(), evento.getSemestre(),
                memoria.getFechaConsolidacion(), cerradaPor != null ? cerradaPor.getId() : null, nombre(cerradaPor),
                memoria.getResumen(), memoria.getArchivoUrl(), false,
                indicadores.stream().map(IndicadorResponse::fromEntity).toList(),
                memoria.getContenido() != null ? memoria.getContenido() : "{}");
    }

    private static String nombre(Usuario usuario) {
        if (usuario == null || usuario.getPersona() == null) {
            return null;
        }
        return (usuario.getPersona().getNombres() + " " + usuario.getPersona().getApellidos()).trim();
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId).orElseThrow(() -> eventoNoEncontrado(eventoId));
    }

    private static RecursoNoEncontradoException eventoNoEncontrado(Long eventoId) {
        return new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId);
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName()).orElse(null);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
