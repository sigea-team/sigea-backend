package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asistencia.AsistenciaResponse;
import com.sigea.demosigea_backend.dto.asistencia.AsistenciasActividadResponse;
import com.sigea.demosigea_backend.exception.AsistenciaRechazadaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Asistencia;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.MetodoRegistroAsistencia;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.ActividadRepository;
import com.sigea.demosigea_backend.repository.AsistenciaRepository;
import com.sigea.demosigea_backend.repository.CodigoQrRepository;
import com.sigea.demosigea_backend.repository.InscripcionActividadRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link AsistenciaService} (HU-24, Criterios 2 y 3).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenciaServiceTest {

    private static final String CODIGO = "SIGEA-11112222333344445555666677778888";

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private CodigoQrRepository codigoQrRepository;
    @Mock private InscripcionEventoRepository inscripcionEventoRepository;
    @Mock private InscripcionActividadRepository inscripcionActividadRepository;
    @Mock private ActividadRepository actividadRepository;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private AsistenciaService servicio;

    private Evento evento;
    private Actividad actividad;
    private InscripcionEvento inscripcion;
    private CodigoQr qr;

    @BeforeEach
    void setUp() {
        evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.en_ejecucion).build();
        Persona persona = Persona.builder().id(10L).nombres("María").apellidos("Pérez")
                .numeroDocumento("1090123456").correo("maria.perez@ufps.edu.co").build();
        inscripcion = InscripcionEvento.builder().id(501L).evento(evento).persona(persona)
                .categoriaParticipante(CategoriaParticipante.estudiante).fechaInscripcion(LocalDateTime.now()).build();
        actividad = Actividad.builder().id(201L).evento(evento).nombre("Taller de Spring Boot")
                .fecha(LocalDate.now()).horaInicio(LocalTime.of(9, 0)).horaFin(LocalTime.of(11, 0))
                .estado(Actividad.ESTADO_PROGRAMADA).build();
        qr = CodigoQr.builder().id(77L).inscripcion(inscripcion).codigo(CODIGO)
                .fechaGeneracion(LocalDateTime.now().minusDays(1)).vigente(true).build();

        when(actividadRepository.findByIdAndEvento_Id(201L, 1L)).thenReturn(Optional.of(actividad));
        when(codigoQrRepository.findByCodigoConInscripcion(CODIGO)).thenReturn(Optional.of(qr));
        when(inscripcionEventoRepository.findByIdAndEvento_IdParaAsistencia(501L, 1L)).thenReturn(Optional.of(inscripcion));
        when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(true);
        when(asistenciaRepository.findByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(Optional.empty());
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(inv -> {
            Asistencia a = inv.getArgument(0);
            a.setId(900L);
            return a;
        });
    }

    private AsistenciaRechazadaException rechazoQr(String codigoLeido) {
        return assertThrows(AsistenciaRechazadaException.class, () -> servicio.registrarPorQr(1L, 201L, codigoLeido));
    }

    @Test
    @DisplayName("Criterio 2: con un QR válido registra la asistencia con fecha y hora y método qr")
    void registraConQrValido() {
        LocalDateTime antes = LocalDateTime.now();

        AsistenciaResponse resp = servicio.registrarPorQr(1L, 201L, "  " + CODIGO + "\n");

        assertEquals(900L, resp.asistenciaId());
        assertEquals(501L, resp.inscripcionId());
        assertEquals(201L, resp.actividadId());
        assertEquals("María Pérez", resp.participanteNombre());
        assertEquals("1090123456", resp.numeroDocumento());
        assertEquals(MetodoRegistroAsistencia.qr, resp.metodoRegistro());
        assertNotNull(resp.fechaHoraRegistro());
        assertTrue(!resp.fechaHoraRegistro().isBefore(antes));

        ArgumentCaptor<Asistencia> guardada = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(guardada.capture());
        assertEquals(actividad, guardada.getValue().getActividad());
        assertEquals(inscripcion, guardada.getValue().getInscripcion());
        verify(inscripcionEventoRepository).findByIdAndEvento_IdParaAsistencia(501L, 1L);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.ASISTENCIA_REGISTRADA), eq("asistencias"), eq(900L), any());
    }

    @Test
    @DisplayName("Criterio 3: un código desconocido se rechaza con QR_INVALIDO (404)")
    void rechazaCodigoDesconocido() {
        when(codigoQrRepository.findByCodigoConInscripcion("OTRO")).thenReturn(Optional.empty());

        AsistenciaRechazadaException ex = rechazoQr("OTRO");

        assertEquals(AsistenciaRechazadaException.QR_INVALIDO, ex.getCodigo());
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: un código no vigente se rechaza")
    void rechazaCodigoNoVigente() {
        qr.setVigente(false);

        assertEquals(AsistenciaRechazadaException.QR_NO_VIGENTE, rechazoQr(CODIGO).getCodigo());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: el QR de una inscripción a otro evento se rechaza")
    void rechazaQrDeOtroEvento() {
        inscripcion.setEvento(Evento.builder().id(2L).nombre("Congreso de Software").build());

        AsistenciaRechazadaException ex = rechazoQr(CODIGO);

        assertEquals(AsistenciaRechazadaException.QR_OTRO_EVENTO, ex.getCodigo());
        assertTrue(ex.getMessage().contains("Congreso de Software"));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: quien no está inscrito en la actividad no queda registrado")
    void rechazaNoInscritoEnActividad() {
        when(inscripcionActividadRepository.existsByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(false);

        AsistenciaRechazadaException ex = rechazoQr(CODIGO);

        assertEquals(AsistenciaRechazadaException.NO_INSCRITO_ACTIVIDAD, ex.getCodigo());
        assertTrue(ex.getMessage().contains("Taller de Spring Boot"));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: si la asistencia ya existe se informa con la asistencia previa, sin duplicarla")
    void informaAsistenciaDuplicada() {
        Asistencia previa = Asistencia.builder().id(800L).inscripcion(inscripcion).actividad(actividad)
                .fechaHoraRegistro(LocalDateTime.of(2026, 11, 20, 9, 5)).metodoRegistro(MetodoRegistroAsistencia.qr).build();
        when(asistenciaRepository.findByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(Optional.of(previa));

        AsistenciaRechazadaException ex = rechazoQr(CODIGO);

        assertEquals(AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, ex.getCodigo());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("20/11/2026 09:05"));
        assertEquals(800L, ex.getAsistenciaExistente().asistenciaId());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: dos lecturas simultáneas: la que choca con el índice único se informa como duplicada")
    void choqueConIndiceUnicoEsDuplicado() {
        when(asistenciaRepository.save(any(Asistencia.class))).thenThrow(new DataIntegrityViolationException("dup"));

        AsistenciaRechazadaException ex = rechazoQr(CODIGO);

        assertEquals(AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, ex.getCodigo());
        assertNull(ex.getAsistenciaExistente());
    }

    @Test
    @DisplayName("No se registra asistencia en una actividad cancelada")
    void rechazaActividadCancelada() {
        actividad.setEstado(Actividad.ESTADO_CANCELADA);

        assertEquals(AsistenciaRechazadaException.ACTIVIDAD_NO_DISPONIBLE, rechazoQr(CODIGO).getCodigo());
    }

    @Test
    @DisplayName("No se registra asistencia en una actividad por programar")
    void rechazaActividadPorProgramar() {
        actividad.setEstado(Actividad.ESTADO_POR_PROGRAMAR);

        assertEquals(AsistenciaRechazadaException.ACTIVIDAD_NO_DISPONIBLE, rechazoQr(CODIGO).getCodigo());
    }

    @Test
    @DisplayName("No se registra asistencia con el evento cerrado o en configuración")
    void rechazaEventoNoDisponible() {
        evento.setEstado(EstadoEvento.cerrado);
        assertEquals(AsistenciaRechazadaException.EVENTO_NO_DISPONIBLE, rechazoQr(CODIGO).getCodigo());

        evento.setEstado(EstadoEvento.en_configuracion);
        assertEquals(AsistenciaRechazadaException.EVENTO_NO_DISPONIBLE, rechazoQr(CODIGO).getCodigo());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actividad de otro evento: 404")
    void actividadDeOtroEvento() {
        when(actividadRepository.findByIdAndEvento_Id(201L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.registrarPorQr(2L, 201L, CODIGO));
    }

    @Test
    @DisplayName("Registro manual: método manual y mismas validaciones")
    void registroManual() {
        AsistenciaResponse resp = servicio.registrarManual(1L, 201L, 501L);

        assertEquals(MetodoRegistroAsistencia.manual, resp.metodoRegistro());
        verify(codigoQrRepository, never()).findByCodigoConInscripcion(any());
    }

    @Test
    @DisplayName("Registro manual: inscripción que no es del evento → 404")
    void registroManualInscripcionInexistente() {
        when(inscripcionEventoRepository.findByIdAndEvento_IdParaAsistencia(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.registrarManual(1L, 201L, 999L));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registro manual: tampoco duplica una asistencia registrada por QR")
    void registroManualDuplicado() {
        when(asistenciaRepository.findByInscripcion_IdAndActividad_Id(501L, 201L)).thenReturn(Optional.of(
                Asistencia.builder().id(800L).inscripcion(inscripcion).actividad(actividad)
                        .fechaHoraRegistro(LocalDateTime.now()).metodoRegistro(MetodoRegistroAsistencia.qr).build()));

        AsistenciaRechazadaException ex = assertThrows(AsistenciaRechazadaException.class,
                () -> servicio.registrarManual(1L, 201L, 501L));
        assertEquals(AsistenciaRechazadaException.ASISTENCIA_DUPLICADA, ex.getCodigo());
    }

    @Test
    @DisplayName("Lista las asistencias de la actividad con el total de inscritos")
    void listaAsistencias() {
        Asistencia a = Asistencia.builder().id(800L).inscripcion(inscripcion).actividad(actividad)
                .fechaHoraRegistro(LocalDateTime.now()).metodoRegistro(MetodoRegistroAsistencia.qr).build();
        when(asistenciaRepository.findByActividadConParticipante(201L)).thenReturn(List.of(a));
        when(inscripcionActividadRepository.countByActividad_Id(201L)).thenReturn(30L);

        AsistenciasActividadResponse resp = servicio.listarPorActividad(1L, 201L);

        assertEquals(30L, resp.inscritos());
        assertEquals(1L, resp.asistentes());
        assertEquals("María Pérez", resp.asistencias().get(0).participanteNombre());
    }
}
