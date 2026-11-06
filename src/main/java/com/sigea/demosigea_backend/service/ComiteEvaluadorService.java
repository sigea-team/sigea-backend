package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.comiteevaluador.ActualizarEvaluadorRequest;
import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorRequest;
import com.sigea.demosigea_backend.dto.comiteevaluador.EvaluadorResponse;
import com.sigea.demosigea_backend.exception.DatosPersonaDuplicadosException;
import com.sigea.demosigea_backend.exception.EvaluadorDuplicadoException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Afiliacion;
import com.sigea.demosigea_backend.model.ComiteEvaluador;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AfiliacionRepository;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ComiteEvaluadorRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registro del comité evaluador de una convocatoria (HU-15, RF23).
 * <ul>
 *   <li><b>Criterio 3</b>: registra a los evaluadores con su área de experticia y los deja disponibles para que
 *       HU-16 les asigne propuestas. Si la persona tiene cuenta de usuario, se le agrega el rol
 *       {@code EVALUADOR} para que pueda ingresar a calificar (HU-17).</li>
 *   <li><b>Criterio 4</b>: impide registrar dos veces el mismo correo en el comité de la convocatoria.</li>
 * </ul>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComiteEvaluadorService {

    /** Nombre del rol base de evaluador (changeset 17-hu02-rol-evaluador). */
    static final String ROL_EVALUADOR = "EVALUADOR";

    private final ComiteEvaluadorRepository comiteRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final PersonaRepository personaRepository;
    private final AfiliacionRepository afiliacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AsignacionEvaluacionRepository asignacionRepository;
    private final AuditoriaService auditoriaService;

    /**
     * Evaluadores del comité de la convocatoria, ordenados por apellidos.
     *
     * @param convocatoriaId ID de la convocatoria.
     * @return evaluadores con su cuenta y sus propuestas asignadas.
     */
    @Transactional(readOnly = true)
    public List<EvaluadorResponse> listar(Long convocatoriaId) {
        buscarConvocatoria(convocatoriaId);
        List<ComiteEvaluador> miembros = comiteRepository.findByConvocatoriaConPersona(convocatoriaId);
        if (miembros.isEmpty()) {
            return List.of();
        }

        List<Long> personaIds = miembros.stream().map(m -> m.getPersona().getId()).toList();
        Set<Long> conCuenta = new HashSet<>(usuarioRepository.findPersonaIdsConCuenta(personaIds));

        Map<Long, Long> asignadas = new HashMap<>();
        for (Object[] fila : asignacionRepository.contarPorEvaluadorDeConvocatoria(convocatoriaId)) {
            asignadas.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }

        List<EvaluadorResponse> respuesta = new ArrayList<>(miembros.size());
        for (ComiteEvaluador m : miembros) {
            respuesta.add(EvaluadorResponse.of(m, convocatoriaId, conCuenta.contains(m.getPersona().getId()),
                    asignadas.getOrDefault(m.getId(), 0L)));
        }
        return respuesta;
    }

    /**
     * HU-15, Criterios 3 y 4: registra un evaluador en el comité de la convocatoria.
     * <p>
     * La persona se busca por correo; si no existe se crea con los datos personales de la solicitud.
     * </p>
     *
     * @param convocatoriaId ID de la convocatoria.
     * @param request        correo, área de experticia y, para personas nuevas, sus datos.
     * @return el evaluador registrado.
     * @throws RecursoNoEncontradoException    si la convocatoria o la afiliación no existen.
     * @throws EvaluadorDuplicadoException     si el correo ya está en el comité (Criterio 4).
     * @throws SolicitudInvalidaException      si la persona es nueva y faltan sus datos.
     * @throws DatosPersonaDuplicadosException si el documento de la persona nueva ya pertenece a otra persona.
     */
    @Transactional
    public EvaluadorResponse registrar(Long convocatoriaId, EvaluadorRequest request) {
        Convocatoria convocatoria = buscarConvocatoria(convocatoriaId);
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);

        // Criterio 4: el correo no puede repetirse en el comité de la convocatoria
        if (comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(convocatoriaId, correo)) {
            throw new EvaluadorDuplicadoException(
                    "Ya existe un evaluador con el correo " + correo + " en el comité de esta convocatoria.");
        }

        Optional<Persona> existente = personaRepository.findByCorreoIgnoreCase(correo);
        boolean personaNueva = existente.isEmpty();
        Persona persona = existente.orElseGet(() -> crearPersona(correo, request));

        ComiteEvaluador miembro = comiteRepository.save(ComiteEvaluador.builder()
                .convocatoria(convocatoria)
                .persona(persona)
                .areaExperticia(request.areaExperticia().trim())
                .build());

        boolean tieneCuenta = asegurarRolEvaluador(persona);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("convocatoriaId", convocatoriaId);
        datos.put("personaId", persona.getId());
        datos.put("correo", persona.getCorreo());
        datos.put("areaExperticia", miembro.getAreaExperticia());
        datos.put("personaNueva", personaNueva);
        datos.put("tieneCuenta", tieneCuenta);
        auditoriaService.registrar(TipoOperacionAuditoria.EVALUADOR_REGISTRADO, "comites_evaluadores",
                miembro.getId(), datos);

        return EvaluadorResponse.of(miembro, convocatoriaId, tieneCuenta, 0);
    }

    /**
     * Cambia el área de experticia de un evaluador del comité.
     *
     * @param convocatoriaId    ID de la convocatoria.
     * @param comiteEvaluadorId ID del miembro del comité.
     * @param request           nueva área de experticia.
     * @return el evaluador actualizado.
     */
    @Transactional
    public EvaluadorResponse actualizar(Long convocatoriaId, Long comiteEvaluadorId,
                                        ActualizarEvaluadorRequest request) {
        ComiteEvaluador miembro = buscarMiembro(convocatoriaId, comiteEvaluadorId);
        String anterior = miembro.getAreaExperticia();
        miembro.setAreaExperticia(request.areaExperticia().trim());
        comiteRepository.save(miembro);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("convocatoriaId", convocatoriaId);
        datos.put("areaAnterior", anterior != null ? anterior : "");
        datos.put("areaNueva", miembro.getAreaExperticia());
        auditoriaService.registrar(TipoOperacionAuditoria.EVALUADOR_ACTUALIZADO, "comites_evaluadores",
                miembro.getId(), datos);

        boolean tieneCuenta = usuarioRepository.existsByPersona_Id(miembro.getPersona().getId());
        return EvaluadorResponse.of(miembro, convocatoriaId, tieneCuenta,
                asignacionRepository.countByComiteEvaluador_Id(miembro.getId()));
    }

    /**
     * Retira un evaluador del comité. Solo es posible si no tiene propuestas asignadas; si las tiene, primero
     * deben reasignarse (HU-16).
     *
     * @param convocatoriaId    ID de la convocatoria.
     * @param comiteEvaluadorId ID del miembro del comité.
     * @throws OperacionNoPermitidaException si el evaluador tiene propuestas asignadas.
     */
    @Transactional
    public void retirar(Long convocatoriaId, Long comiteEvaluadorId) {
        ComiteEvaluador miembro = buscarMiembro(convocatoriaId, comiteEvaluadorId);
        long asignadas = asignacionRepository.countByComiteEvaluador_Id(miembro.getId());
        if (asignadas > 0) {
            throw new OperacionNoPermitidaException("No se puede retirar al evaluador: tiene " + asignadas
                    + " propuesta(s) asignada(s). Reasígnelas a otro evaluador antes de retirarlo.");
        }
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("convocatoriaId", convocatoriaId);
        datos.put("personaId", miembro.getPersona().getId());
        datos.put("correo", miembro.getPersona().getCorreo());
        comiteRepository.delete(miembro);
        auditoriaService.registrar(TipoOperacionAuditoria.EVALUADOR_RETIRADO, "comites_evaluadores",
                comiteEvaluadorId, datos);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private Convocatoria buscarConvocatoria(Long convocatoriaId) {
        return convocatoriaRepository.findById(convocatoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la convocatoria con ID: " + convocatoriaId));
    }

    private ComiteEvaluador buscarMiembro(Long convocatoriaId, Long comiteEvaluadorId) {
        ComiteEvaluador miembro = comiteRepository.findDetalleById(comiteEvaluadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el evaluador con ID: " + comiteEvaluadorId));
        if (!miembro.getConvocatoria().getId().equals(convocatoriaId)) {
            throw new RecursoNoEncontradoException("El evaluador " + comiteEvaluadorId
                    + " no pertenece al comité de la convocatoria " + convocatoriaId + ".");
        }
        return miembro;
    }

    /**
     * Crea la persona del evaluador cuando su correo no está registrado.
     *
     * @param correo  correo normalizado.
     * @param request datos personales de la solicitud.
     * @return la persona creada.
     */
    private Persona crearPersona(String correo, EvaluadorRequest request) {
        List<String> faltantes = new ArrayList<>();
        if (vacio(request.tipoDocumento())) faltantes.add("tipoDocumento");
        if (vacio(request.numeroDocumento())) faltantes.add("numeroDocumento");
        if (vacio(request.nombres())) faltantes.add("nombres");
        if (vacio(request.apellidos())) faltantes.add("apellidos");
        if (!faltantes.isEmpty()) {
            throw new SolicitudInvalidaException("El correo " + correo + " no pertenece a ninguna persona registrada. "
                    + "Para registrarla como evaluadora complete: " + String.join(", ", faltantes) + ".");
        }

        String documento = request.numeroDocumento().trim();
        if (personaRepository.existsByNumeroDocumento(documento)) {
            throw new DatosPersonaDuplicadosException("numeroDocumento", "El documento " + documento
                    + " ya pertenece a otra persona registrada con un correo distinto.");
        }

        Afiliacion afiliacion = null;
        if (request.afiliacionId() != null) {
            afiliacion = afiliacionRepository.findById(request.afiliacionId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró la afiliación con ID: " + request.afiliacionId()));
        }

        return personaRepository.save(Persona.builder()
                .tipoDocumento(request.tipoDocumento().trim())
                .numeroDocumento(documento)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(correo)
                .telefono(vacio(request.telefono()) ? null : request.telefono().trim())
                .afiliacion(afiliacion)
                .build());
    }

    /**
     * Si la persona tiene cuenta, le agrega el rol {@code EVALUADOR} (si aún no lo tiene) para que pueda
     * ingresar a calificar las propuestas que le asignen.
     *
     * @param persona persona evaluadora.
     * @return {@code true} si la persona tiene cuenta de usuario.
     */
    private boolean asegurarRolEvaluador(Persona persona) {
        if (persona.getId() == null) {
            return false;
        }
        Optional<Usuario> usuario = usuarioRepository.findByPersona_Id(persona.getId());
        if (usuario.isEmpty()) {
            return false;
        }
        Usuario u = usuario.get();
        boolean yaLoTiene = u.getRoles().stream().anyMatch(r -> ROL_EVALUADOR.equalsIgnoreCase(r.getNombre()));
        if (!yaLoTiene) {
            rolRepository.findByNombreIgnoreCase(ROL_EVALUADOR).ifPresentOrElse(rol -> {
                u.getRoles().add(rol);
                usuarioRepository.save(u);
            }, () -> log.warn("No existe el rol {}; el evaluador {} no podrá ingresar a calificar.",
                    ROL_EVALUADOR, persona.getCorreo()));
        }
        return true;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
