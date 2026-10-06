package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConvocatoriaServiceTest {

    @Mock
    private ConvocatoriaRepository convocatoriaRepository;

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private ConvocatoriaService convocatoriaService;

    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final LocalDateTime CIERRE = LocalDateTime.of(2026, 11, 15, 23, 59);

    private Evento eventoBase() {
        return Evento.builder()
                .id(1L)
                .nombre("Congreso de Prueba")
                .fechaInicio(LocalDate.of(2026, 10, 1))
                .fechaFin(LocalDate.of(2026, 10, 5))
                .estado(EstadoEvento.en_configuracion)
                .build();
    }

    private ConvocatoriaRequest requestValida() {
        return new ConvocatoriaRequest(
                1L,
                "Convocatoria Ponencias 2026",
                "Descripción de la convocatoria",
                "Requisitos de la convocatoria",
                APERTURA,
                CIERRE
        );
    }

    private Convocatoria convocatoriaEjemplo(EstadoConvocatoria estado) {
        return Convocatoria.builder()
                .id(10L)
                .evento(eventoBase())
                .titulo("Convocatoria Ponencias 2026")
                .descripcion("Descripción")
                .requisitos("Requisitos")
                .fechaApertura(APERTURA)
                .fechaCierre(CIERRE)
                .estado(estado)
                .build();
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: Dado que el evento está configurado, la convocatoria se guarda en estado borrador")
    void crear_eventoExiste_guardaEnEstadoBorrador() {
        Evento evento = eventoBase();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> {
            Convocatoria c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ConvocatoriaResponse response = convocatoriaService.crear(requestValida());

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(1L, response.eventoId());
        assertEquals("Convocatoria Ponencias 2026", response.titulo());
        assertEquals(EstadoConvocatoria.borrador, response.estado());
        assertFalse(response.estaAbierta());
        verify(convocatoriaRepository).save(any(Convocatoria.class));
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: Si la fecha de cierre es anterior a la de apertura, rechaza la operación")
    void crear_fechaCierreAnterior_lanzaSolicitudInvalidaException() {
        ConvocatoriaRequest requestInvalido = new ConvocatoriaRequest(
                1L,
                "Título",
                "Desc",
                "Req",
                CIERRE,
                APERTURA // Cierre antes que apertura
        );

        SolicitudInvalidaException ex = assertThrows(
                SolicitudInvalidaException.class,
                () -> convocatoriaService.crear(requestInvalido)
        );

        assertTrue(ex.getMessage().contains("posterior a la fecha de apertura"));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: Si la fecha de cierre es igual a la de apertura, rechaza la operación")
    void crear_fechasIguales_lanzaSolicitudInvalidaException() {
        ConvocatoriaRequest requestInvalido = new ConvocatoriaRequest(
                1L,
                "Título",
                "Desc",
                "Req",
                APERTURA,
                APERTURA
        );

        assertThrows(SolicitudInvalidaException.class, () -> convocatoriaService.crear(requestInvalido));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 2: Si el evento no existe, rechaza la operación con RecursoNoEncontradoException")
    void crear_eventoNoExiste_lanzaRecursoNoEncontrado() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());

        ConvocatoriaRequest request = new ConvocatoriaRequest(
                99L, "Título", "Desc", "Req", APERTURA, CIERRE
        );

        assertThrows(RecursoNoEncontradoException.class, () -> convocatoriaService.crear(request));
        verify(convocatoriaRepository, never()).save(any());
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: Editar una convocatoria en borrador actualiza la información sin publicarla")
    void actualizar_convocatoriaEnBorrador_actualizaSinPublicar() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase()));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime nuevaApertura = APERTURA.plusDays(1);
        LocalDateTime nuevoCierre = CIERRE.plusDays(10);
        ConvocatoriaRequest cambios = new ConvocatoriaRequest(
                1L,
                "Título Modificado",
                "Nueva Descripción",
                "Nuevos Requisitos",
                nuevaApertura,
                nuevoCierre
        );

        ConvocatoriaResponse response = convocatoriaService.actualizar(10L, cambios);

        assertEquals("Título Modificado", response.titulo());
        assertEquals("Nueva Descripción", response.descripcion());
        assertEquals(nuevaApertura, response.fechaApertura());
        assertEquals(nuevoCierre, response.fechaCierre());
        assertEquals(EstadoConvocatoria.borrador, response.estado());
        assertFalse(response.estaAbierta());
    }

    @Test
    @DisplayName("Criterio 3: Intentar actualizar con fechas inválidas es rechazado")
    void actualizar_fechasInvalidas_lanzaSolicitudInvalidaException() {
        ConvocatoriaRequest cambios = new ConvocatoriaRequest(
                1L, "Título", "Desc", "Req", CIERRE, APERTURA
        );

        assertThrows(SolicitudInvalidaException.class, () -> convocatoriaService.actualizar(10L, cambios));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Publicar: Cambia estado de borrador a publicada exitosamente")
    void publicar_convocatoriaEnBorrador_cambiaAPublicada() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvocatoriaResponse res = convocatoriaService.publicar(10L);

        assertEquals(EstadoConvocatoria.publicada, res.estado());
        verify(convocatoriaRepository).save(existente);
    }

    @Test
    @DisplayName("Publicar: Si no está en estado borrador, lanza OperacionNoPermitidaException")
    void publicar_noEnBorrador_lanzaOperacionNoPermitida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(OperacionNoPermitidaException.class, () -> convocatoriaService.publicar(10L));
        verify(convocatoriaRepository, never()).save(any());
    }

    // ---------------- Criterio 4 ----------------

    @Test
    @DisplayName("Criterio 4: Dado que la fecha de cierre ya se cumplió, el sistema impide automáticamente el envío")
    void validarRecepcionPropuestas_fechaCierreTranscurrida_lanzaConvocatoriaCerradaException() {
        LocalDateTime aperturaPasada = LocalDateTime.now().minusDays(10);
        LocalDateTime cierrePasado = LocalDateTime.now().minusDays(1);

        Convocatoria convocatoriaExpirada = Convocatoria.builder()
                .id(10L)
                .evento(eventoBase())
                .titulo("Convocatoria Cerrada")
                .fechaApertura(aperturaPasada)
                .fechaCierre(cierrePasado)
                .estado(EstadoConvocatoria.publicada)
                .build();

        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoriaExpirada));

        ConvocatoriaCerradaException ex = assertThrows(
                ConvocatoriaCerradaException.class,
                () -> convocatoriaService.validarRecepcionPropuestas(10L)
        );

        assertTrue(ex.getMessage().contains("ya se cumplió"));
    }

    @Test
    @DisplayName("Criterio 4: Si la convocatoria está en borrador (no publicada), no permite recepción")
    void validarRecepcionPropuestas_estadoBorrador_lanzaOperacionNoPermitidaException() {
        LocalDateTime aperturaPasada = LocalDateTime.now().minusDays(10);
        LocalDateTime cierreFuturo = LocalDateTime.now().plusDays(10);

        Convocatoria convocatoriaBorrador = Convocatoria.builder()
                .id(10L)
                .evento(eventoBase())
                .titulo("Convocatoria Borrador")
                .fechaApertura(aperturaPasada)
                .fechaCierre(cierreFuturo)
                .estado(EstadoConvocatoria.borrador)
                .build();

        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoriaBorrador));

        OperacionNoPermitidaException ex = assertThrows(
                OperacionNoPermitidaException.class,
                () -> convocatoriaService.validarRecepcionPropuestas(10L)
        );

        assertTrue(ex.getMessage().contains("no está publicada"));
    }

    @Test
    @DisplayName("Criterio 4: Convocatoria publicada y en periodo vigente permite el envío sin lanzar excepción")
    void validarRecepcionPropuestas_vigenteYPublicada_exito() {
        LocalDateTime aperturaPasada = LocalDateTime.now().minusDays(1);
        LocalDateTime cierreFuturo = LocalDateTime.now().plusDays(10);

        Convocatoria convocatoriaVigente = Convocatoria.builder()
                .id(10L)
                .evento(eventoBase())
                .titulo("Convocatoria Vigente")
                .fechaApertura(aperturaPasada)
                .fechaCierre(cierreFuturo)
                .estado(EstadoConvocatoria.publicada)
                .build();

        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(convocatoriaVigente));

        assertDoesNotThrow(() -> convocatoriaService.validarRecepcionPropuestas(10L));
    }

    // ---------------- Consultas y Eliminación ----------------

    @Test
    @DisplayName("Obtener por ID: retorna DTO de la convocatoria")
    void obtenerPorId_exitoso() {
        Convocatoria c = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(c));

        ConvocatoriaResponse res = convocatoriaService.obtenerPorId(10L);

        assertEquals(10L, res.id());
        assertEquals("Convocatoria Ponencias 2026", res.titulo());
    }

    @Test
    @DisplayName("Listar: consulta convocatorias aplicando filtros opcionales")
    void listar_conFiltroEventoYEstado() {
        Convocatoria c = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findByEvento_IdAndEstadoOrderByIdDesc(1L, EstadoConvocatoria.borrador))
                .thenReturn(List.of(c));

        List<ConvocatoriaResponse> lista = convocatoriaService.listar(1L, EstadoConvocatoria.borrador);

        assertEquals(1, lista.size());
        assertEquals(10L, lista.get(0).id());
    }

    @Test
    @DisplayName("Eliminar: elimina correctamente una convocatoria existente")
    void eliminar_exitoso() {
        Convocatoria c = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(c));

        convocatoriaService.eliminar(10L);

        verify(convocatoriaRepository).delete(c);
    }
}
