package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaRequest;
import com.sigea.demosigea_backend.dto.lineatematica.LineaTematicaResponse;
import com.sigea.demosigea_backend.exception.LineaTematicaDuplicadaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.LineaTematica;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private LineaTematicaRepository lineaTematicaRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private LineaTematicaService lineaTematicaService;

    private Evento evento() {
        return Evento.builder()
                .id(1L)
                .nombre("Congreso Internacional de Sistemas")
                .estado(EstadoEvento.en_configuracion)
                .fechaInicio(LocalDate.of(2026, 10, 20))
                .fechaFin(LocalDate.of(2026, 10, 22))
                .build();
    }

    private LineaTematica linea(Evento evento) {
        return LineaTematica.builder()
                .id(10L)
                .evento(evento)
                .nombre("Inteligencia Artificial")
                .descripcion("Modelos generativos y aprendizaje profundo")
                .build();
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: guarda la línea temática en el evento y registra auditoría")
    void crear_datosValidos_guardaYRegistraAuditoria() {
        Evento e = evento();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));
        when(lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "Inteligencia Artificial")).thenReturn(false);
        when(lineaTematicaRepository.save(any(LineaTematica.class))).thenAnswer(inv -> {
            LineaTematica lt = inv.getArgument(0);
            lt.setId(10L);
            return lt;
        });

        LineaTematicaRequest request = new LineaTematicaRequest(1L, " Inteligencia Artificial ", " Modelos generativos ");
        LineaTematicaResponse response = lineaTematicaService.crear(1L, request);

        ArgumentCaptor<LineaTematica> captor = ArgumentCaptor.forClass(LineaTematica.class);
        verify(lineaTematicaRepository).save(captor.capture());
        LineaTematica guardada = captor.getValue();

        assertEquals("Inteligencia Artificial", guardada.getNombre());
        assertEquals("Modelos generativos", guardada.getDescripcion());
        assertEquals(e, guardada.getEvento());

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.eventoId());
        assertEquals("Inteligencia Artificial", response.nombre());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.LINEA_TEMATICA_CREADA),
                eq(LineaTematicaService.ENTIDAD_LINEA_TEMATICA), eq(10L), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: evento no encontrado lanza 404 RecursoNoEncontradoException")
    void crear_eventoNoEncontrado_lanzaException() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        LineaTematicaRequest request = new LineaTematicaRequest(99L, "Ciberseguridad", "Seguridad informática");
        assertThrows(RecursoNoEncontradoException.class, () -> lineaTematicaService.crear(99L, request));

        verify(lineaTematicaRepository, never()).save(any());
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: rechaza el registro si ya existe una línea temática con el mismo nombre en el evento (409)")
    void crear_nombreDuplicadoEnMismoEvento_lanzaLineaTematicaDuplicadaException() {
        Evento e = evento();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));
        when(lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCase(1L, "Inteligencia Artificial")).thenReturn(true);

        LineaTematicaRequest request = new LineaTematicaRequest(1L, "Inteligencia Artificial", "Descripción");

        LineaTematicaDuplicadaException ex = assertThrows(LineaTematicaDuplicadaException.class,
                () -> lineaTematicaService.crear(1L, request));

        assertTrue(ex.getMessage().contains("Ya existe una línea temática con el nombre 'Inteligencia Artificial'"));
        verify(lineaTematicaRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), any(), any(), any());
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: actualiza nombre y descripción sin modificar ID ni afectar registros históricos")
    void actualizar_datosValidos_actualizaCorrectamente() {
        Evento e = evento();
        LineaTematica lt = linea(e);
        when(lineaTematicaRepository.findById(10L)).thenReturn(Optional.of(lt));
        when(lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(1L, "IA y Robótica", 10L)).thenReturn(false);
        when(lineaTematicaRepository.save(any(LineaTematica.class))).thenAnswer(inv -> inv.getArgument(0));

        LineaTematicaRequest request = new LineaTematicaRequest(1L, " IA y Robótica ", " Nuevos alcances ");
        LineaTematicaResponse response = lineaTematicaService.actualizar(10L, request);

        assertEquals(10L, response.id());
        assertEquals("IA y Robótica", response.nombre());
        assertEquals("Nuevos alcances", response.descripcion());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.LINEA_TEMATICA_ACTUALIZADA),
                eq(LineaTematicaService.ENTIDAD_LINEA_TEMATICA), eq(10L), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: rechaza la actualización si el nuevo nombre choca con otra línea del mismo evento")
    void actualizar_nombreExistenteEnOtraLinea_lanzaLineaTematicaDuplicadaException() {
        Evento e = evento();
        LineaTematica lt = linea(e);
        when(lineaTematicaRepository.findById(10L)).thenReturn(Optional.of(lt));
        when(lineaTematicaRepository.existsByEvento_IdAndNombreIgnoreCaseAndIdNot(1L, "Ingeniería de Software", 10L)).thenReturn(true);

        LineaTematicaRequest request = new LineaTematicaRequest(1L, "Ingeniería de Software", "Descripción");

        assertThrows(LineaTematicaDuplicadaException.class, () -> lineaTematicaService.actualizar(10L, request));
        verify(lineaTematicaRepository, never()).save(any());
    }

    // ---------------- Eliminar ----------------

    @Test
    @DisplayName("Eliminar: elimina exitosamente si no está en uso")
    void eliminar_lineaSinUso_eliminaCorrectamente() {
        Evento e = evento();
        LineaTematica lt = linea(e);
        when(lineaTematicaRepository.findById(10L)).thenReturn(Optional.of(lt));
        when(lineaTematicaRepository.estaEnUso(10L)).thenReturn(false);

        lineaTematicaService.eliminar(10L);

        verify(lineaTematicaRepository).delete(lt);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.LINEA_TEMATICA_ELIMINADA),
                eq(LineaTematicaService.ENTIDAD_LINEA_TEMATICA), eq(10L), anyMap());
    }

    @Test
    @DisplayName("Eliminar: rechaza eliminación si la línea está vinculada a propuestas o actividades (409)")
    void eliminar_lineaEnUso_lanzaOperacionNoPermitidaException() {
        Evento e = evento();
        LineaTematica lt = linea(e);
        when(lineaTematicaRepository.findById(10L)).thenReturn(Optional.of(lt));
        when(lineaTematicaRepository.estaEnUso(10L)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class, () -> lineaTematicaService.eliminar(10L));
        verify(lineaTematicaRepository, never()).delete(any());
    }

    // ---------------- Consultas ----------------

    @Test
    @DisplayName("ListarPorEvento: devuelve todas las líneas temáticas del evento")
    void listarPorEvento_eventoExistente_retornaLista() {
        Evento e = evento();
        LineaTematica lt1 = linea(e);
        LineaTematica lt2 = LineaTematica.builder().id(11L).evento(e).nombre("Redes").descripcion("Infraestructura").build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));
        when(lineaTematicaRepository.findByEvento_IdOrderByIdAsc(1L)).thenReturn(List.of(lt1, lt2));

        List<LineaTematicaResponse> lista = lineaTematicaService.listarPorEvento(1L);

        assertEquals(2, lista.size());
        assertEquals("Inteligencia Artificial", lista.get(0).nombre());
        assertEquals("Redes", lista.get(1).nombre());
    }
}
