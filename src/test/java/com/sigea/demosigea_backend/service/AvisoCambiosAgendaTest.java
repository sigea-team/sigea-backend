package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.AgendaPublicada;
import com.sigea.demosigea_backend.model.EstadoAgenda;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Sala;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoSala;
import com.sigea.demosigea_backend.repository.AgendaPublicadaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link AvisoCambiosAgenda} (HU-22, Criterio 5: notificar cambios de una agenda publicada).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AvisoCambiosAgendaTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2027, 3, 1, 10, 0);
    private static final Long EVENTO = 1L;
    private static final LocalDate DIA = LocalDate.of(2027, 3, 10);

    @Mock private AgendaPublicadaRepository agendaPublicadaRepository;
    @Mock private NotificacionService notificacionService;

    private AvisoCambiosAgenda aviso;
    private Evento evento;
    private Sala auditorio;
    private Sala salaB;
    private Persona ana;

    @BeforeEach
    void preparar() {
        aviso = new AvisoCambiosAgenda(agendaPublicadaRepository, notificacionService,
                Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));
        evento = Evento.builder().id(EVENTO).nombre("Simposio").estado(EstadoEvento.habilitado).build();
        auditorio = Sala.builder().id(10L).evento(evento).nombre("Auditorio").tipo(TipoSala.fisica).aforoMaximo(100).build();
        salaB = Sala.builder().id(11L).evento(evento).nombre("Sala B").tipo(TipoSala.fisica).aforoMaximo(50).build();
        ana = Persona.builder().id(20L).nombres("Ana").apellidos("Pérez").build();
        when(notificacionService.notificarAInscritosDeActividad(anyLong(), any(), anyString(), anyString()))
                .thenReturn(3);
    }

    private Actividad actividad(LocalDate dia, int desde, int hasta, Sala sala) {
        return Actividad.builder().id(5L).evento(evento).nombre("Taller IA").fecha(dia)
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).sala(sala).ponente(ana).build();
    }

    private void agendaPublicada(LocalDate... dias) {
        when(agendaPublicadaRepository.existsByEvento_IdAndEstado(EVENTO, EstadoAgenda.publicada)).thenReturn(true);
        for (LocalDate d : dias) {
            when(agendaPublicadaRepository.existsByEvento_IdAndFechaDiaAndEstado(EVENTO, d, EstadoAgenda.publicada))
                    .thenReturn(true);
            when(agendaPublicadaRepository.findByEvento_IdAndFechaDia(EVENTO, d)).thenReturn(Optional.of(
                    AgendaPublicada.builder().evento(evento).fechaDia(d).estado(EstadoAgenda.publicada).build()));
        }
    }

    @Test
    @DisplayName("si la agenda no está publicada no notifica a nadie")
    void agendaNoPublicada() {
        Actividad a = actividad(DIA, 9, 11, auditorio);
        var antes = AvisoCambiosAgenda.Programacion.de(a);
        a.setHoraInicio(LocalTime.of(14, 0));
        a.setHoraFin(LocalTime.of(16, 0));

        assertEquals(0, aviso.actividadModificada(a, antes));
        verify(notificacionService, never()).notificarAInscritosDeActividad(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Criterio 5: un cambio de horario y sala se notifica a los inscritos con lo de antes y lo de ahora")
    void notificaCambio() {
        agendaPublicada(DIA);
        Actividad a = actividad(DIA, 9, 11, auditorio);
        var antes = AvisoCambiosAgenda.Programacion.de(a);
        a.setHoraInicio(LocalTime.of(14, 0));
        a.setHoraFin(LocalTime.of(16, 0));
        a.setSala(salaB);

        assertEquals(3, aviso.actividadModificada(a, antes));

        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarAInscritosDeActividad(eq(5L),
                eq(TipoNotificacion.ACTIVIDAD_AGENDA_MODIFICADA), anyString(), mensaje.capture());
        assertTrue(mensaje.getValue().contains("09:00–11:00, sala Auditorio"), mensaje.getValue());
        assertTrue(mensaje.getValue().contains("14:00–16:00, sala Sala B"), mensaje.getValue());
        assertTrue(mensaje.getValue().contains("ponente Ana Pérez"), mensaje.getValue());
    }

    @Test
    @DisplayName("guardar sin cambiar horario, sala, ponente ni nombre no notifica")
    void sinCambiosNoNotifica() {
        agendaPublicada(DIA);
        Actividad a = actividad(DIA, 9, 11, auditorio);
        assertEquals(0, aviso.actividadModificada(a, AvisoCambiosAgenda.Programacion.de(a)));
        verify(notificacionService, never()).notificarAInscritosDeActividad(anyLong(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("si la actividad pasa a un día no publicado, ese día se publica y se notifica el cambio")
    void publicaDiaNuevo() {
        agendaPublicada(DIA);
        LocalDate otroDia = DIA.plusDays(1);
        when(agendaPublicadaRepository.findByEvento_IdAndFechaDia(EVENTO, otroDia)).thenReturn(Optional.empty());
        Actividad a = actividad(DIA, 9, 11, auditorio);
        var antes = AvisoCambiosAgenda.Programacion.de(a);
        a.setFecha(otroDia);

        aviso.actividadModificada(a, antes);

        ArgumentCaptor<AgendaPublicada> fila = ArgumentCaptor.forClass(AgendaPublicada.class);
        verify(agendaPublicadaRepository).save(fila.capture());
        assertEquals(otroDia, fila.getValue().getFechaDia());
        assertEquals(EstadoAgenda.publicada, fila.getValue().getEstado());
        assertEquals(AHORA, fila.getValue().getFechaPublicacion());
        verify(notificacionService).notificarAInscritosDeActividad(eq(5L),
                eq(TipoNotificacion.ACTIVIDAD_AGENDA_MODIFICADA), anyString(), anyString());
    }

    @Test
    @DisplayName("una actividad nueva en una agenda publicada no notifica (no tiene inscritos), pero queda visible")
    void actividadNueva() {
        agendaPublicada(DIA);
        Actividad a = actividad(DIA, 9, 11, auditorio);
        assertEquals(0, aviso.actividadModificada(a, AvisoCambiosAgenda.Programacion.NINGUNA));
        verify(notificacionService, never()).notificarAInscritosDeActividad(anyLong(), any(), anyString(), anyString());
        verify(agendaPublicadaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 5: retirar una actividad visible notifica que se retiró")
    void notificaRetiro() {
        agendaPublicada(DIA);
        Actividad a = actividad(DIA, 9, 11, auditorio);
        var antes = AvisoCambiosAgenda.Programacion.de(a);
        ActividadService.quitarHorario(a);

        assertEquals(3, aviso.actividadModificada(a, antes));
        verify(notificacionService).notificarAInscritosDeActividad(eq(5L),
                eq(TipoNotificacion.ACTIVIDAD_AGENDA_RETIRADA), anyString(), anyString());
    }

    @Test
    @DisplayName("no notifica cambios de actividades de días que los participantes todavía no veían")
    void diaNoPublicadoNoNotifica() {
        agendaPublicada(DIA.plusDays(2));
        Actividad a = actividad(DIA, 9, 11, auditorio);
        var antes = AvisoCambiosAgenda.Programacion.de(a);

        assertEquals(0, aviso.actividadRetirada(a, antes));
        verify(notificacionService, never()).notificarAInscritosDeActividad(anyLong(), any(), anyString(), anyString());
    }
}
