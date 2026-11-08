package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.sala.DisponibilidadHorariaRequest;
import com.sigea.demosigea_backend.dto.sala.OcupacionSalaResponse;
import com.sigea.demosigea_backend.dto.sala.SalaRequest;
import com.sigea.demosigea_backend.dto.sala.SalaResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.SalaDuplicadaException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.DisponibilidadSala;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.DisponibilidadSalaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalaServiceTest {

    @Mock private SalaRepository salaRepository;
    @Mock private DisponibilidadSalaRepository disponibilidadSalaRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private SalaService service;

    private Evento evento1;
    private Evento evento2;
    private Sala salaAuditorio;

    @BeforeEach
    void setUp() {
        evento1 = Evento.builder().id(1L).nombre("Congreso Internacional").estado(EstadoEvento.habilitado).build();
        evento2 = Evento.builder().id(2L).nombre("Seminario IA").estado(EstadoEvento.habilitado).build();

        salaAuditorio = Sala.builder()
                .id(10L)
                .nombre("Auditorio 5")
                .ubicacion("Edificio Fundadores")
                .tipo(TipoSala.fisica)
                .aforoMaximo(150)
                .permiteSimultaneidad(false)
                .eventos(new HashSet<>())
                .build();

        when(salaRepository.findById(10L)).thenReturn(Optional.of(salaAuditorio));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento1));
        when(eventoRepository.findById(2L)).thenReturn(Optional.of(evento2));
    }

    @Test
    @DisplayName("Crear sala desvinculada de eventos exitosamente")
    void crearGlobal_exitosa() {
        SalaRequest request = new SalaRequest(
                "Laboratorio 201", "Piso 2", TipoSala.fisica, 40,
                List.of(new DisponibilidadHorariaRequest(LocalDate.of(2026, 11, 10), LocalTime.of(8, 0), LocalTime.of(12, 0))),
                false
        );

        when(salaRepository.existsByNombreIgnoreCase("Laboratorio 201")).thenReturn(false);
        when(salaRepository.saveAndFlush(any(Sala.class))).thenAnswer(inv -> {
            Sala s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        SalaResponse response = service.crearGlobal(request);

        assertNotNull(response);
        assertEquals(20L, response.id());
        assertEquals("Laboratorio 201", response.nombre());
        assertNull(response.eventoId(), "La sala creada en el catálogo no debe tener eventoId obligatorio");
        verify(disponibilidadSalaRepository).save(any(DisponibilidadSala.class));
    }

    @Test
    @DisplayName("Rechazar creación de sala duplicada en el catálogo institucional")
    void crearGlobal_duplicada() {
        SalaRequest request = new SalaRequest("Auditorio 5", null, TipoSala.fisica, 100, null, false);
        when(salaRepository.existsByNombreIgnoreCase("Auditorio 5")).thenReturn(true);

        assertThrows(SalaDuplicadaException.class, () -> service.crearGlobal(request));
    }

    @Test
    @DisplayName("Vincular sala existente a un evento")
    void vincularSalaAEvento_exitosa() {
        SalaResponse response = service.vincularSalaAEvento(1L, 10L);

        assertNotNull(response);
        assertTrue(salaAuditorio.getEventos().contains(evento1));
        verify(salaRepository).save(salaAuditorio);
    }

    @Test
    @DisplayName("Desvincular sala de un evento si no tiene actividades")
    void desvincularSalaDeEvento_sinActividades() {
        salaAuditorio.getEventos().add(evento1);
        when(actividadRepository.findBySala_IdAndEvento_Id(10L, 1L)).thenReturn(Collections.emptyList());

        service.desvincularSalaDeEvento(1L, 10L);

        assertFalse(salaAuditorio.getEventos().contains(evento1));
        verify(salaRepository).save(salaAuditorio);
    }

    @Test
    @DisplayName("Impedir desvincular sala si tiene actividades en el evento")
    void desvincularSalaDeEvento_conActividades_lanzaExcepcion() {
        salaAuditorio.getEventos().add(evento1);
        Actividad act = Actividad.builder().id(99L).nombre("Charla").evento(evento1).sala(salaAuditorio).build();
        when(actividadRepository.findBySala_IdAndEvento_Id(10L, 1L)).thenReturn(List.of(act));

        assertThrows(OperacionNoPermitidaException.class, () -> service.desvincularSalaDeEvento(1L, 10L));
    }

    @Test
    @DisplayName("Consultar ocupaciones de la sala por actividades de eventos")
    void obtenerOcupacionesGlobal_retornaActividades() {
        Actividad act = Actividad.builder()
                .id(101L)
                .nombre("Taller Docker")
                .evento(evento1)
                .sala(salaAuditorio)
                .fecha(LocalDate.of(2026, 11, 15))
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(11, 0))
                .build();

        when(actividadRepository.findActividadesPorSala(10L)).thenReturn(List.of(act));

        List<OcupacionSalaResponse> ocupaciones = service.obtenerOcupacionesGlobal(10L, null);

        assertNotNull(ocupaciones);
        assertEquals(1, ocupaciones.size());
        assertEquals("Taller Docker", ocupaciones.get(0).actividadNombre());
        assertEquals("Congreso Internacional", ocupaciones.get(0).eventoNombre());
        assertEquals(LocalTime.of(9, 0), ocupaciones.get(0).horaInicio());
        assertEquals(LocalTime.of(11, 0), ocupaciones.get(0).horaFin());
    }
}
