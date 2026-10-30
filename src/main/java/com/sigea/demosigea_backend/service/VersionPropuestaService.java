package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.propuesta.ActualizarPropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorResponse;
import com.sigea.demosigea_backend.dto.propuesta.AutorizacionEdicionResponse;
import com.sigea.demosigea_backend.dto.propuesta.EnvioPropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.VersionPropuestaResponse;
import com.sigea.demosigea_backend.exception.AlmacenamientoException;
import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.PropuestaBloqueadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.DocumentoPropuesta;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.DocumentoPropuestaRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import com.sigea.demosigea_backend.service.ValidadorDocumentosPropuesta.DocumentoValidado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Servicio de envío de propuestas con documentos y gestión de sus versiones (HU-14: RF19, RF20, RF21).
 * <ul>
 *   <li><b>Criterio 1:</b> registra la propuesta en una convocatoria activa, guarda sus documentos como
 *       versión 1 y confirma la recepción.</li>
 *   <li><b>Criterio 2:</b> rechaza el envío si la convocatoria cerró, si un archivo tiene formato o tamaño no
 *       permitido o si faltan campos o documentos obligatorios, indicando el motivo.</li>
 *   <li><b>Criterio 3:</b> cada modificación (incluido el reemplazo de un documento) crea una versión nueva;
 *       las anteriores no se modifican.</li>
 *   <li><b>Criterio 4:</b> el historial muestra todas las versiones con su fecha y el usuario que hizo el cambio.
 *       Lo consultan el administrador, los evaluadores asignados a la propuesta y sus autores.</li>
 *   <li><b>Criterio 5:</b> si la propuesta ya tiene versión definitiva (RF29), se bloquea su edición salvo
 *       autorización explícita del administrador, que es de un solo uso.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionPropuestaService {

    /** Autoridades que se consideran administrador/organizador para propuestas. */
    private static final Set<String> AUTORIDADES_ADMIN = Set.of("ROLE_ADMIN", "ADMIN", "CONVOCATORIAS_GESTIONAR");

    private final PropuestaService propuestaService;
    private final PropuestaRepository propuestaRepository;
    private final PropuestaAutorRepository propuestaAutorRepository;
    private final VersionPropuestaRepository versionPropuestaRepository;
    private final DocumentoPropuestaRepository documentoPropuestaRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final ConvocatoriaService convocatoriaService;
    private final LineaTematicaRepository lineaTematicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionEvaluacionRepository asignacionEvaluacionRepository;
    private final ValidadorDocumentosPropuesta validadorDocumentos;
    private final AlmacenamientoArchivos almacenamiento;
    private final AuditoriaService auditoriaService;

    /**
     * Archivo listo para descargar.
     *
     * @param recurso        Contenido del archivo.
     * @param nombreOriginal Nombre con el que se descarga.
     * @param tipoContenido  Tipo MIME.
     * @param tamanoBytes    Tamaño en bytes.
     */
    public record ArchivoDescarga(Resource recurso, String nombreOriginal, String tipoContenido, Long tamanoBytes) {
    }

    // ------------------------------------------------------------------
    // Criterios 1 y 2: envío de la propuesta con sus documentos
    // ------------------------------------------------------------------

    /**
     * Envía una propuesta a una convocatoria activa adjuntando sus documentos. Crea la versión 1.
     * <p>
     * Todas las validaciones (convocatoria abierta, documentos) se hacen antes de guardar nada.
     * Si la solicitud no trae autores, el usuario autenticado queda como autor principal.
     * </p>
     *
     * @param request  Datos de la propuesta (los mismos de HU-13).
     * @param archivos Documentos adjuntos (al menos uno).
     * @return Confirmación con la propuesta y la versión creada.
     */
    @Transactional
    public EnvioPropuestaResponse enviarPropuesta(PropuestaRequest request, List<MultipartFile> archivos) {
        Usuario usuario = usuarioAutenticado();

        Convocatoria convocatoria = convocatoriaRepository.findById(request.convocatoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + request.convocatoriaId()));
        convocatoriaService.validarRecepcionPropuestas(convocatoria.getId());

        List<DocumentoValidado> documentos = validadorDocumentos.validar(archivos, true);

        PropuestaResponse creada = propuestaService.crearPropuesta(conAutorPorDefecto(request, usuario));
        Propuesta propuesta = propuestaRepository.findById(creada.id())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la propuesta con ID: " + creada.id()));

        VersionPropuesta version = nuevaVersion(propuesta, 1, usuario, null);
        guardarDocumentosNuevos(version, propuesta.getId(), documentos, true);
        VersionPropuesta guardada = versionPropuestaRepository.save(version);

        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_VERSION_CREADA, "versiones_propuesta",
                guardada.getId(), Map.of(
                        "propuestaId", propuesta.getId(),
                        "numeroVersion", 1,
                        "documentos", guardada.getDocumentos().size()
                ));
        log.info("Propuesta ID {} recibida con {} documento(s) (versión 1)",
                propuesta.getId(), guardada.getDocumentos().size());

        return new EnvioPropuestaResponse(
                "La propuesta '" + propuesta.getTitulo() + "' fue recibida correctamente (versión 1).",
                creada,
                VersionPropuestaResponse.fromEntity(guardada));
    }

    // ------------------------------------------------------------------
    // Criterios 3 y 5: modificación de la propuesta (nueva versión)
    // ------------------------------------------------------------------

    /**
     * Modifica una propuesta y crea una versión nueva conservando las anteriores.
     *
     * @param propuestaId ID de la propuesta.
     * @param request     Cambios (puede ser {@code null} si solo se cambian documentos).
     * @param archivos    Documentos nuevos (opcional).
     * @return Confirmación con la propuesta actualizada y la versión creada.
     */
    @Transactional
    public EnvioPropuestaResponse crearNuevaVersion(Long propuestaId, ActualizarPropuestaRequest request,
                                                    List<MultipartFile> archivos) {
        Usuario usuario = usuarioAutenticado();
        Propuesta propuesta = propuestaRepository.findByIdParaActualizar(propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la propuesta con ID: " + propuestaId));

        verificarPuedeModificar(propuesta, usuario);
        boolean usaAutorizacion = verificarEstadoEditable(propuesta);

        ActualizarPropuestaRequest cambios = request != null ? request
                : new ActualizarPropuestaRequest(null, null, null, null, null, null);

        List<DocumentoValidado> nuevos = validadorDocumentos.validar(archivos, false);
        VersionPropuesta anterior = versionPropuestaRepository
                .findTopByPropuesta_IdOrderByNumeroVersionDesc(propuestaId).orElse(null);
        List<DocumentoPropuesta> conservados = documentosConservados(anterior, cambios.documentosConservados());

        if (conservados.isEmpty() && nuevos.isEmpty()) {
            throw new ArchivoInvalidoException("La nueva versión debe tener al menos un documento.",
                    Map.of("documentos", "Adjunte un documento o conserve alguno de la versión anterior."));
        }
        validadorDocumentos.validarCantidadTotal(conservados.size() + nuevos.size());

        boolean cambiaronDatos = aplicarCambios(propuesta, cambios);
        int documentosAnteriores = anterior != null && anterior.getDocumentos() != null ? anterior.getDocumentos().size() : 0;
        boolean cambiaronDocumentos = !nuevos.isEmpty() || anterior == null || conservados.size() != documentosAnteriores;
        if (!cambiaronDatos && !cambiaronDocumentos) {
            throw new SolicitudInvalidaException(
                    "No se detectaron cambios en la propuesta; no se creó una nueva versión.");
        }

        int numero = anterior != null ? anterior.getNumeroVersion() + 1 : 1;
        VersionPropuesta version = nuevaVersion(propuesta, numero, usuario, cambios.notaCambio());
        boolean principalEnNuevos = !nuevos.isEmpty();
        guardarDocumentosNuevos(version, propuestaId, nuevos, principalEnNuevos);
        copiarConservados(version, conservados, !principalEnNuevos);

        if (usaAutorizacion) {
            propuesta.setEdicionAutorizada(false);
        }
        propuestaRepository.save(propuesta);
        VersionPropuesta guardada = versionPropuestaRepository.save(version);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("propuestaId", propuestaId);
        detalle.put("numeroVersion", numero);
        detalle.put("documentosNuevos", nuevos.size());
        detalle.put("documentosConservados", conservados.size());
        detalle.put("datosModificados", cambiaronDatos);
        detalle.put("usoAutorizacionExplicita", usaAutorizacion);
        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_VERSION_CREADA, "versiones_propuesta",
                guardada.getId(), detalle);
        log.info("Propuesta ID {}: creada versión {} por usuario {}", propuestaId, numero, usuario.getId());

        return new EnvioPropuestaResponse(
                "Se registró la versión " + numero + " de la propuesta. Las versiones anteriores se conservan como evidencia.",
                PropuestaResponse.fromEntity(propuesta, autoresDe(propuestaId)),
                VersionPropuestaResponse.fromEntity(guardada));
    }

    // ------------------------------------------------------------------
    // Criterio 4: historial de versiones
    // ------------------------------------------------------------------

    /**
     * Lista todas las versiones de la propuesta con fecha y autor del cambio.
     *
     * @param propuestaId ID de la propuesta.
     * @return Versiones de la más antigua a la más reciente.
     */
    @Transactional(readOnly = true)
    public List<VersionPropuestaResponse> listarVersiones(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        verificarPuedeConsultar(propuesta, usuarioAutenticado());
        return versionPropuestaRepository.findByPropuesta_IdOrderByNumeroVersionAsc(propuestaId)
                .stream()
                .map(VersionPropuestaResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene una versión específica de la propuesta.
     *
     * @param propuestaId   ID de la propuesta.
     * @param numeroVersion Número de la versión.
     * @return Versión solicitada.
     */
    @Transactional(readOnly = true)
    public VersionPropuestaResponse obtenerVersion(Long propuestaId, Integer numeroVersion) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        verificarPuedeConsultar(propuesta, usuarioAutenticado());
        return versionPropuestaRepository.findByPropuesta_IdAndNumeroVersion(propuestaId, numeroVersion)
                .map(VersionPropuestaResponse::fromEntity)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "La propuesta ID %d no tiene una versión %d.", propuestaId, numeroVersion)));
    }

    /**
     * Prepara la descarga de un documento de cualquier versión de la propuesta.
     *
     * @param propuestaId ID de la propuesta.
     * @param documentoId ID del documento.
     * @return Archivo con su nombre y tipo.
     */
    @Transactional(readOnly = true)
    public ArchivoDescarga descargarDocumento(Long propuestaId, Long documentoId) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        verificarPuedeConsultar(propuesta, usuarioAutenticado());
        DocumentoPropuesta documento = documentoPropuestaRepository.findByIdAndVersion_Propuesta_Id(documentoId, propuestaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el documento ID %d en la propuesta ID %d.", documentoId, propuestaId)));
        return new ArchivoDescarga(
                almacenamiento.cargar(documento.getRutaAlmacenamiento()),
                documento.getNombreOriginal(),
                documento.getTipoContenido(),
                documento.getTamanoBytes());
    }

    // ------------------------------------------------------------------
    // Criterio 5: autorización explícita
    // ------------------------------------------------------------------

    /**
     * Concede una autorización de un solo uso para modificar una propuesta bloqueada.
     * La restricción por rol (administrador/organizador) se aplica en el controlador.
     *
     * @param propuestaId ID de la propuesta.
     * @param motivo      Justificación, queda en la auditoría.
     * @return Resultado de la autorización.
     */
    @Transactional
    public AutorizacionEdicionResponse autorizarEdicion(Long propuestaId, String motivo) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        if (propuesta.getEstado() == EstadoPropuesta.rechazada) {
            throw new OperacionNoPermitidaException(
                    "La propuesta fue rechazada; no se puede autorizar su modificación.");
        }
        propuesta.setEdicionAutorizada(true);
        propuestaRepository.save(propuesta);

        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_EDICION_AUTORIZADA, "propuestas",
                propuestaId, Map.of("motivo", motivo.trim()));
        log.info("Autorizada la edición de la propuesta ID {}", propuestaId);

        return new AutorizacionEdicionResponse(propuestaId, true, motivo.trim(),
                "Se autorizó una modificación de la propuesta. La autorización se consume al crear la siguiente versión.");
    }

    // ------------------------------------------------------------------
    // Reglas de acceso y de estado
    // ------------------------------------------------------------------

    /**
     * Pueden modificar la propuesta sus autores, quien la envió (creador de la versión 1) y el administrador.
     */
    private void verificarPuedeModificar(Propuesta propuesta, Usuario usuario) {
        if (esAdministrador() || esAutorOEmisor(propuesta, usuario)) {
            return;
        }
        throw new AccessDeniedException("Solo los autores de la propuesta pueden modificarla.");
    }

    /**
     * Pueden consultar el historial el administrador, los evaluadores asignados a la propuesta (RNF08)
     * y sus autores.
     */
    private void verificarPuedeConsultar(Propuesta propuesta, Usuario usuario) {
        if (esAdministrador() || esAutorOEmisor(propuesta, usuario)) {
            return;
        }
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        if (personaId != null && asignacionEvaluacionRepository
                .existsByPropuesta_IdAndComiteEvaluador_Persona_Id(propuesta.getId(), personaId)) {
            return;
        }
        throw new AccessDeniedException("No tiene acceso a las versiones de esta propuesta.");
    }

    private boolean esAutorOEmisor(Propuesta propuesta, Usuario usuario) {
        Long personaId = usuario.getPersona() != null ? usuario.getPersona().getId() : null;
        if (personaId != null && propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(propuesta.getId(), personaId)) {
            return true;
        }
        return versionPropuestaRepository.findFirstByPropuesta_IdOrderByNumeroVersionAsc(propuesta.getId())
                .map(VersionPropuesta::getCreadoPor)
                .map(creador -> Objects.equals(creador.getId(), usuario.getId()))
                .orElse(false);
    }

    /**
     * Verifica que la propuesta se pueda modificar según su estado.
     * <ul>
     *   <li>Rechazada: nunca.</li>
     *   <li>Con versión definitiva (Criterio 5), en evaluación o aprobada: solo con autorización explícita.</li>
     *   <li>Recibida: mientras la convocatoria esté abierta.</li>
     *   <li>Con ajustes solicitados: sí, aunque la convocatoria ya haya cerrado (CU-16).</li>
     * </ul>
     *
     * @return {@code true} si la modificación consume la autorización explícita.
     */
    private boolean verificarEstadoEditable(Propuesta propuesta) {
        boolean autorizada = Boolean.TRUE.equals(propuesta.getEdicionAutorizada());

        if (propuesta.getEstado() == EstadoPropuesta.rechazada) {
            throw new OperacionNoPermitidaException("La propuesta fue rechazada y no admite modificaciones.");
        }
        if (versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(propuesta.getId())) {
            if (!autorizada) {
                throw new PropuestaBloqueadaException(
                        "La propuesta ya fue aprobada en versión definitiva y no puede modificarse "
                                + "sin autorización explícita del administrador.");
            }
            return true;
        }
        if (autorizada) {
            return true;
        }
        switch (propuesta.getEstado()) {
            case recibida -> convocatoriaService.validarRecepcionPropuestas(propuesta.getConvocatoriaId());
            case ajustes -> {
                // Se permite subir la versión con los ajustes solicitados por el evaluador.
            }
            default -> throw new OperacionNoPermitidaException(String.format(
                    "La propuesta está en estado '%s' y no puede modificarse sin autorización explícita del administrador.",
                    propuesta.getEstado()));
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Construcción de versiones y documentos
    // ------------------------------------------------------------------

    private VersionPropuesta nuevaVersion(Propuesta propuesta, int numero, Usuario usuario, String nota) {
        return VersionPropuesta.builder()
                .propuesta(propuesta)
                .numeroVersion(numero)
                .creadoPor(usuario)
                .titulo(propuesta.getTitulo())
                .resumen(propuesta.getResumen())
                .palabrasClave(propuesta.getPalabrasClave())
                .lineaTematica(propuesta.getLineaTematica())
                .observaciones(nota != null && !nota.isBlank() ? nota.trim() : null)
                .esDefinitiva(false)
                .build();
    }

    /**
     * Guarda los archivos nuevos en el almacenamiento y los agrega a la versión. Si la transacción
     * falla después, los archivos guardados se eliminan para no dejar archivos huérfanos.
     */
    private void guardarDocumentosNuevos(VersionPropuesta version, Long propuestaId,
                                         List<DocumentoValidado> documentos, boolean primeroEsPrincipal) {
        List<String> rutasGuardadas = new ArrayList<>();
        limpiarSiFalla(rutasGuardadas);
        String carpeta = "propuestas/" + propuestaId + "/v" + version.getNumeroVersion();

        for (int i = 0; i < documentos.size(); i++) {
            DocumentoValidado doc = documentos.get(i);
            String ruta;
            try (InputStream contenido = doc.archivo().getInputStream()) {
                ruta = almacenamiento.guardar(carpeta, doc.extension(), contenido);
            } catch (IOException ex) {
                throw new AlmacenamientoException("No fue posible leer el archivo '" + doc.nombreOriginal() + "'.", ex);
            }
            rutasGuardadas.add(ruta);

            boolean principal = primeroEsPrincipal && i == 0;
            version.agregarDocumento(DocumentoPropuesta.builder()
                    .nombreOriginal(doc.nombreOriginal())
                    .rutaAlmacenamiento(ruta)
                    .tipoContenido(doc.tipoContenido())
                    .tamanoBytes(doc.archivo().getSize())
                    .principal(principal)
                    .build());
            if (principal) {
                version.setDocumentoUrl(ruta);
            }
        }
    }

    /**
     * Copia a la nueva versión los documentos conservados de la anterior (apuntan al mismo archivo).
     */
    private void copiarConservados(VersionPropuesta version, List<DocumentoPropuesta> conservados,
                                   boolean definirPrincipal) {
        DocumentoPropuesta principalAnterior = conservados.stream()
                .filter(d -> Boolean.TRUE.equals(d.getPrincipal()))
                .findFirst()
                .orElse(conservados.isEmpty() ? null : conservados.get(0));

        for (DocumentoPropuesta original : conservados) {
            boolean principal = definirPrincipal && original == principalAnterior;
            version.agregarDocumento(DocumentoPropuesta.builder()
                    .nombreOriginal(original.getNombreOriginal())
                    .rutaAlmacenamiento(original.getRutaAlmacenamiento())
                    .tipoContenido(original.getTipoContenido())
                    .tamanoBytes(original.getTamanoBytes())
                    .principal(principal)
                    .fechaCarga(original.getFechaCarga())
                    .build());
            if (principal) {
                version.setDocumentoUrl(original.getRutaAlmacenamiento());
            }
        }
    }

    /**
     * Determina qué documentos de la versión anterior pasan a la nueva.
     */
    private List<DocumentoPropuesta> documentosConservados(VersionPropuesta anterior, List<Long> idsConservados) {
        List<DocumentoPropuesta> actuales = anterior != null && anterior.getDocumentos() != null
                ? anterior.getDocumentos() : List.of();
        if (idsConservados == null) {
            return new ArrayList<>(actuales);
        }
        Set<Long> ids = new HashSet<>(idsConservados);
        Set<Long> idsActuales = new HashSet<>();
        actuales.forEach(d -> idsActuales.add(d.getId()));
        List<Long> desconocidos = ids.stream().filter(id -> !idsActuales.contains(id)).sorted().toList();
        if (!desconocidos.isEmpty()) {
            throw new SolicitudInvalidaException(
                    "Los documentos " + desconocidos + " no pertenecen a la versión actual de la propuesta.");
        }
        return actuales.stream().filter(d -> ids.contains(d.getId())).toList();
    }

    /**
     * Aplica a la propuesta los campos que llegaron en la solicitud.
     *
     * @return {@code true} si algún dato cambió.
     */
    private boolean aplicarCambios(Propuesta propuesta, ActualizarPropuestaRequest cambios) {
        boolean cambio = false;
        if (cambios.titulo() != null) {
            String titulo = cambios.titulo().trim();
            if (titulo.isEmpty()) {
                throw new SolicitudInvalidaException("El título de la propuesta no puede quedar vacío.");
            }
            if (!titulo.equals(propuesta.getTitulo())) {
                propuesta.setTitulo(titulo);
                cambio = true;
            }
        }
        if (cambios.resumen() != null) {
            String resumen = cambios.resumen().trim();
            if (!resumen.equals(Objects.toString(propuesta.getResumen(), ""))) {
                propuesta.setResumen(resumen.isEmpty() ? null : resumen);
                cambio = true;
            }
        }
        if (cambios.palabrasClave() != null) {
            String palabras = cambios.palabrasClave().trim();
            if (!palabras.equals(Objects.toString(propuesta.getPalabrasClave(), ""))) {
                propuesta.setPalabrasClave(palabras.isEmpty() ? null : palabras);
                cambio = true;
            }
        }
        if (cambios.lineaTematicaId() != null) {
            Long actual = propuesta.getLineaTematica() != null ? propuesta.getLineaTematica().getId() : null;
            if (!cambios.lineaTematicaId().equals(actual)) {
                LineaTematica linea = lineaTematicaRepository.findById(cambios.lineaTematicaId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "No se encontró la línea temática con ID: " + cambios.lineaTematicaId()));
                propuesta.setLineaTematica(linea);
                cambio = true;
            }
        }
        return cambio;
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    /**
     * Si la solicitud no trae autores, registra al usuario autenticado como autor principal.
     */
    private PropuestaRequest conAutorPorDefecto(PropuestaRequest request, Usuario usuario) {
        if (request.autores() != null && !request.autores().isEmpty()) {
            return request;
        }
        Persona persona = usuario.getPersona();
        if (persona == null) {
            throw new SolicitudInvalidaException("Debe indicar al menos un autor de la propuesta.");
        }
        AutorRequest autor = new AutorRequest(
                persona.getId(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getCorreo(),
                persona.getAfiliacion() != null ? persona.getAfiliacion().getNombreAfiliacion() : null,
                persona.getAfiliacion() != null ? persona.getAfiliacion().getId() : null,
                RolAutor.autor_principal,
                1,
                persona.getTipoDocumento(),
                persona.getNumeroDocumento(),
                persona.getTelefono());
        return new PropuestaRequest(request.convocatoriaId(), request.titulo(), request.resumen(),
                request.palabrasClave(), request.lineaTematicaId(), List.of(autor));
    }

    private List<AutorResponse> autoresDe(Long propuestaId) {
        return propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuestaId)
                .stream()
                .map(AutorResponse::fromEntity)
                .toList();
    }

    private Propuesta buscarPropuesta(Long id) {
        return propuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la propuesta con ID: " + id));
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Debe iniciar sesión para gestionar propuestas.");
        }
        return usuarioRepository.findByPersona_CorreoIgnoreCase(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("No se encontró el usuario autenticado."));
    }

    private boolean esAdministrador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority autoridad : auth.getAuthorities()) {
            if (AUTORIDADES_ADMIN.contains(autoridad.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registra la eliminación de los archivos guardados si la transacción termina en rollback.
     * La lista se llena después; al momento del rollback contiene todas las rutas guardadas.
     */
    private void limpiarSiFalla(List<String> rutas) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    rutas.forEach(almacenamiento::eliminar);
                }
            }
        });
    }
}
