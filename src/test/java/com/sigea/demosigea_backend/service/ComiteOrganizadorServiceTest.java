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
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComiteOrganizadorServiceTest {

    @Mock
    private ComiteOrganizadorRepository comiteRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ComiteOrganizadorService comiteService;

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas").tipo("Congreso")
                .fechaInicio(LocalDate.of(2026, 10, 20)).fechaFin(LocalDate.of(2026, 10, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    private Persona persona() {
        return Persona.builder()
                .id(12L).tipoDocumento("CC").numeroDocumento("1090123456")
                .nombres("Ana María").apellidos("Pérez Gómez").correo("ana.perez@ufps.edu.co")
                .build();
    }

    private ComiteOrganizador miembroActivo(Evento evento, Persona persona) {
        return ComiteOrganizador.builder()
                .id(5L).evento(evento).persona(persona).rolComite("Coordinador general")
                .fechaAsignacion(LocalDateTime.of(2026, 9, 20, 10, 0)).activo(true)
                .build();
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: asocia la persona al evento con el rol indicado y registra auditoría")
    void agregar_porPersonaId_asociaConRol() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        Persona persona = persona();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona));
        when(comiteRepository.existsByEvento_IdAndPersona_IdAndActivoTrue(1L, 12L)).thenReturn(false);
        when(comiteRepository.saveAndFlush(any(ComiteOrganizador.class))).thenAnswer(inv -> {
            ComiteOrganizador c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        MiembroComiteResponse response = comiteService.agregar(1L,
                new MiembroComiteRequest(12L, null, "  Coordinador general  "));

        ArgumentCaptor<ComiteOrganizador> captor = ArgumentCaptor.forClass(ComiteOrganizador.class);
        verify(comiteRepository).saveAndFlush(captor.capture());
        ComiteOrganizador guardado = captor.getValue();
        assertEquals(evento, guardado.getEvento());
        assertEquals(persona, guardado.getPersona());
        assertEquals("Coordinador general", guardado.getRolComite());
        assertTrue(guardado.isActivo());

        assertEquals(5L, response.id());
        assertEquals(1L, response.eventoId());
        assertEquals(12L, response.personaId());
        assertEquals("Ana María Pérez Gómez", response.nombreCompleto());
        assertEquals("Coordinador general", response.rolComite());
        assertTrue(response.activo());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.MIEMBRO_COMITE_AGREGADO),
                eq(ComiteOrganizadorService.ENTIDAD_COMITE), eq(5L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: permite identificar a la persona por número de documento")
    void agregar_porDocumento() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(personaRepository.findByNumeroDocumento("1090123456")).thenReturn(Optional.of(persona()));
        when(comiteRepository.saveAndFlush(any(ComiteOrganizador.class))).thenAnswer(inv -> inv.getArgument(0));

        MiembroComiteResponse response = comiteService.agregar(1L,
                new MiembroComiteRequest(null, " 1090123456 ", "Logística"));

        assertEquals(12L, response.personaId());
        assertEquals("Logística", response.rolComite());
        verify(personaRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Criterio 1: el evento debe existir")
    void agregar_eventoInexistente_404() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> comiteService.agregar(99L, new MiembroComiteRequest(12L, null, "Logística")));
        verify(comiteRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 1: la persona debe estar registrada")
    void agregar_personaInexistente_404() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(personaRepository.findById(12L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> comiteService.agregar(1L, new MiembroComiteRequest(12L, null, "Logística")));
        verify(comiteRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Un evento cerrado no admite cambios en su comité")
    void agregar_eventoCerrado_409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.cerrado)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> comiteService.agregar(1L, new MiembroComiteRequest(12L, null, "Logística")));
        verify(personaRepository, never()).findById(anyLong());
        verify(comiteRepository, never()).saveAndFlush(any());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: si la persona ya es miembro vigente, indica que existe y no duplica")
    void agregar_miembroDuplicado_409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        when(comiteRepository.existsByEvento_IdAndPersona_IdAndActivoTrue(1L, 12L)).thenReturn(true);

        MiembroComiteDuplicadoException ex = assertThrows(MiembroComiteDuplicadoException.class,
                () -> comiteService.agregar(1L, new MiembroComiteRequest(12L, null, "Otro rol")));

        assertTrue(ex.getMessage().contains("ya es miembro vigente"));
        verify(comiteRepository, never()).saveAndFlush(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 2: peticiones simultáneas detectadas por el índice único responden 409, no 500")
    void agregar_carreraDetectadaPorIndice_409() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        ConstraintViolationException violacion = new ConstraintViolationException("duplicado",
                new SQLException("duplicate key", ComiteOrganizadorService.SQLSTATE_VIOLACION_UNICIDAD),
                ComiteOrganizadorService.INDICE_MIEMBRO_ACTIVO);
        when(comiteRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup", violacion));

        assertThrows(MiembroComiteDuplicadoException.class,
                () -> comiteService.agregar(1L, new MiembroComiteRequest(12L, null, "Logística")));
    }

    @Test
    @DisplayName("Criterio 2: otras violaciones de integridad no se confunden con un duplicado")
    void esViolacionMiembroActivo_otraRestriccion_false() {
        ConstraintViolationException otra = new ConstraintViolationException("fk",
                new SQLException("fk", "23503"), "comite_organizador_persona_id_fkey");
        assertFalse(ComiteOrganizadorService.esViolacionMiembroActivo(new DataIntegrityViolationException("x", otra)));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: retira al miembro de la lista vigente conservando el registro")
    void retirar_conservaHistorial() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        ComiteOrganizador miembro = miembroActivo(evento, persona());
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(comiteRepository.findByIdAndEvento_Id(5L, 1L)).thenReturn(Optional.of(miembro));
        when(comiteRepository.save(miembro)).thenReturn(miembro);

        MiembroComiteResponse response = comiteService.retirar(1L, 5L);

        assertFalse(response.activo());
        assertNotNull(response.fechaRetiro());
        assertEquals("Coordinador general", response.rolComite());
        verify(comiteRepository, never()).delete(any());
        verify(comiteRepository, never()).deleteById(anyLong());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.MIEMBRO_COMITE_RETIRADO),
                eq(ComiteOrganizadorService.ENTIDAD_COMITE), eq(5L), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: no se puede retirar dos veces al mismo miembro")
    void retirar_yaRetirado_409() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        ComiteOrganizador miembro = miembroActivo(evento, persona());
        miembro.retirar();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(comiteRepository.findByIdAndEvento_Id(5L, 1L)).thenReturn(Optional.of(miembro));

        assertThrows(OperacionNoPermitidaException.class, () -> comiteService.retirar(1L, 5L));
        verify(comiteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: el miembro debe pertenecer al comité del evento indicado")
    void retirar_miembroDeOtroEvento_404() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(comiteRepository.findByIdAndEvento_Id(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> comiteService.retirar(1L, 5L));
    }

    @Test
    @DisplayName("Criterio 3: tras el retiro la persona puede volver a vincularse (nuevo registro)")
    void agregar_despuesDeRetiro_creaNuevaParticipacion() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(personaRepository.findById(12L)).thenReturn(Optional.of(persona()));
        // La participación anterior está retirada: ya no cuenta como vigente.
        when(comiteRepository.existsByEvento_IdAndPersona_IdAndActivoTrue(1L, 12L)).thenReturn(false);
        when(comiteRepository.saveAndFlush(any(ComiteOrganizador.class))).thenAnswer(inv -> {
            ComiteOrganizador c = inv.getArgument(0);
            c.setId(9L);
            return c;
        });

        MiembroComiteResponse response = comiteService.agregar(1L, new MiembroComiteRequest(12L, null, "Coordinador general"));

        assertEquals(9L, response.id());
        assertTrue(response.activo());
    }

    // ---------------- Consulta ----------------

    @Test
    @DisplayName("Listar: por defecto solo vigentes; con historial incluye retirados")
    void listar_vigentesEHistorial() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        ComiteOrganizador activo = miembroActivo(evento, persona());
        ComiteOrganizador retirado = miembroActivo(evento, persona());
        retirado.setId(4L);
        retirado.retirar();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(comiteRepository.findByEvento_IdAndActivoTrueOrderByFechaAsignacionAscIdAsc(1L)).thenReturn(List.of(activo));
        when(comiteRepository.findByEvento_IdOrderByActivoDescFechaAsignacionAscIdAsc(1L)).thenReturn(List.of(activo, retirado));

        assertEquals(1, comiteService.listar(1L, false).size());
        List<MiembroComiteResponse> historial = comiteService.listar(1L, true);
        assertEquals(2, historial.size());
        assertFalse(historial.get(1).activo());
    }
}
