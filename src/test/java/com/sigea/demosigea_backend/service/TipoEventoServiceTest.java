package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoRequest;
import com.sigea.demosigea_backend.dto.tipoevento.TipoEventoResponse;
import com.sigea.demosigea_backend.dto.tipoevento.UsoTipoEventoResponse;
import com.sigea.demosigea_backend.exception.ParametroEventoDuplicadoException;
import com.sigea.demosigea_backend.exception.ParametroEventoEnUsoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.TipoEvento;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.TipoEventoRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoEventoServiceTest {

    @Mock
    private TipoEventoRepository tipoRepository;

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private TipoEventoService tipoService;

    private static TipoEvento congreso() {
        return TipoEvento.builder().id(1L).nombre("Congreso").descripcion("Varios días").build();
    }

    @Test
    @DisplayName("Listar: devuelve el catálogo ordenado que entrega el repositorio")
    void listar() {
        when(tipoRepository.findAllByOrderByNombreAscIdAsc()).thenReturn(List.of(congreso()));

        List<TipoEventoResponse> tipos = tipoService.listar();

        assertEquals(1, tipos.size());
        assertEquals("Congreso", tipos.get(0).nombre());
    }

    @Test
    @DisplayName("Crear: normaliza el nombre, guarda y registra en la auditoría")
    void crear_normalizaYAudita() {
        when(tipoRepository.existsByNombreIgnoreCase("Encuentro de semilleros")).thenReturn(false);
        when(tipoRepository.saveAndFlush(any(TipoEvento.class))).thenAnswer(inv -> {
            TipoEvento t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TipoEventoResponse creado = tipoService.crear(new TipoEventoRequest("  Encuentro   de semilleros ", "  "));

        assertEquals(7L, creado.id());
        assertEquals("Encuentro de semilleros", creado.nombre());
        assertNull(creado.descripcion());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_EVENTO_CREADO), eq("tipos_evento"), eq(7L), anyMap());
    }

    @Test
    @DisplayName("Crear: rechaza un nombre que ya existe sin distinguir mayúsculas")
    void crear_duplicado_rechaza() {
        when(tipoRepository.existsByNombreIgnoreCase("congreso")).thenReturn(true);

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.crear(new TipoEventoRequest("congreso", null)));
        verify(tipoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Crear: si el índice único detecta un duplicado (peticiones simultáneas), responde como duplicado")
    void crear_violacionIndice_seTraduceADuplicado() {
        when(tipoRepository.existsByNombreIgnoreCase("Congreso")).thenReturn(false);
        when(tipoRepository.saveAndFlush(any(TipoEvento.class)))
                .thenThrow(violacion("23505", "ux_tipos_evento_nombre_ci"));

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.crear(new TipoEventoRequest("Congreso", null)));
    }

    @Test
    @DisplayName("Actualizar: el propio registro no cuenta como duplicado y se audita el antes y el después")
    void actualizar_excluyeElPropioRegistro() {
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(congreso()));
        when(tipoRepository.existsByNombreIgnoreCaseAndIdNot("Congreso internacional", 1L)).thenReturn(false);
        when(tipoRepository.saveAndFlush(any(TipoEvento.class))).thenAnswer(inv -> inv.getArgument(0));

        TipoEventoResponse actualizado = tipoService.actualizar(1L, new TipoEventoRequest("Congreso internacional", "Con invitados"));

        assertEquals("Congreso internacional", actualizado.nombre());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_EVENTO_ACTUALIZADO), eq("tipos_evento"), eq(1L), anyMap());
    }

    @Test
    @DisplayName("Actualizar: rechaza el nombre de otro tipo existente")
    void actualizar_duplicado_rechaza() {
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(congreso()));
        when(tipoRepository.existsByNombreIgnoreCaseAndIdNot("Seminario", 1L)).thenReturn(true);

        assertThrows(ParametroEventoDuplicadoException.class,
                () -> tipoService.actualizar(1L, new TipoEventoRequest("Seminario", null)));
    }

    @Test
    @DisplayName("Uso: informa cuántos eventos usan el tipo")
    void consultarUso() {
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(congreso()));
        when(eventoRepository.countByTipoEvento_Id(1L)).thenReturn(3L);

        UsoTipoEventoResponse uso = tipoService.consultarUso(1L);

        assertEquals(3L, uso.eventos());
        assertFalse(uso.eliminable());
    }

    @Test
    @DisplayName("Eliminar: un tipo que no usa ningún evento se elimina y se audita")
    void eliminar_sinUso() {
        TipoEvento tipo = congreso();
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(eventoRepository.countByTipoEvento_Id(1L)).thenReturn(0L);

        tipoService.eliminar(1L);

        verify(tipoRepository).delete(tipo);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.TIPO_EVENTO_ELIMINADO), eq("tipos_evento"), eq(1L), anyMap());
    }

    @Test
    @DisplayName("Eliminar: un tipo en uso no se elimina e informa cuántos eventos lo usan")
    void eliminar_enUso_rechaza() {
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(congreso()));
        when(eventoRepository.countByTipoEvento_Id(1L)).thenReturn(2L);

        ParametroEventoEnUsoException ex = assertThrows(ParametroEventoEnUsoException.class, () -> tipoService.eliminar(1L));

        assertTrue(ex.getMessage().contains("2 evento(s)"));
        verify(tipoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Eliminar: si la llave foránea lo impide (un evento lo tomó a la vez), responde como en uso")
    void eliminar_violacionLlaveForanea_seTraduceAEnUso() {
        TipoEvento tipo = congreso();
        when(tipoRepository.findById(1L)).thenReturn(Optional.of(tipo));
        when(eventoRepository.countByTipoEvento_Id(1L)).thenReturn(0L);
        doThrow(violacion("23503", "eventos_tipo_evento_id_fkey")).when(tipoRepository).flush();

        assertThrows(ParametroEventoEnUsoException.class, () -> tipoService.eliminar(1L));
        verify(auditoriaService, never()).registrar(any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("Un tipo inexistente responde 404")
    void obtener_inexistente() {
        when(tipoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> tipoService.obtener(9L));
    }

    @Test
    @DisplayName("Crear: guarda la descripción sin espacios sobrantes")
    void crear_guardaDescripcion() {
        when(tipoRepository.existsByNombreIgnoreCase("Hackatón")).thenReturn(false);
        when(tipoRepository.saveAndFlush(any(TipoEvento.class))).thenAnswer(inv -> inv.getArgument(0));

        tipoService.crear(new TipoEventoRequest("Hackatón", "  Maratón de programación  "));

        ArgumentCaptor<TipoEvento> captor = ArgumentCaptor.forClass(TipoEvento.class);
        verify(tipoRepository).saveAndFlush(captor.capture());
        assertEquals("Maratón de programación", captor.getValue().getDescripcion());
    }

    /** Imita la excepción que entrega Spring: DataIntegrityViolationException envolviendo la de Hibernate. */
    private static DataIntegrityViolationException violacion(String sqlState, String restriccion) {
        ConstraintViolationException hibernate = new ConstraintViolationException(
                "could not execute statement", new SQLException("error de integridad", sqlState), restriccion);
        return new DataIntegrityViolationException("could not execute statement", hibernate);
    }
}
