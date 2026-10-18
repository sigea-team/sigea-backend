package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.propuesta.ActualizarAutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.RecursoDuplicadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Afiliacion;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropuestaServiceTest {

    @Mock
    private PropuestaRepository propuestaRepository;

    @Mock
    private PropuestaAutorRepository propuestaAutorRepository;

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private LineaTematicaRepository lineaTematicaRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private AfiliacionRepository afiliacionRepository;

    @Mock
    private ConvocatoriaService convocatoriaService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private PropuestaService propuestaService;

    // ------------------------------------------------------------------
    // Criterio 1: Asociar datos de autor/ponente al enviar una propuesta
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: Envío de propuesta asocia correctamente los datos del autor o ponente")
    void crearPropuesta_ConAutor_AsociaAutorAPropuesta() {
        Convocatoria convocatoria = Convocatoria.builder()
                .id(1L)
                .titulo("Convocatoria 2026")
                .estado(EstadoConvocatoria.publicada)
                .build();
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria));

        LineaTematica linea = LineaTematica.builder().id(2L).nombre("Inteligencia Artificial").build();
        when(lineaTematicaRepository.findById(2L)).thenReturn(Optional.of(linea));

        Afiliacion afil = Afiliacion.builder().id(10L).nombreAfiliacion("UFPS").build();
        when(afiliacionRepository.findByNombreAfiliacionIgnoreCase("UFPS")).thenReturn(Optional.of(afil));

        Persona personaGuardada = Persona.builder()
                .id(100L)
                .nombres("Carlos")
                .apellidos("Gómez")
                .correo("carlos@ufps.edu.co")
                .tipoDocumento("CC")
                .numeroDocumento("1098765432")
                .afiliacion(afil)
                .build();
        when(personaRepository.findByCorreoIgnoreCase("carlos@ufps.edu.co")).thenReturn(Optional.empty());
        when(personaRepository.save(any(Persona.class))).thenReturn(personaGuardada);

        Propuesta propuestaGuardada = Propuesta.builder()
                .id(50L)
                .convocatoria(convocatoria)
                .titulo("IA en la Educación")
                .lineaTematica(linea)
                .estado(EstadoPropuesta.recibida)
                .autores(new ArrayList<>())
                .build();
        when(propuestaRepository.save(any(Propuesta.class))).thenReturn(propuestaGuardada);

        PropuestaAutor paGuardado = PropuestaAutor.builder()
                .propuesta(propuestaGuardada)
                .persona(personaGuardada)
                .rol(RolAutor.autor_principal)
                .orden(1)
                .build();
        when(propuestaAutorRepository.save(any(PropuestaAutor.class))).thenReturn(paGuardado);

        AutorRequest autorReq = new AutorRequest(
                null, "Carlos", "Gómez", "carlos@ufps.edu.co", "UFPS", null,
                RolAutor.autor_principal, 1, "CC", "1098765432", null
        );
        PropuestaRequest propuestaReq = new PropuestaRequest(
                1L, "IA en la Educación", "Resumen...", "IA, Edu", 2L, List.of(autorReq)
        );

        PropuestaResponse respuesta = propuestaService.crearPropuesta(propuestaReq);

        assertNotNull(respuesta);
        assertEquals(50L, respuesta.id());
        assertEquals("IA en la Educación", respuesta.titulo());
        assertEquals(1, respuesta.autores().size());

        AutorResponse autorResp = respuesta.autores().get(0);
        assertEquals(100L, autorResp.personaId());
        assertEquals("carlos@ufps.edu.co", autorResp.correo());
        assertEquals(RolAutor.autor_principal, autorResp.rol());
        assertEquals(1, autorResp.orden());

        verify(convocatoriaService).validarRecepcionPropuestas(1L);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PROPUESTA_REGISTRADA), eq("propuestas"), eq(50L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: Agregar autor o ponente a propuesta existente lo vincula directamente")
    void agregarAutor_APropuestaExistente_AsociaCorrectamente() {
        Propuesta propuesta = Propuesta.builder()
                .id(10L)
                .titulo("Propuesta Robótica")
                .estado(EstadoPropuesta.recibida)
                .build();
        when(propuestaRepository.findById(10L)).thenReturn(Optional.of(propuesta));

        Persona persona = Persona.builder()
                .id(5L)
                .nombres("Laura")
                .apellidos("Martínez")
                .correo("laura@ufps.edu.co")
                .build();
        when(personaRepository.findById(5L)).thenReturn(Optional.of(persona));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(10L, 5L)).thenReturn(false);
        when(propuestaAutorRepository.countByPropuesta_Id(10L)).thenReturn(1L);

        PropuestaAutor guardado = PropuestaAutor.builder()
                .propuesta(propuesta)
                .persona(persona)
                .rol(RolAutor.ponente)
                .orden(2)
                .build();
        when(propuestaAutorRepository.save(any(PropuestaAutor.class))).thenReturn(guardado);

        AutorRequest autorReq = new AutorRequest(
                5L, "Laura", "Martínez", "laura@ufps.edu.co", "UFPS", null,
                RolAutor.ponente, null, null, null, null
        );

        AutorResponse respuesta = propuestaService.agregarAutor(10L, autorReq);

        assertNotNull(respuesta);
        assertEquals(5L, respuesta.personaId());
        assertEquals(RolAutor.ponente, respuesta.rol());
        assertEquals(2, respuesta.orden());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.AUTOR_PROPUESTA_REGISTRADO), eq("propuestas_autores"), eq(10L), anyMap());
    }

    // ------------------------------------------------------------------
    // Criterio 2: Múltiples autores guardados diferenciadamente con su rol
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: Una propuesta con varios autores guarda cada uno con su rol específico (autor principal, coautor, ponente)")
    void crearPropuesta_VariosAutoresRolesDiferenciados_GuardaDiferenciadamente() {
        Convocatoria convocatoria = Convocatoria.builder().id(1L).titulo("Convocatoria").build();
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria));

        Propuesta propuesta = Propuesta.builder().id(1L).titulo("Proyecto Multiautor").build();
        when(propuestaRepository.save(any(Propuesta.class))).thenReturn(propuesta);

        Persona p1 = Persona.builder().id(11L).nombres("Ana").correo("ana@ufps.edu.co").build();
        Persona p2 = Persona.builder().id(12L).nombres("Bernardo").correo("bernardo@ufps.edu.co").build();
        Persona p3 = Persona.builder().id(13L).nombres("Camila").correo("camila@ufps.edu.co").build();

        when(personaRepository.findByCorreoIgnoreCase("ana@ufps.edu.co")).thenReturn(Optional.of(p1));
        when(personaRepository.findByCorreoIgnoreCase("bernardo@ufps.edu.co")).thenReturn(Optional.of(p2));
        when(personaRepository.findByCorreoIgnoreCase("camila@ufps.edu.co")).thenReturn(Optional.of(p3));

        when(propuestaAutorRepository.save(any(PropuestaAutor.class))).thenAnswer(inv -> inv.getArgument(0));

        AutorRequest a1 = new AutorRequest(null, "Ana", null, "ana@ufps.edu.co", "UFPS", null, RolAutor.autor_principal, 1, null, null, null);
        AutorRequest a2 = new AutorRequest(null, "Bernardo", null, "bernardo@ufps.edu.co", "UFPS", null, RolAutor.coautor, 2, null, null, null);
        AutorRequest a3 = new AutorRequest(null, "Camila", null, "camila@ufps.edu.co", "UFPS", null, RolAutor.ponente, 3, null, null, null);

        PropuestaRequest request = new PropuestaRequest(1L, "Proyecto Multiautor", null, null, null, List.of(a1, a2, a3));

        PropuestaResponse respuesta = propuestaService.crearPropuesta(request);

        assertEquals(3, respuesta.autores().size());

        AutorResponse resp1 = respuesta.autores().get(0);
        assertEquals(11L, resp1.personaId());
        assertEquals(RolAutor.autor_principal, resp1.rol());
        assertEquals(1, resp1.orden());

        AutorResponse resp2 = respuesta.autores().get(1);
        assertEquals(12L, resp2.personaId());
        assertEquals(RolAutor.coautor, resp2.rol());
        assertEquals(2, resp2.orden());

        AutorResponse resp3 = respuesta.autores().get(2);
        assertEquals(13L, resp3.personaId());
        assertEquals(RolAutor.ponente, resp3.rol());
        assertEquals(3, resp3.orden());

        ArgumentCaptor<PropuestaAutor> captor = ArgumentCaptor.forClass(PropuestaAutor.class);
        verify(propuestaAutorRepository, times(3)).save(captor.capture());

        List<PropuestaAutor> guardados = captor.getAllValues();
        assertEquals(RolAutor.autor_principal, guardados.get(0).getRol());
        assertEquals(RolAutor.coautor, guardados.get(1).getRol());
        assertEquals(RolAutor.ponente, guardados.get(2).getRol());
    }

    @Test
    @DisplayName("Criterio 2: Actualizar el rol de un autor registrado en una propuesta")
    void actualizarAutor_CambiaRolExitosamente() {
        Propuesta propuesta = Propuesta.builder().id(20L).build();
        Persona persona = Persona.builder().id(7L).nombres("David").build();
        PropuestaAutor existente = PropuestaAutor.builder()
                .propuesta(propuesta)
                .persona(persona)
                .rol(RolAutor.coautor)
                .orden(2)
                .build();

        when(propuestaAutorRepository.findByPropuesta_IdAndPersona_Id(20L, 7L)).thenReturn(Optional.of(existente));
        when(propuestaAutorRepository.save(any(PropuestaAutor.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarAutorRequest updateReq = new ActualizarAutorRequest(RolAutor.ponente, 1);
        AutorResponse respuesta = propuestaService.actualizarAutor(20L, 7L, updateReq);

        assertEquals(RolAutor.ponente, respuesta.rol());
        assertEquals(1, respuesta.orden());
    }

    @Test
    @DisplayName("Criterio 2: Rechaza duplicación de la misma persona en la lista de autores al crear propuesta")
    void crearPropuesta_PersonaDuplicada_LanzaRecursoDuplicadoException() {
        Convocatoria convocatoria = Convocatoria.builder().id(1L).build();
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria));

        Propuesta propuesta = Propuesta.builder().id(1L).build();
        when(propuestaRepository.save(any(Propuesta.class))).thenReturn(propuesta);

        Persona persona = Persona.builder().id(50L).nombres("Elena").correo("elena@ufps.edu.co").build();
        when(personaRepository.findByCorreoIgnoreCase("elena@ufps.edu.co")).thenReturn(Optional.of(persona));

        AutorRequest a1 = new AutorRequest(null, "Elena", null, "elena@ufps.edu.co", "UFPS", null, RolAutor.autor_principal, 1, null, null, null);
        AutorRequest a2 = new AutorRequest(null, "Elena", null, "elena@ufps.edu.co", "UFPS", null, RolAutor.coautor, 2, null, null, null);

        PropuestaRequest req = new PropuestaRequest(1L, "Titulo", null, null, null, List.of(a1, a2));

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> propuestaService.crearPropuesta(req));
        assertTrue(ex.getMessage().contains("duplicada"));
    }

    @Test
    @DisplayName("Criterio 2: Rechaza asociar a propuesta un autor que ya está registrado en ella")
    void agregarAutor_YaRegistrado_LanzaRecursoDuplicadoException() {
        Propuesta propuesta = Propuesta.builder().id(10L).build();
        when(propuestaRepository.findById(10L)).thenReturn(Optional.of(propuesta));

        Persona persona = Persona.builder().id(5L).nombres("Pedro").correo("pedro@ufps.edu.co").build();
        when(personaRepository.findById(5L)).thenReturn(Optional.of(persona));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(10L, 5L)).thenReturn(true);

        AutorRequest req = new AutorRequest(5L, "Pedro", null, "pedro@ufps.edu.co", "UFPS", null, RolAutor.coautor, null, null, null, null);

        assertThrows(RecursoDuplicadoException.class,
                () -> propuestaService.agregarAutor(10L, req));

        verify(propuestaAutorRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Convocatoria cerrada / no encontrada
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: Impide postulación si la convocatoria no existe")
    void crearPropuesta_ConvocatoriaInexistente_LanzaRecursoNoEncontradoException() {
        when(convocatoriaRepository.findById(99L)).thenReturn(Optional.empty());

        PropuestaRequest req = new PropuestaRequest(99L, "Titulo", null, null, null, List.of());

        assertThrows(RecursoNoEncontradoException.class,
                () -> propuestaService.crearPropuesta(req));
    }

    @Test
    @DisplayName("Criterio 1: Impide postulación si la convocatoria está cerrada o no publicada")
    void crearPropuesta_ConvocatoriaCerrada_LanzaConvocatoriaCerradaException() {
        Convocatoria convocatoria = Convocatoria.builder().id(1L).titulo("Convocatoria").build();
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria));

        doThrow(new ConvocatoriaCerradaException("La fecha de cierre de la convocatoria ya se cumplió."))
                .when(convocatoriaService).validarRecepcionPropuestas(1L);

        PropuestaRequest req = new PropuestaRequest(1L, "Titulo", null, null, null, List.of());

        assertThrows(ConvocatoriaCerradaException.class,
                () -> propuestaService.crearPropuesta(req));

        verify(propuestaRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Consultas y eliminación de autores
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Listar autores de una propuesta devuelve los autores ordenados por orden")
    void listarAutores_PropuestaExistente_DevuelveListaOrdenada() {
        Propuesta propuesta = Propuesta.builder().id(10L).build();
        when(propuestaRepository.findById(10L)).thenReturn(Optional.of(propuesta));

        Persona p1 = Persona.builder().id(1L).nombres("Autor 1").build();
        Persona p2 = Persona.builder().id(2L).nombres("Autor 2").build();

        PropuestaAutor pa1 = PropuestaAutor.builder().propuesta(propuesta).persona(p1).orden(1).rol(RolAutor.autor_principal).build();
        PropuestaAutor pa2 = PropuestaAutor.builder().propuesta(propuesta).persona(p2).orden(2).rol(RolAutor.coautor).build();

        when(propuestaAutorRepository.findByPropuesta_IdOrderByOrdenAsc(10L)).thenReturn(List.of(pa1, pa2));

        List<AutorResponse> autores = propuestaService.listarAutores(10L);

        assertEquals(2, autores.size());
        assertEquals("Autor 1", autores.get(0).nombres());
        assertEquals("Autor 2", autores.get(1).nombres());
    }

    @Test
    @DisplayName("Eliminar autor de propuesta desvincula la relación")
    void eliminarAutor_Existente_EliminaCorrectamente() {
        PropuestaAutor pa = PropuestaAutor.builder().build();
        when(propuestaAutorRepository.findByPropuesta_IdAndPersona_Id(10L, 5L)).thenReturn(Optional.of(pa));

        propuestaService.eliminarAutor(10L, 5L);

        verify(propuestaAutorRepository).delete(pa);
    }
}
