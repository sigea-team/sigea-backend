package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.parametro.TipoActividadRequest;
import com.sigea.demosigea_backend.dto.parametro.TipoActividadResponse;
import com.sigea.demosigea_backend.dto.parametro.UsoParametroResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoActividad;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.TipoActividadRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoActividadServiceTest {

    @Mock
    private TipoActividadRepository tipoRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TipoActividadService tipoService;

    private Evento evento(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas").tipo("Congreso")
                .fechaInicio(LocalDate.of(2026, 10, 20)).fechaFin(LocalDate.of(2026, 10, 22))
                .semestre("2026-2").estado(estado)
                .build();
    }

    private TipoActividad tipo(Evento evento) {
        return TipoActividad.builder().id(4L).evento(evento).nombre("Taller").descripcion("Sesión práctica").build();
    }

    private static DataIntegrityViolationException violacion(String sqlState, String restriccion) {
        ConstraintViolationException causa = new ConstraintViolationException(
                "violación", new SQLException("violación", sqlState), restriccion);
        return new DataIntegrityViolationException("violación", causa);
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: crea el tipo con nombre normalizado y registra auditoría")
    void crear_guardaYAudita() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "Mesa redonda")).thenReturn(false);
        when(tipoRepository.saveAndFlush(any(TipoActividad.class))).thenAnswer(inv -> {
            TipoActividad t = inv.getArgument(0);
            t.setId(9L);
            return t;
        });

        TipoActividadResponse response = tipoService.crear(1L,
                new TipoActividadRequest("  Mesa   redonda ", "  Debate entre expertos  "));

        ArgumentCaptor<TipoActividad> captor = ArgumentCaptor.forClass(TipoActividad.class);
        verify(tipoRepository).saveAndFlush(captor.capture());
        assertEquals("Mesa redonda", captor.getValue().getNombre());
        assertEquals("Debate entre expertos", captor.getValue().getDescripcion());
        assertEquals(9L, response.id());
        assertEquals(1L, response.eventoId());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_ACTIVIDAD_CREADO),
                eq(TipoActividadService.ENTIDAD), eq(9L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: descripción vacía se guarda como null")
    void crear_descripcionVacia_seGuardaNull() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.habilitado)));
        when(tipoRepository.saveAndFlush(any(TipoActividad.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoActividadResponse response = tipoService.crear(1L, new TipoActividadRequest("Panel", "   "));

        assertNull(response.descripcion());
    }

    @Test
    @DisplayName("Listar: devuelve el catálogo del evento")
    void listar_devuelveCatalogo() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByEvento_IdOrderByNombreAscIdAsc(1L)).thenReturn(List.of(tipo(evento)));

        List<TipoActividadResponse> lista = tipoService.listar(1L);

        assertEquals(1, lista.size());
        assertEquals("Taller", lista.get(0).nombre());
    }

    @Test
    @DisplayName("Evento inexistente → RecursoNoEncontradoException")
    void listar_eventoInexistente() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> tipoService.listar(99L));
    }

    @Test
    @DisplayName("Tipo de otro evento → RecursoNoEncontradoException")
    void obtener_tipoDeOtroEvento() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> tipoService.obtener(1L, 4L));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: nombre ya existente (sin distinguir mayúsculas) → duplicado, no guarda")
    void crear_nombreDuplicado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(tipoRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "TALLER")).thenReturn(true);

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.crear(1L, new TipoActividadRequest("TALLER", null)));
        verify(tipoRepository, never()).saveAndFlush(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: carrera concurrente detectada por el índice único → duplicado (409, no 500)")
    void crear_indiceUnicoRechaza_traduceADuplicado() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(tipoRepository.saveAndFlush(any(TipoActividad.class)))
                .thenThrow(violacion("23505", "ux_tipos_actividad_evento_nombre_ci"));

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.crear(1L, new TipoActividadRequest("Taller", null)));
    }

    @Test
    @DisplayName("Criterio 3: otra violación de integridad no se disfraza de duplicado")
    void crear_otraViolacion_seRelanza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(EstadoEvento.en_configuracion)));
        when(tipoRepository.saveAndFlush(any(TipoActividad.class)))
                .thenThrow(violacion("23502", "tipos_actividad_nombre_not_null"));

        assertThrows(DataIntegrityViolationException.class,
                () -> tipoService.crear(1L, new TipoActividadRequest("Taller", null)));
    }

    @Test
    @DisplayName("Criterio 3: al renombrar, un nombre usado por otro tipo → duplicado")
    void actualizar_nombreDeOtroTipo_rechaza() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo(evento)));
        when(tipoRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(1L, "Conferencia", 4L)).thenReturn(true);

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.actualizar(1L, 4L, new TipoActividadRequest("Conferencia", null)));
        verify(tipoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Actualizar: conservar el mismo nombre no es duplicado y se audita antes/después")
    void actualizar_mismoNombre_permitido() {
        Evento evento = evento(EstadoEvento.habilitado);
        TipoActividad tipo = tipo(evento);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo));
        when(tipoRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(1L, "taller", 4L)).thenReturn(false);
        when(tipoRepository.saveAndFlush(tipo)).thenReturn(tipo);

        TipoActividadResponse response = tipoService.actualizar(1L, 4L,
                new TipoActividadRequest("taller", "Sesión práctica con cupo"));

        assertEquals("taller", response.nombre());
        assertEquals("Sesión práctica con cupo", response.descripcion());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_ACTIVIDAD_ACTUALIZADO),
                eq(TipoActividadService.ENTIDAD), eq(4L), anyMap());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: tipo en uso por actividades → se impide la eliminación e informa el impacto")
    void eliminar_enUso_rechaza() {
        Evento evento = evento(EstadoEvento.habilitado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo(evento)));
        when(actividadRepository.countByTipoActividad_Id(4L)).thenReturn(3L);

        ParametroEventoEnUsoException ex = assertThrows(ParametroEventoEnUsoException.class,
                () -> tipoService.eliminar(1L, 4L));

        assertTrue(ex.getMessage().contains("3 actividad(es)"));
        verify(tipoRepository, never()).delete(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 2: tipo sin uso → se elimina y se audita")
    void eliminar_sinUso_elimina() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        TipoActividad tipo = tipo(evento);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo));
        when(actividadRepository.countByTipoActividad_Id(4L)).thenReturn(0L);

        tipoService.eliminar(1L, 4L);

        verify(tipoRepository).delete(tipo);
        verify(tipoRepository).flush();
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_ACTIVIDAD_ELIMINADO),
                eq(TipoActividadService.ENTIDAD), eq(4L), anyMap());
    }

    @Test
    @DisplayName("Criterio 2: si la FK lo rechaza al borrar (uso concurrente) → en uso (409, no 500)")
    void eliminar_llaveForaneaRechaza_traduceAEnUso() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo(evento)));
        when(actividadRepository.countByTipoActividad_Id(4L)).thenReturn(0L);
        doThrow(violacion("23503", "actividades_tipo_actividad_id_fkey")).when(tipoRepository).flush();

        assertThrows(ParametroEventoEnUsoException.class, () -> tipoService.eliminar(1L, 4L));
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 2: consultar uso permite advertir del impacto antes de eliminar")
    void consultarUso_informaImpacto() {
        Evento evento = evento(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(tipoRepository.findByIdAndEvento_Id(4L, 1L)).thenReturn(Optional.of(tipo(evento)));
        when(actividadRepository.countByTipoActividad_Id(4L)).thenReturn(2L);

        UsoParametroResponse uso = tipoService.consultarUso(1L, 4L);

        assertEquals(2L, uso.actividades());
        assertEquals(0L, uso.propuestas());
        assertFalse(uso.eliminable());
    }

    // ---------------- Regla de estado ----------------

    @ParameterizedTest
    @EnumSource(value = EstadoEvento.class, names = {"en_ejecucion", "cerrado"})
    @DisplayName("Con el evento en ejecución o cerrado el catálogo no se modifica")
    void crear_eventoNoModificable_rechaza(EstadoEvento estado) {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento(estado)));

        assertThrows(OperacionNoPermitidaException.class,
                () -> tipoService.crear(1L, new TipoActividadRequest("Taller", null)));
        assertThrows(OperacionNoPermitidaException.class, () -> tipoService.eliminar(1L, 4L));
        verify(tipoRepository, never()).saveAndFlush(any());
        verify(tipoRepository, never()).delete(any());
    }
}
