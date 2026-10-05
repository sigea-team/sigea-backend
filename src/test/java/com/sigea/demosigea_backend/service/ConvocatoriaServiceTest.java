package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaServiceTest {

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private ConvocatoriaService convocatoriaService;

    private Evento evento() {
        return Evento.builder()
                .id(1L)
                .nombre("Congreso de Prueba")
                .estado(EstadoEvento.en_configuracion)
                .fechaInicio(LocalDate.of(2026, 10, 1))
                .fechaFin(LocalDate.of(2026, 10, 5))
                .build();
    }

    private Convocatoria convocatoria(Evento evento) {
        return Convocatoria.builder()
                .id(5L)
                .evento(evento)
                .titulo("Convocatoria Ponencias 2026")
                .descripcion("Recepción de trabajos de investigación")
                .requisitos("Formato IEEE")
                .fechaApertura(LocalDateTime.of(2026, 4, 1, 8, 0))
                .fechaCierre(LocalDateTime.of(2026, 6, 1, 23, 59))
                .estado(EstadoConvocatoria.borrador)
                .build();
    }

    @Test
    @DisplayName("Crear convocatoria: crea exitosamente y registra auditoría")
    void crear_datosValidos_creaConvocatoria() {
        Evento e = evento();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> {
            Convocatoria c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });

        ConvocatoriaRequest request = new ConvocatoriaRequest(
                1L,
                "Convocatoria Ponencias 2026",
                "Descripción",
                "Requisitos",
                LocalDateTime.of(2026, 4, 1, 8, 0),
                LocalDateTime.of(2026, 6, 1, 23, 59),
                EstadoConvocatoria.borrador
        );

        ConvocatoriaResponse response = convocatoriaService.crear(1L, request);

        assertNotNull(response);
        assertEquals(5L, response.id());
        assertEquals(1L, response.eventoId());
        assertEquals("Convocatoria Ponencias 2026", response.titulo());
        assertEquals(EstadoConvocatoria.borrador, response.estado());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONVOCATORIA_CREADA),
                eq(ConvocatoriaService.ENTIDAD_CONVOCATORIA), eq(5L), anyMap());
    }

    @Test
    @DisplayName("Crear convocatoria: rechaza si la fecha de cierre es anterior a la fecha de apertura")
    void crear_fechasInvalida_lanzaSolicitudInvalidaException() {
        Evento e = evento();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(e));

        ConvocatoriaRequest request = new ConvocatoriaRequest(
                1L,
                "Convocatoria Fechas Mal",
                "Descripción",
                "Requisitos",
                LocalDateTime.of(2026, 6, 1, 8, 0),
                LocalDateTime.of(2026, 4, 1, 23, 59),
                EstadoConvocatoria.borrador
        );

        assertThrows(SolicitudInvalidaException.class, () -> convocatoriaService.crear(1L, request));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar convocatoria: modifica los datos correctamente")
    void actualizar_datosValidos_actualizaConvocatoria() {
        Evento e = evento();
        Convocatoria c = convocatoria(e);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvocatoriaRequest request = new ConvocatoriaRequest(
                1L,
                "Título Actualizado",
                "Nueva Descripción",
                "Nuevos Requisitos",
                LocalDateTime.of(2026, 4, 1, 8, 0),
                LocalDateTime.of(2026, 7, 1, 23, 59),
                EstadoConvocatoria.publicada
        );

        ConvocatoriaResponse response = convocatoriaService.actualizar(5L, request);

        assertEquals("Título Actualizado", response.titulo());
        assertEquals(EstadoConvocatoria.publicada, response.estado());

        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONVOCATORIA_ACTUALIZADA),
                eq(ConvocatoriaService.ENTIDAD_CONVOCATORIA), eq(5L), anyMap());
    }

    @Test
    @DisplayName("Eliminar convocatoria: elimina el registro y audita")
    void eliminar_idExistente_eliminaExitosamente() {
        Evento e = evento();
        Convocatoria c = convocatoria(e);
        when(convocatoriaRepository.findById(5L)).thenReturn(Optional.of(c));

        convocatoriaService.eliminar(5L);

        verify(convocatoriaRepository).delete(c);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONVOCATORIA_ELIMINADA),
                eq(ConvocatoriaService.ENTIDAD_CONVOCATORIA), eq(5L), anyMap());
    }

    @Test
    @DisplayName("Obtener por ID: si no existe lanza RecursoNoEncontradoException (404)")
    void obtenerPorId_inexistente_lanzaException() {
        when(convocatoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> convocatoriaService.obtenerPorId(99L));
    }
}
