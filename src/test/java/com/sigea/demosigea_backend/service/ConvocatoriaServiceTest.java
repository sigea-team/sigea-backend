package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.convocatoria.CambioFechaCierreResponse;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaRequest;
import com.sigea.demosigea_backend.dto.convocatoria.ConvocatoriaResponse;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.ConvocatoriaIncompletaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.TipoNotificacion;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private NotificacionService notificacionService;

    private ConvocatoriaService convocatoriaService;

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** "Ahora" fijo para las pruebas: 17/10/2026 12:00 (hora de Bogotá). */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 17, 12, 0);
    private static final LocalDateTime APERTURA = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final LocalDateTime CIERRE = LocalDateTime.of(2026, 11, 15, 23, 59);

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        convocatoriaService = new ConvocatoriaService(convocatoriaRepository, eventoRepository,
                auditoriaService, notificacionService, reloj);
    }

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

    private Evento eventoHabilitado() {
        Evento evento = eventoBase();
        evento.setEstado(EstadoEvento.habilitado);
        return evento;
    }

    private Convocatoria convocatoriaPublicable() {
        Convocatoria c = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        c.setEvento(eventoHabilitado());
        return c;
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

    // ---------------- HU-11 (RF14): Publicar convocatorias ----------------

    @Test
    @DisplayName("HU-11 Criterio 1: una convocatoria completa en borrador se publica, queda activa y se audita")
    void publicar_convocatoriaCompletaEnBorrador_cambiaAPublicadaYAudita() {
        Convocatoria existente = convocatoriaPublicable();
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));

        ConvocatoriaResponse res = convocatoriaService.publicar(10L);

        assertEquals(EstadoConvocatoria.publicada, res.estado());
        verify(convocatoriaRepository).save(existente);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONVOCATORIA_PUBLICADA),
                eq("convocatorias"), eq(10L), anyMap());
    }

    @Test
    @DisplayName("HU-11 Criterio 3: sin fecha límite de recepción, impide publicar y solicita completar el dato")
    void publicar_sinFechaCierre_lanzaConvocatoriaIncompletaException() {
        Convocatoria existente = convocatoriaPublicable();
        existente.setFechaCierre(null);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        ConvocatoriaIncompletaException ex = assertThrows(ConvocatoriaIncompletaException.class,
                () -> convocatoriaService.publicar(10L));

        assertTrue(ex.getCamposFaltantes().containsKey("fechaCierre"));
        assertTrue(ex.getMessage().contains("fecha límite"));
        assertEquals(EstadoConvocatoria.borrador, existente.getEstado());
        verify(convocatoriaRepository, never()).save(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), any(), anyMap());
    }

    @Test
    @DisplayName("HU-11: no se publica una convocatoria cuya fecha límite ya pasó")
    void publicar_fechaCierreVencida_lanzaOperacionNoPermitida() {
        Convocatoria existente = convocatoriaPublicable();
        existente.setFechaApertura(AHORA.minusDays(20));
        existente.setFechaCierre(AHORA.minusDays(1));
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(OperacionNoPermitidaException.class, () -> convocatoriaService.publicar(10L));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-11: no se publica la convocatoria si el evento aún no está publicado (RN-04)")
    void publicar_eventoEnConfiguracion_lanzaOperacionNoPermitida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.borrador); // evento en_configuracion
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> convocatoriaService.publicar(10L));

        assertTrue(ex.getMessage().contains("no está publicado"));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Publicar: Si no está en estado borrador, lanza OperacionNoPermitidaException")
    void publicar_noEnBorrador_lanzaOperacionNoPermitida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(OperacionNoPermitidaException.class, () -> convocatoriaService.publicar(10L));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: modificar la fecha límite de una publicada vigente la actualiza y avisa a todos los usuarios activos")
    void actualizarFechaCierre_publicadaVigente_actualizaYNotifica() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        LocalDateTime nueva = CIERRE.plusDays(15);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(convocatoriaRepository.save(any(Convocatoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionService.notificarATodos(eq(TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA), anyString(),
                anyString(), eq("convocatorias"), eq(10L))).thenReturn(2);

        CambioFechaCierreResponse res = convocatoriaService.actualizarFechaCierre(10L, nueva);

        assertEquals(nueva, res.convocatoria().fechaCierre());
        assertEquals(CIERRE, res.fechaCierreAnterior());
        assertEquals(2, res.usuariosNotificados());

        ArgumentCaptor<String> titulo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> mensaje = ArgumentCaptor.forClass(String.class);
        verify(notificacionService).notificarATodos(eq(TipoNotificacion.CONVOCATORIA_FECHA_MODIFICADA),
                titulo.capture(), mensaje.capture(), eq("convocatorias"), eq(10L));
        assertEquals("Nueva fecha límite: Convocatoria Ponencias 2026", titulo.getValue());
        assertTrue(mensaje.getValue().contains("Congreso de Prueba"));
        assertTrue(mensaje.getValue().contains("15/11/2026 23:59"));
        assertTrue(mensaje.getValue().contains("30/11/2026 23:59"));
        assertTrue(mensaje.getValue().contains("Aún estás a tiempo"));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONVOCATORIA_FECHA_CIERRE_MODIFICADA),
                eq("convocatorias"), eq(10L), anyMap());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: una convocatoria ya expirada no permite modificar su fecha límite")
    void actualizarFechaCierre_convocatoriaExpirada_lanzaConvocatoriaCerradaException() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        existente.setFechaApertura(AHORA.minusDays(20));
        existente.setFechaCierre(AHORA.minusMinutes(1));
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(ConvocatoriaCerradaException.class,
                () -> convocatoriaService.actualizarFechaCierre(10L, AHORA.plusDays(10)));
        verify(convocatoriaRepository, never()).save(any());
        verify(notificacionService, never()).notificarATodos(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: solo se modifica la fecha límite de convocatorias publicadas")
    void actualizarFechaCierre_enBorrador_lanzaOperacionNoPermitida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.borrador);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(OperacionNoPermitidaException.class,
                () -> convocatoriaService.actualizarFechaCierre(10L, CIERRE.plusDays(1)));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: la nueva fecha límite debe ser futura")
    void actualizarFechaCierre_nuevaFechaPasada_lanzaSolicitudInvalida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(SolicitudInvalidaException.class,
                () -> convocatoriaService.actualizarFechaCierre(10L, AHORA.minusHours(1)));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-11 Criterio 2: la nueva fecha límite debe ser posterior a la de apertura")
    void actualizarFechaCierre_anteriorAApertura_lanzaSolicitudInvalida() {
        Convocatoria existente = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        existente.setFechaApertura(AHORA.plusDays(5)); // apertura aún futura
        when(convocatoriaRepository.findById(10L)).thenReturn(Optional.of(existente));

        assertThrows(SolicitudInvalidaException.class,
                () -> convocatoriaService.actualizarFechaCierre(10L, AHORA.plusDays(2)));
        verify(convocatoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("HU-11 Criterio 1: la consulta pública solo devuelve convocatorias publicadas")
    void listarPublicadas_consultaSoloEstadoPublicada() {
        Convocatoria c = convocatoriaEjemplo(EstadoConvocatoria.publicada);
        when(convocatoriaRepository.findByEstadoOrderByIdDesc(EstadoConvocatoria.publicada)).thenReturn(List.of(c));

        List<ConvocatoriaResponse> lista = convocatoriaService.listarPublicadas(null);

        assertEquals(1, lista.size());
        assertEquals(EstadoConvocatoria.publicada, lista.get(0).estado());
    }

    @Test
    @DisplayName("HU-11 Criterio 1: una convocatoria no publicada no es visible en la consulta pública")
    void obtenerPublicada_noPublicada_lanzaRecursoNoEncontrado() {
        when(convocatoriaRepository.findByIdAndEstado(10L, EstadoConvocatoria.publicada)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> convocatoriaService.obtenerPublicada(10L));
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
