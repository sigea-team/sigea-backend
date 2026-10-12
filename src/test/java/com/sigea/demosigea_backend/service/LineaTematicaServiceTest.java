package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.parametro.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.parametro.LineaTematicaResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDate;
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

@ExtendWith(MockitoExtension.class)
class LineaTematicaServiceTest {

    @Mock
    private LineaTematicaRepository lineaRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private PropuestaRepository propuestaRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private LineaTematicaService lineaService;

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas")
                .fechaInicio(LocalDate.of(2026, 10, 20)).fechaFin(LocalDate.of(2026, 10, 22))
                .estado(estado)
                .build();
    }

    private LineaTematica linea(Evento evento) {
        return LineaTematica.builder().id(7L).evento(evento).nombre("Ingeniería de software").build();
    }

    @Test
    @DisplayName("Criterio 1: crea la línea temática y registra auditoría")
    void crear_guardaYAudita() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(lineaRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "Ciencia de datos")).thenReturn(false);
        when(lineaRepository.saveAndFlush(any(LineaTematica.class))).thenAnswer(inv -> {
            LineaTematica l = inv.getArgument(0);
            l.setId(8L);
            return l;
        });

        LineaTematicaResponse response = lineaService.crear(1L,
                new LineaTematicaRequest(" Ciencia de datos ", "Analítica y visualización"));

        assertEquals(8L, response.id());
        assertEquals("Ciencia de datos", response.nombre());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.LINEA_TEMATICA_CREADA),
                eq(LineaTematicaService.ENTIDAD), eq(8L), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: nombre duplicado en el evento → rechazo")
    void crear_duplicado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(lineaRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "ingeniería de software")).thenReturn(true);

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> lineaService.crear(1L, new LineaTematicaRequest("ingeniería de software", null)));
        verify(lineaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: el índice original (evento_id, nombre) también se traduce a duplicado")
    void crear_indiceOriginal_traduceADuplicado() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(lineaRepository.saveAndFlush(any(LineaTematica.class))).thenThrow(new DataIntegrityViolationException("dup",
                new ConstraintViolationException("dup", new SQLException("dup", "23505"),
                        "\"lineas_tematicas_evento_id_nombre_idx\"")));

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> lineaService.crear(1L, new LineaTematicaRequest("Redes", null)));
    }

    @Test
    @DisplayName("Criterio 2: línea usada en propuestas y actividades → no se elimina, mensaje con el impacto")
    void eliminar_enUso_rechaza() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(lineaRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(linea(evento)));
        when(actividadRepository.countByLineaTematica_Id(7L)).thenReturn(1L);
        when(propuestaRepository.countByLineaTematica_Id(7L)).thenReturn(5L);

        ParametroEventoEnUsoException ex = assertThrows(ParametroEventoEnUsoException.class,
                () -> lineaService.eliminar(1L, 7L));

        assertTrue(ex.getMessage().contains("1 actividad(es) de la agenda y 5 propuesta(s)"));
        verify(lineaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Criterio 2: línea usada solo en propuestas también bloquea la eliminación")
    void consultarUso_soloPropuestas_noEliminable() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(lineaRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(linea(evento)));
        when(actividadRepository.countByLineaTematica_Id(7L)).thenReturn(0L);
        when(propuestaRepository.countByLineaTematica_Id(7L)).thenReturn(2L);

        UsoParametroResponse uso = lineaService.consultarUso(1L, 7L);

        assertFalse(uso.eliminable());
        assertEquals(2L, uso.propuestas());
    }

    @Test
    @DisplayName("Criterio 2: línea sin uso → se elimina y se audita")
    void eliminar_sinUso_elimina() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        LineaTematica linea = linea(evento);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(lineaRepository.findByIdAndEvento_Id(7L, 1L)).thenReturn(Optional.of(linea));

        lineaService.eliminar(1L, 7L);

        verify(lineaRepository).delete(linea);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.LINEA_TEMATICA_ELIMINADA),
                eq(LineaTematicaService.ENTIDAD), eq(7L), anyMap());
    }

    @Test
    @DisplayName("Con el evento cerrado no se puede actualizar")
    void actualizar_eventoCerrado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.cerrado)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> lineaService.actualizar(1L, 7L, new LineaTematicaRequest("Redes", null)));
    }
}
