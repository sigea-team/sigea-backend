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
import com.sigea.demosigea_backend.model.Rol;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.AfiliacionRepository;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ComiteEvaluadorRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.RolRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ComiteEvaluadorService} (HU-15, Criterios 3 y 4).
 */
@ExtendWith(MockitoExtension.class)
class ComiteEvaluadorServiceTest {

    @Mock private ComiteEvaluadorRepository comiteRepository;
    @Mock private ConvocatoriaRepository convocatoriaRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private AfiliacionRepository afiliacionRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private AsignacionEvaluacionRepository asignacionRepository;
    @Mock private AuditoriaService auditoriaService;

    private ComiteEvaluadorService service;
    private Convocatoria convocatoria;
    private Persona ana;

    @BeforeEach
    void setUp() {
        service = new ComiteEvaluadorService(comiteRepository, convocatoriaRepository, personaRepository,
                afiliacionRepository, usuarioRepository, rolRepository, asignacionRepository, auditoriaService);
        convocatoria = Convocatoria.builder().id(10L).titulo("Convocatoria de Ponencias 2026").build();
        ana = Persona.builder().id(21L).nombres("Ana").apellidos("Pérez").correo("ana.perez@ufps.edu.co")
                .numeroDocumento("1090123456").tipoDocumento("CC").build();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static EvaluadorRequest soloCorreo(String correo) {
        return new EvaluadorRequest(correo, "Ingeniería de software", null, null, null, null, null, null);
    }

    private static EvaluadorRequest personaNueva(String correo, String documento, Long afiliacionId) {
        return new EvaluadorRequest(correo, "Analítica de datos", "CC", documento, "Luis", "Gómez", "3001234567",
                afiliacionId);
    }

    private void convocatoriaExiste() {
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoria));
    }

    private void guardadoSimulado() {
        when(comiteRepository.save(any(ComiteEvaluador.class))).thenAnswer(inv -> {
            ComiteEvaluador ce = inv.getArgument(0);
            ce.setId(3L);
            return ce;
        });
    }

    private ComiteEvaluador miembro(Long id, Convocatoria conv) {
        return ComiteEvaluador.builder().id(id).convocatoria(conv).persona(ana)
                .areaExperticia("Ingeniería de software").build();
    }

    // ------------------------------------------------------------------
    // Criterio 3
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 3: registra a una persona existente con su área y le agrega el rol EVALUADOR")
    void registrar_personaExistente_conCuenta() {
        convocatoriaExiste();
        Usuario usuario = Usuario.builder().id(100L).persona(ana).build();
        Rol evaluador = Rol.builder().id(7L).nombre("EVALUADOR").build();
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "ana.perez@ufps.edu.co"))
                .thenReturn(false);
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(ana));
        guardadoSimulado();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.of(usuario));
        when(rolRepository.findByNombreIgnoreCase("EVALUADOR")).thenReturn(Optional.of(evaluador));

        EvaluadorResponse r = service.registrar(10L, soloCorreo("  Ana.Perez@UFPS.edu.co "));

        assertEquals(3L, r.comiteEvaluadorId());
        assertEquals(10L, r.convocatoriaId());
        assertEquals(21L, r.personaId());
        assertEquals("Ingeniería de software", r.areaExperticia());
        assertTrue(r.tieneCuenta());
        assertEquals(0L, r.propuestasAsignadas());
        assertTrue(usuario.getRoles().contains(evaluador));
        verify(usuarioRepository).save(usuario);
        verify(personaRepository, never()).save(any());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.EVALUADOR_REGISTRADO), eq("comites_evaluadores"),
                eq(3L), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: si el usuario ya tiene el rol EVALUADOR no se modifica")
    void registrar_usuarioYaEvaluador_noCambiaRoles() {
        convocatoriaExiste();
        Usuario usuario = Usuario.builder().id(100L).persona(ana).build();
        usuario.getRoles().add(Rol.builder().id(7L).nombre("Evaluador").build());
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "ana.perez@ufps.edu.co"))
                .thenReturn(false);
        when(personaRepository.findByCorreoIgnoreCase("ana.perez@ufps.edu.co")).thenReturn(Optional.of(ana));
        guardadoSimulado();
        when(usuarioRepository.findByPersona_Id(21L)).thenReturn(Optional.of(usuario));

        EvaluadorResponse r = service.registrar(10L, soloCorreo("ana.perez@ufps.edu.co"));

        assertTrue(r.tieneCuenta());
        verify(usuarioRepository, never()).save(any());
        verify(rolRepository, never()).findByNombreIgnoreCase(any());
    }

    @Test
    @DisplayName("Criterio 3: crea la persona cuando el correo no está registrado")
    void registrar_personaNueva_laCrea() {
        convocatoriaExiste();
        Afiliacion ufps = Afiliacion.builder().id(1L).nombreAfiliacion("UFPS").build();
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "luis@ufps.edu.co"))
                .thenReturn(false);
        when(personaRepository.findByCorreoIgnoreCase("luis@ufps.edu.co")).thenReturn(Optional.empty());
        when(personaRepository.existsByNumeroDocumento("1090999888")).thenReturn(false);
        when(afiliacionRepository.findById(1L)).thenReturn(Optional.of(ufps));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> {
            Persona p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });
        guardadoSimulado();
        when(usuarioRepository.findByPersona_Id(30L)).thenReturn(Optional.empty());

        EvaluadorResponse r = service.registrar(10L, personaNueva("Luis@UFPS.edu.co", "1090999888", 1L));

        ArgumentCaptor<Persona> captor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepository).save(captor.capture());
        assertEquals("luis@ufps.edu.co", captor.getValue().getCorreo());
        assertEquals("Luis", captor.getValue().getNombres());
        assertEquals(ufps, captor.getValue().getAfiliacion());
        assertEquals(30L, r.personaId());
        assertEquals("UFPS", r.afiliacion());
        assertFalse(r.tieneCuenta());
    }

    @Test
    @DisplayName("Persona nueva sin datos personales: rechaza e indica los campos que faltan")
    void registrar_personaNuevaSinDatos_rechaza() {
        convocatoriaExiste();
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "nadie@ufps.edu.co"))
                .thenReturn(false);
        when(personaRepository.findByCorreoIgnoreCase("nadie@ufps.edu.co")).thenReturn(Optional.empty());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.registrar(10L, soloCorreo("nadie@ufps.edu.co")));

        assertTrue(ex.getMessage().contains("numeroDocumento"));
        verify(comiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Persona nueva cuyo documento ya pertenece a otra persona: 409 DATOS_PERSONA_DUPLICADOS")
    void registrar_documentoDeOtraPersona_rechaza() {
        convocatoriaExiste();
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "luis@ufps.edu.co"))
                .thenReturn(false);
        when(personaRepository.findByCorreoIgnoreCase("luis@ufps.edu.co")).thenReturn(Optional.empty());
        when(personaRepository.existsByNumeroDocumento("1090123456")).thenReturn(true);

        DatosPersonaDuplicadosException ex = assertThrows(DatosPersonaDuplicadosException.class,
                () -> service.registrar(10L, personaNueva("luis@ufps.edu.co", "1090123456", null)));

        assertEquals("numeroDocumento", ex.getCampo());
        verify(comiteRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Criterio 4
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 4: un correo ya registrado en el comité se rechaza sin crear duplicados")
    void registrar_correoDuplicado_rechaza() {
        convocatoriaExiste();
        when(comiteRepository.existsByConvocatoria_IdAndPersona_CorreoIgnoreCase(10L, "ana.perez@ufps.edu.co"))
                .thenReturn(true);

        EvaluadorDuplicadoException ex = assertThrows(EvaluadorDuplicadoException.class,
                () -> service.registrar(10L, soloCorreo("ANA.PEREZ@ufps.edu.co")));

        assertTrue(ex.getMessage().contains("ana.perez@ufps.edu.co"));
        verify(comiteRepository, never()).save(any());
        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Convocatoria inexistente responde 404")
    void registrar_convocatoriaInexistente() {
        when(convocatoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.registrar(99L, soloCorreo("ana.perez@ufps.edu.co")));
    }

    // ------------------------------------------------------------------
    // Listar, actualizar y retirar
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Lista los evaluadores con su cuenta y sus propuestas asignadas")
    void listar_evaluadores() {
        convocatoriaExiste();
        Persona luis = Persona.builder().id(22L).nombres("Luis").apellidos("Gómez").correo("luis@ufps.edu.co").build();
        ComiteEvaluador m1 = miembro(3L, convocatoria);
        ComiteEvaluador m2 = ComiteEvaluador.builder().id(5L).convocatoria(convocatoria).persona(luis)
                .areaExperticia("Redes").build();
        when(comiteRepository.findByConvocatoriaConPersona(10L)).thenReturn(List.of(m1, m2));
        when(usuarioRepository.findPersonaIdsConCuenta(List.of(21L, 22L))).thenReturn(List.of(21L));
        when(asignacionRepository.contarPorEvaluadorDeConvocatoria(10L))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 2L}));

        List<EvaluadorResponse> lista = service.listar(10L);

        assertEquals(2, lista.size());
        assertTrue(lista.get(0).tieneCuenta());
        assertEquals(2L, lista.get(0).propuestasAsignadas());
        assertFalse(lista.get(1).tieneCuenta());
        assertEquals(0L, lista.get(1).propuestasAsignadas());
    }

    @Test
    @DisplayName("Actualiza el área de experticia del evaluador")
    void actualizar_area() {
        ComiteEvaluador m = miembro(3L, convocatoria);
        when(comiteRepository.findDetalleById(3L)).thenReturn(Optional.of(m));
        when(usuarioRepository.existsByPersona_Id(21L)).thenReturn(true);
        when(asignacionRepository.countByComiteEvaluador_Id(3L)).thenReturn(1L);

        EvaluadorResponse r = service.actualizar(10L, 3L, new ActualizarEvaluadorRequest(" Redes "));

        assertEquals("Redes", r.areaExperticia());
        assertEquals(1L, r.propuestasAsignadas());
        verify(comiteRepository).save(m);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.EVALUADOR_ACTUALIZADO), eq("comites_evaluadores"),
                eq(3L), anyMap());
    }

    @Test
    @DisplayName("Un evaluador de otra convocatoria responde 404")
    void actualizar_evaluadorDeOtraConvocatoria() {
        Convocatoria otra = Convocatoria.builder().id(11L).build();
        when(comiteRepository.findDetalleById(3L)).thenReturn(Optional.of(miembro(3L, otra)));

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizar(10L, 3L, new ActualizarEvaluadorRequest("Redes")));
        verify(comiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("No retira a un evaluador con propuestas asignadas")
    void retirar_conAsignaciones_rechaza() {
        when(comiteRepository.findDetalleById(3L)).thenReturn(Optional.of(miembro(3L, convocatoria)));
        when(asignacionRepository.countByComiteEvaluador_Id(3L)).thenReturn(2L);

        assertThrows(OperacionNoPermitidaException.class, () -> service.retirar(10L, 3L));
        verify(comiteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Retira a un evaluador sin propuestas asignadas")
    void retirar_sinAsignaciones() {
        ComiteEvaluador m = miembro(3L, convocatoria);
        when(comiteRepository.findDetalleById(3L)).thenReturn(Optional.of(m));
        when(asignacionRepository.countByComiteEvaluador_Id(3L)).thenReturn(0L);

        service.retirar(10L, 3L);

        verify(comiteRepository).delete(m);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.EVALUADOR_RETIRADO), eq("comites_evaluadores"),
                eq(3L), anyMap());
    }
}
