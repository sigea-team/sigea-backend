package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.propuesta.ActualizarAutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Afiliacion;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.PropuestaAutor;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.AfiliacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio de negocio para la gestión de propuestas académicas y el registro
 * diferenciado de sus autores y ponentes (HU-13).
 * <ul>
 *   <li><b>Criterio 1:</b> Asocia los datos de autor o ponente a la propuesta académica enviada.</li>
 *   <li><b>Criterio 2:</b> Permite registrar múltiples autores por propuesta diferenciando su rol
 *       específico (autor principal, coautor, ponente).</li>
 *   <li><b>Criterio 3:</b> Exige los datos obligatorios (nombre, correo, filiación) rechazando la
 *       operación si alguno falta.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PropuestaService {

    private final PropuestaRepository propuestaRepository;
    private final PropuestaAutorRepository propuestaAutorRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final LineaTematicaRepository lineaTematicaRepository;
    private final PersonaRepository personaRepository;
    private final AfiliacionRepository afiliacionRepository;
    private final ConvocatoriaService convocatoriaService;
    private final AuditoriaService auditoriaService;

    // ------------------------------------------------------------------
    // Envío de Propuesta con Autores (Criterios 1, 2 y 3)
    // ------------------------------------------------------------------

    /**
     * Registra y envía una propuesta académica postulada a una convocatoria abierta,
     * asociando a cada autor o ponente con su rol específico y orden de autoría.
     *
     * @param request Datos de la propuesta y lista de autores postulantes.
     * @return Propuesta creada con la lista detallada de autores asociados.
     * @throws RecursoNoEncontradoException si la convocatoria o línea temática no existen.
     * @throws RecursoDuplicadoException si se intenta asociar la misma persona dos veces a la propuesta.
     */
    @Transactional
    public PropuestaResponse crearPropuesta(PropuestaRequest request) {
        log.info("Iniciando registro de propuesta académica para convocatoria ID: {}", request.convocatoriaId());

        Convocatoria convocatoria = convocatoriaRepository.findById(request.convocatoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + request.convocatoriaId()));

        // Valida que la convocatoria esté abierta y publicada para recepción
        convocatoriaService.validarRecepcionPropuestas(convocatoria.getId());

        LineaTematica lineaTematica = null;
        if (request.lineaTematicaId() != null) {
            lineaTematica = lineaTematicaRepository.findById(request.lineaTematicaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró la línea temática con ID: " + request.lineaTematicaId()));
        }

        Propuesta propuesta = Propuesta.builder()
                .convocatoria(convocatoria)
                .titulo(request.titulo().trim())
                .resumen(request.resumen() != null ? request.resumen().trim() : null)
                .palabrasClave(request.palabrasClave() != null ? request.palabrasClave().trim() : null)
                .lineaTematica(lineaTematica)
                .estado(EstadoPropuesta.recibida)
                .build();

        Propuesta propuestaGuardada = propuestaRepository.save(propuesta);
        log.info("Propuesta guardada preliminarmente con ID: {}", propuestaGuardada.getId());

        List<AutorResponse> autoresResponse = new ArrayList<>();
        if (request.autores() != null && !request.autores().isEmpty()) {
            Set<Long> personasRegistradas = new HashSet<>();
            List<Persona> personasResueltas = new ArrayList<>();

            for (AutorRequest autorReq : request.autores()) {
                Persona persona = resolverOCrearPersona(autorReq);

                if (!personasRegistradas.add(persona.getId())) {
                    throw new RecursoDuplicadoException(String.format(
                            "La persona '%s' (%s) está duplicada en la lista de autores de la propuesta.",
                            persona.getNombres(), persona.getCorreo()));
                }
                personasResueltas.add(persona);
            }

            int ordenSecuencial = 1;
            for (int i = 0; i < request.autores().size(); i++) {
                AutorRequest autorReq = request.autores().get(i);
                Persona persona = personasResueltas.get(i);

                int orden = autorReq.orden() != null ? autorReq.orden() : ordenSecuencial++;
                RolAutor rol = autorReq.rol();

                PropuestaAutor autorPropuesta = PropuestaAutor.builder()
                        .propuesta(propuestaGuardada)
                        .persona(persona)
                        .orden(orden)
                        .rol(rol)
                        .build();

                PropuestaAutor autorGuardado = propuestaAutorRepository.save(autorPropuesta);
                PropuestaAutor aRegistrar = autorGuardado != null ? autorGuardado : autorPropuesta;
                propuestaGuardada.agregarAutor(aRegistrar);
                autoresResponse.add(AutorResponse.fromEntity(aRegistrar));
            }
        }

        auditoriaService.registrar(TipoOperacionAuditoria.PROPUESTA_REGISTRADA, "propuestas",
                propuestaGuardada.getId(), Map.of(
                        "titulo", propuestaGuardada.getTitulo(),
                        "convocatoriaId", convocatoria.getId(),
                        "totalAutores", autoresResponse.size()
                ));

        log.info("Propuesta ID {} registrada exitosamente con {} autor(es)",
                propuestaGuardada.getId(), autoresResponse.size());

        return PropuestaResponse.fromEntity(propuestaGuardada, autoresResponse);
    }

    // ------------------------------------------------------------------
    // Gestión de Autores en Propuesta Existente (Criterios 1, 2 y 3)
    // ------------------------------------------------------------------

    /**
     * Vincula un autor o ponente a una propuesta académica existente.
     *
     * @param propuestaId ID de la propuesta a la que se asocia el autor.
     * @param request     Datos personales, filiación y rol específico del autor.
     * @return Autor vinculado a la propuesta.
     * @throws RecursoNoEncontradoException si la propuesta o persona especificada no existen.
     * @throws RecursoDuplicadoException si la persona ya está asociada a la propuesta.
     */
    @Transactional
    public AutorResponse agregarAutor(Long propuestaId, AutorRequest request) {
        log.info("Asociando autor a propuesta ID: {}", propuestaId);

        Propuesta propuesta = buscarPropuesta(propuestaId);
        Persona persona = resolverOCrearPersona(request);

        if (propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(propuestaId, persona.getId())) {
            throw new RecursoDuplicadoException(String.format(
                    "La persona '%s' (%s) ya se encuentra registrada como autor o ponente en esta propuesta.",
                    persona.getNombres(), persona.getCorreo()));
        }

        int orden = request.orden() != null
                ? request.orden()
                : (int) propuestaAutorRepository.countByPropuesta_Id(propuestaId) + 1;

        PropuestaAutor autor = PropuestaAutor.builder()
                .propuesta(propuesta)
                .persona(persona)
                .orden(orden)
                .rol(request.rol())
                .build();

        PropuestaAutor guardado = propuestaAutorRepository.save(autor);

        auditoriaService.registrar(TipoOperacionAuditoria.AUTOR_PROPUESTA_REGISTRADO, "propuestas_autores",
                propuesta.getId(), Map.of(
                        "propuestaId", propuesta.getId(),
                        "personaId", persona.getId(),
                        "persona", (persona.getNombres() + " " + persona.getApellidos()).trim(),
                        "rol", guardado.getRol().name(),
                        "orden", guardado.getOrden()
                ));

        log.info("Autor persona ID {} asociado con éxito a propuesta ID {} con rol {}",
                persona.getId(), propuestaId, guardado.getRol());

        return AutorResponse.fromEntity(guardado);
    }

    /**
     * Vincula en lote una lista de autores o ponentes a una propuesta existente.
     *
     * @param propuestaId ID de la propuesta.
     * @param requests    Lista de autores a asociar.
     * @return Lista de autores vinculados.
     */
    @Transactional
    public List<AutorResponse> agregarAutoresEnLote(Long propuestaId, List<AutorRequest> requests) {
        List<AutorResponse> resultados = new ArrayList<>();
        if (requests != null) {
            for (AutorRequest request : requests) {
                resultados.add(agregarAutor(propuestaId, request));
            }
        }
        return resultados;
    }

    /**
     * Lista los autores y ponentes de una propuesta académica ordenados por su orden de autoría.
     *
     * @param propuestaId ID de la propuesta.
     * @return Lista de autores y ponentes asociados.
     */
    @Transactional(readOnly = true)
    public List<AutorResponse> listarAutores(Long propuestaId) {
        buscarPropuesta(propuestaId);
        return propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(propuestaId)
                .stream()
                .map(AutorResponse::fromEntity)
                .toList();
    }

    /**
     * Obtiene el detalle completo de una propuesta con su lista de autores.
     *
     * @param propuestaId ID de la propuesta.
     * @return Propuesta con autores vinculados.
     */
    @Transactional(readOnly = true)
    public PropuestaResponse obtenerPropuesta(Long propuestaId) {
        Propuesta propuesta = buscarPropuesta(propuestaId);
        List<AutorResponse> autores = listarAutores(propuestaId);
        return PropuestaResponse.fromEntity(propuesta, autores);
    }

    /**
     * Lista todas las propuestas enviadas a una convocatoria específica.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return Lista de propuestas postuladas.
     */
    @Transactional(readOnly = true)
    public List<PropuestaResponse> listarPorConvocatoria(Long convocatoriaId) {
        convocatoriaRepository.findById(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + convocatoriaId));

        return propuestaRepository.findByConvocatoria_IdOrderByFechaEnvioDesc(convocatoriaId)
                .stream()
                .map(p -> {
                    List<AutorResponse> autores = propuestaAutorRepository
                            .findByPropuesta_IdOrderByOrdenAsc(p.getId())
                            .stream()
                            .map(AutorResponse::fromEntity)
                            .toList();
                    return PropuestaResponse.fromEntity(p, autores);
                })
                .toList();
    }

    /**
     * Actualiza el rol u orden de prelación de un autor en la propuesta (Criterio 2).
     *
     * @param propuestaId ID de la propuesta.
     * @param personaId   ID de la persona vinculada.
     * @param request     Nuevo rol u orden.
     * @return Autor con datos actualizados.
     */
    @Transactional
    public AutorResponse actualizarAutor(Long propuestaId, Long personaId, ActualizarAutorRequest request) {
        PropuestaAutor autor = propuestaAutorRepository.findByPropuesta_IdAndPersona_Id(propuestaId, personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el autor con persona ID %d en la propuesta ID %d", personaId, propuestaId)));

        autor.setRol(request.rol());
        if (request.orden() != null) {
            autor.setOrden(request.orden());
        }

        PropuestaAutor actualizado = propuestaAutorRepository.save(autor);
        log.info("Autor persona ID {} actualizado en propuesta ID {} a rol {}", personaId, propuestaId, actualizado.getRol());
        return AutorResponse.fromEntity(actualizado);
    }

    /**
     * Desvincula a un autor o ponente de una propuesta académica.
     *
     * @param propuestaId ID de la propuesta.
     * @param personaId   ID de la persona a desvincular.
     */
    @Transactional
    public void eliminarAutor(Long propuestaId, Long personaId) {
        PropuestaAutor autor = propuestaAutorRepository.findByPropuesta_IdAndPersona_Id(propuestaId, personaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                        "No se encontró el autor con persona ID %d en la propuesta ID %d", personaId, propuestaId)));

        propuestaAutorRepository.delete(autor);
        log.info("Autor persona ID {} desvinculado de propuesta ID {}", personaId, propuestaId);
    }

    // ------------------------------------------------------------------
    // Métodos Auxiliares para Resolución de Persona y Afiliación
    // ------------------------------------------------------------------

    private Propuesta buscarPropuesta(Long id) {
        return propuestaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la propuesta con ID: " + id));
    }

    /**
     * Resuelve una persona existente en el sistema por ID, correo o documento,
     * o la crea y persiste si se trata de un nuevo autor.
     *
     * @param request Datos del autor.
     * @return Entidad {@link Persona} gestionada.
     */
    private Persona resolverOCrearPersona(AutorRequest request) {
        if (request.personaId() != null) {
            return personaRepository.findById(request.personaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró la persona con ID: " + request.personaId()));
        }

        String correo = request.correo().trim().toLowerCase();
        Optional<Persona> porCorreo = personaRepository.findByCorreoIgnoreCase(correo);
        if (porCorreo.isPresent()) {
            Persona persona = porCorreo.get();
            if (persona.getAfiliacion() == null && request.filiacion() != null && !request.filiacion().isBlank()) {
                Afiliacion afil = resolverAfiliacion(request.filiacion(), request.afiliacionId());
                persona.setAfiliacion(afil);
                personaRepository.save(persona);
            }
            return persona;
        }

        if (request.numeroDocumento() != null && !request.numeroDocumento().isBlank()) {
            Optional<Persona> porDoc = personaRepository.findByNumeroDocumento(request.numeroDocumento().trim());
            if (porDoc.isPresent()) {
                Persona persona = porDoc.get();
                if (persona.getAfiliacion() == null && request.filiacion() != null && !request.filiacion().isBlank()) {
                    Afiliacion afil = resolverAfiliacion(request.filiacion(), request.afiliacionId());
                    persona.setAfiliacion(afil);
                    personaRepository.save(persona);
                }
                return persona;
            }
        }

        String nombre = request.nombre() != null ? request.nombre().trim() : "";
        String apellidos = request.apellidos() != null ? request.apellidos().trim() : "";
        if (apellidos.isBlank()) {
            int lastSpace = nombre.lastIndexOf(' ');
            if (lastSpace > 0) {
                apellidos = nombre.substring(lastSpace + 1).trim();
                nombre = nombre.substring(0, lastSpace).trim();
            } else {
                apellidos = nombre;
            }
        }

        String tipoDoc = request.tipoDocumento() != null && !request.tipoDocumento().isBlank()
                ? request.tipoDocumento().trim()
                : "CC";
        String numDoc = request.numeroDocumento() != null && !request.numeroDocumento().isBlank()
                ? request.numeroDocumento().trim()
                : "DOC-" + Math.abs(correo.hashCode()) + "-" + (System.currentTimeMillis() % 100000);

        Afiliacion afil = resolverAfiliacion(request.filiacion(), request.afiliacionId());

        Persona nueva = Persona.builder()
                .nombres(nombre)
                .apellidos(apellidos)
                .correo(correo)
                .tipoDocumento(tipoDoc)
                .numeroDocumento(numDoc)
                .telefono(request.telefono() != null ? request.telefono().trim() : null)
                .afiliacion(afil)
                .build();

        return personaRepository.save(nueva);
    }

    /**
     * Resuelve la afiliación institucional del autor desde el catálogo o la crea si no existe.
     *
     * @param filiacionTexto Nombre textual de la filiación.
     * @param afiliacionId   ID del catálogo si fue seleccionado.
     * @return Entidad {@link Afiliacion} resuelta.
     */
    private Afiliacion resolverAfiliacion(String filiacionTexto, Long afiliacionId) {
        if (afiliacionId != null) {
            return afiliacionRepository.findById(afiliacionId).orElse(null);
        }
        if (filiacionTexto != null && !filiacionTexto.isBlank()) {
            String texto = filiacionTexto.trim();
            Optional<Afiliacion> encontrada = afiliacionRepository.findByNombreAfiliacionIgnoreCase(texto);
            if (encontrada.isPresent()) {
                return encontrada.get();
            }
            try {
                Afiliacion nueva = Afiliacion.builder()
                        .nombreAfiliacion(texto)
                        .build();
                return afiliacionRepository.save(nueva);
            } catch (Exception ex) {
                return afiliacionRepository.findByNombreAfiliacionIgnoreCase(texto).orElse(null);
            }
        }
        return null;
    }
}
