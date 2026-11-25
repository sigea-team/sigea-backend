package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.certificado.ConfiguracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.EmisionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.GeneracionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.HabilitacionCertificadosResponse;
import com.sigea.demosigea_backend.dto.certificado.MisCertificadosEventoResponse;
import com.sigea.demosigea_backend.dto.certificado.OpcionCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.VerificacionCertificadoResponse;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.EventoArchivadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Actividad;
import com.sigea.demosigea_backend.model.Certificado;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ParticipacionConferencista;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.TipoParticipacionConferencista;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.CertificadoRepository;
import com.sigea.demosigea_backend.repository.CertificadoRepository.ResumenAsistencia;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.ParticipacionConferencistaRepository;
import com.sigea.demosigea_backend.repository.PersonaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link CertificadoService} (HU-26, RF47, CU-23).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificadoServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 24, 18, 30, 15);
    private static final String CORREO = "ana@ufps.edu.co";
    private static final byte[] PDF = "%PDF-1.4 prueba".getBytes();

    @Mock private CertificadoRepository certificadoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ParticipacionConferencistaRepository participacionRepository;
    @Mock private GeneradorPdfCertificado generadorPdf;
    @Mock private AlmacenamientoArchivos almacenamiento;
    @Mock private AuditoriaService auditoriaService;
    @Mock private PlantillaCertificadoService plantillaService;

    private CertificadoService servicio;
    private Evento evento;
    private Persona ana;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA);
        servicio = new CertificadoService(certificadoRepository, eventoRepository, personaRepository, usuarioRepository,
                participacionRepository, generadorPdf, almacenamiento, auditoriaService, plantillaService, reloj);
        ReflectionTestUtils.setField(servicio, "urlVerificacionBase", "https://sigea-team.github.io/sigea-front/#/verificar/");

        evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.cerrado)
                .fechaInicio(LocalDate.of(2026, 11, 10)).fechaFin(LocalDate.of(2026, 11, 12))
                .porcentajeMinimoAsistencia(new BigDecimal("80.00")).build();
        ana = Persona.builder().id(10L).nombres("Ana María").apellidos("Pérez").tipoDocumento("CC")
                .numeroDocumento("1090123456").correo(CORREO).build();

        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(certificadoRepository.bloquearEmision(anyLong(), anyLong())).thenReturn(1);
        when(certificadoRepository.findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());
        when(certificadoRepository.existsByCodigoVerificacion(anyString())).thenReturn(false);
        when(certificadoRepository.saveAndFlush(any(Certificado.class))).thenAnswer(inv -> {
            Certificado c = inv.getArgument(0);
            c.setId(77L);
            return c;
        });
        when(certificadoRepository.actividadesComoPonente(anyLong(), anyLong())).thenReturn(List.of());
        when(participacionRepository.findByEvento_IdAndPersona_Id(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(generadorPdf.generar(any())).thenReturn(PDF);
        when(plantillaService.cargarParaPdf(anyLong())).thenReturn(new PlantillaCertificadoService.PlantillaPdf(null, List.of()));
        // Por defecto los certificados del evento ya están habilitados; las pruebas de habilitación lo cambian.
        when(plantillaService.fechaHabilitacion(anyLong())).thenReturn(Optional.of(AHORA.minusDays(1)));
        when(almacenamiento.guardar(anyString(), eq("pdf"), any(InputStream.class))).thenReturn("certificados/1/a.pdf");

        autenticar(CORREO, "ROLE_PARTICIPANTE");
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO))
                .thenReturn(Optional.of(Usuario.builder().id(100L).persona(ana).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 1: cumple los requisitos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 1: con el 80 % de asistencia se emite el certificado de asistente con los datos del evento")
    void emiteCertificadoDeAsistente() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 5, 4, 480)));

        EmisionCertificadoResponse respuesta = servicio.solicitar(1L, TipoCertificado.asistente);

        assertTrue(respuesta.generadoAhora());
        ArgumentCaptor<Certificado> captor = ArgumentCaptor.forClass(Certificado.class);
        verify(certificadoRepository).saveAndFlush(captor.capture());
        Certificado c = captor.getValue();
        assertEquals(TipoCertificado.asistente, c.getTipo());
        assertEquals(new BigDecimal("80.00"), c.getPorcentajeAsistencia());
        assertEquals("Ana María Pérez", c.getNombreParticipante());
        assertEquals("CC", c.getTipoDocumento());
        assertEquals("1090123456", c.getNumeroDocumento());
        assertEquals("Semana de Ingeniería", c.getNombreEvento());
        assertEquals(LocalDate.of(2026, 11, 10), c.getFechaInicioEvento());
        assertEquals(LocalDate.of(2026, 11, 12), c.getFechaFinEvento());
        assertEquals(AHORA, c.getFechaGeneracion());
        assertEquals("certificados/1/a.pdf", c.getArchivoUrl());
        assertTrue(c.getCodigoVerificacion().matches("CERT-[0-9A-F]{24}"));
        assertEquals("Asistió a 4 de las 5 actividades en las que se inscribió (80 %), con una intensidad de 8 horas.",
                c.getDetalle());
        assertEquals(77L, respuesta.certificado().certificadoId());
        assertEquals("/api/v1/certificados/77/pdf", respuesta.certificado().urlDescarga());

        ArgumentCaptor<GeneradorPdfCertificado.DatosCertificado> datos =
                ArgumentCaptor.forClass(GeneradorPdfCertificado.DatosCertificado.class);
        verify(generadorPdf).generar(datos.capture());
        assertEquals("https://sigea-team.github.io/sigea-front/#/verificar/" + c.getCodigoVerificacion(),
                datos.getValue().urlVerificacion());
        verify(certificadoRepository).bloquearEmision(10L, 1L);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CERTIFICADO_GENERADO), eq("certificados"),
                eq(77L), anyMap());
    }

    @Test
    @DisplayName("Plantilla: el PDF lleva el fondo y las firmas del evento; la plantilla se bloquea antes que la persona")
    void usaLaPlantillaDelEvento() {
        byte[] fondo = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        var firmas = List.of(new GeneradorPdfCertificado.Firma("Judith Rodríguez", "Directora de programa", new byte[]{1}));
        when(plantillaService.cargarParaPdf(1L)).thenReturn(new PlantillaCertificadoService.PlantillaPdf(fondo, firmas));
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 1, 1, 60)));

        servicio.solicitar(1L, TipoCertificado.asistente);

        ArgumentCaptor<GeneradorPdfCertificado.DatosCertificado> datos =
                ArgumentCaptor.forClass(GeneradorPdfCertificado.DatosCertificado.class);
        verify(generadorPdf).generar(datos.capture());
        assertArrayEquals(fondo, datos.getValue().fondoJpeg());
        assertEquals("Judith Rodríguez", datos.getValue().firmas().get(0).nombre());
        InOrder orden = inOrder(certificadoRepository);
        orden.verify(certificadoRepository).bloquearPlantillaParaEmitir(1L);
        orden.verify(certificadoRepository).bloquearEmision(10L, 1L);
        orden.verify(certificadoRepository).saveAndFlush(any(Certificado.class));
    }

    @Test
    @DisplayName("Criterio 1: quien tiene actividades realizadas a su cargo recibe el certificado de ponente")
    void emiteCertificadoDePonente() {
        when(certificadoRepository.actividadesComoPonente(1L, 10L)).thenReturn(List.of(
                actividad(5L, "Taller de Spring", 8, 10), actividad(6L, "Panel de IA", 10, 11)));

        EmisionCertificadoResponse respuesta = servicio.solicitar(1L, TipoCertificado.ponente);

        assertTrue(respuesta.generadoAhora());
        assertEquals(TipoCertificado.ponente, respuesta.certificado().tipo());
        assertNull(respuesta.certificado().porcentajeAsistencia());
        assertEquals("A cargo de las actividades «Taller de Spring» y «Panel de IA», con una intensidad de 3 horas.",
                respuesta.certificado().detalle());
    }

    @Test
    @DisplayName("Criterio 1: quien está registrado como conferencista del evento recibe el certificado de conferencista")
    void emiteCertificadoDeConferencista() {
        when(participacionRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                .thenReturn(Optional.of(participacion(TipoParticipacionConferencista.conferencista)));
        when(certificadoRepository.actividadesComoPonente(1L, 10L))
                .thenReturn(List.of(actividad(5L, "Conferencia central", 9, 10)));

        EmisionCertificadoResponse respuesta = servicio.solicitar(1L, TipoCertificado.conferencista);

        assertEquals(TipoCertificado.conferencista, respuesta.certificado().tipo());
        assertEquals("A cargo de la actividad «Conferencia central», con una intensidad de 1 hora.",
                respuesta.certificado().detalle());
    }

    @Test
    @DisplayName("Criterio 1: el evento publicado cuya fecha de fin ya pasó cuenta como terminado")
    void eventoVencidoSinCambioDeEstado() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 2, 2, 120)));

        assertTrue(servicio.solicitar(1L, TipoCertificado.asistente).generadoAhora());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 2: no cumple los requisitos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 2: por debajo del mínimo informa el motivo y no emite el certificado")
    void noCumpleElPorcentaje() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 7, 3, 180)));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));

        assertEquals(CertificadoException.REQUISITOS_NO_CUMPLIDOS, ex.getCodigo());
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("No cumples los requisitos para el certificado de asistente. Asistió a 3 de las 7 actividades en "
                + "las que se inscribió (42,85 %); el mínimo es 80 %.", ex.getMessage());
        verify(certificadoRepository, never()).saveAndFlush(any());
        verify(almacenamiento, never()).guardar(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Criterio 2: el porcentaje mínimo es el configurado en el evento")
    void usaElPorcentajeDelEvento() {
        evento.setPorcentajeMinimoAsistencia(new BigDecimal("60.00"));
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 5, 3, 0)));

        assertTrue(servicio.solicitar(1L, TipoCertificado.asistente).generadoAhora());
    }

    @Test
    @DisplayName("Criterio 2: sin inscripción al evento o sin actividades inscritas no se emite")
    void sinInscripcionOSinActividades() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.empty());
        CertificadoException sinInscripcion = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));
        assertTrue(sinInscripcion.getMessage().contains("No tiene inscripción en el evento."));

        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 0, 0, 0)));
        CertificadoException sinActividades = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));
        assertTrue(sinActividades.getMessage().contains("ninguna actividad realizada"));
        verify(certificadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 2: el ponente sin actividades realizadas a su cargo no recibe certificado")
    void ponenteSinActividades() {
        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.ponente));

        assertEquals(CertificadoException.REQUISITOS_NO_CUMPLIDOS, ex.getCodigo());
        assertTrue(ex.getMessage().contains("No tiene actividades realizadas a su cargo"));
    }

    @Test
    @DisplayName("Criterio 2: un conferencista no puede pedir el certificado de ponente (corresponde otro rol)")
    void rolDistintoAlRegistrado() {
        when(participacionRepository.findByEvento_IdAndPersona_Id(1L, 10L))
                .thenReturn(Optional.of(participacion(TipoParticipacionConferencista.conferencista)));
        when(certificadoRepository.actividadesComoPonente(1L, 10L))
                .thenReturn(List.of(actividad(5L, "Conferencia central", 9, 10)));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.ponente));

        assertTrue(ex.getMessage().contains("registrada como conferencista"));
    }

    @Test
    @DisplayName("El evento que no ha terminado no emite certificados (la asistencia aún puede cambiar)")
    void eventoNoFinalizado() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        evento.setFechaFin(AHORA.toLocalDate());
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 2, 2, 0)));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));

        assertEquals(CertificadoException.EVENTO_NO_FINALIZADO, ex.getCodigo());
        verify(certificadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Los certificados de evaluador y organizador todavía no se emiten")
    void tipoNoDisponible() {
        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.evaluador));

        assertEquals(CertificadoException.TIPO_NO_DISPONIBLE, ex.getCodigo());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Criterio 3: ya generado
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Criterio 3: si ya fue generado se entrega el mismo, sin crear otro registro ni otro PDF")
    void entregaElMismo() {
        Certificado existente = certificado(55L, TipoCertificado.asistente);
        when(certificadoRepository.findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(10L, 1L,
                TipoCertificado.asistente)).thenReturn(Optional.of(existente));

        EmisionCertificadoResponse respuesta = servicio.solicitar(1L, TipoCertificado.asistente);

        assertFalse(respuesta.generadoAhora());
        assertEquals(55L, respuesta.certificado().certificadoId());
        assertEquals("CERT-AAAABBBBCCCCDDDDEEEEFFFF", respuesta.certificado().codigoVerificacion());
        verify(certificadoRepository, never()).saveAndFlush(any());
        verify(generadorPdf, never()).generar(any());
        verify(auditoriaService, never()).registrar(any(), anyString(), anyLong(), anyMap());
    }

    @Test
    @DisplayName("Criterio 3: con el evento archivado (HU-28) se sigue entregando el certificado ya emitido")
    void archivadoEntregaElExistente() {
        Evento archivado = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.cerrado)
                .fechaInicio(evento.getFechaInicio()).fechaFin(evento.getFechaFin()).memoriasHistoricas(1L).build();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(archivado));
        when(certificadoRepository.findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(10L, 1L,
                TipoCertificado.asistente)).thenReturn(Optional.of(certificado(55L, TipoCertificado.asistente)));

        assertFalse(servicio.solicitar(1L, TipoCertificado.asistente).generadoAhora());
    }

    @Test
    @DisplayName("HU-28: con el evento archivado no se emiten certificados nuevos")
    void archivadoNoEmiteNuevos() {
        Evento archivado = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.cerrado)
                .fechaInicio(evento.getFechaInicio()).fechaFin(evento.getFechaFin()).memoriasHistoricas(1L).build();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(archivado));

        assertThrows(EventoArchivadoException.class, () -> servicio.solicitar(1L, TipoCertificado.asistente));
        verify(certificadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: si otra solicitud lo emitió al mismo tiempo, responde 409 sin duplicar")
    void emisionSimultanea() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 1, 1, 60)));
        when(certificadoRepository.saveAndFlush(any(Certificado.class)))
                .thenThrow(new DataIntegrityViolationException("ux_certificados_persona_evento_tipo"));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));

        assertEquals(CertificadoException.EMISION_SIMULTANEA, ex.getCodigo());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Consulta del participante
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Mis certificados: muestra cada rol con su estado y el certificado ya emitido")
    void consultarMios() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 4, 1, 60)));
        when(certificadoRepository.actividadesComoPonente(1L, 10L))
                .thenReturn(List.of(actividad(5L, "Taller de Spring", 8, 10)));
        when(certificadoRepository.findByPersona_IdAndEvento_IdOrderByFechaGeneracionAscIdAsc(10L, 1L))
                .thenReturn(List.of(certificado(55L, TipoCertificado.ponente)));

        MisCertificadosEventoResponse respuesta = servicio.consultarMios(1L);

        assertTrue(respuesta.eventoFinalizado());
        assertEquals(new BigDecimal("80.00"), respuesta.porcentajeMinimoAsistencia());
        assertEquals(2, respuesta.opciones().size());
        OpcionCertificadoResponse asistente = respuesta.opciones().get(0);
        assertEquals(TipoCertificado.asistente, asistente.tipo());
        assertFalse(asistente.cumpleRequisitos());
        assertFalse(asistente.puedeGenerar());
        assertEquals(new BigDecimal("25.00"), asistente.porcentajeAsistencia());
        assertTrue(asistente.motivo().contains("el mínimo es 80 %"));
        OpcionCertificadoResponse ponente = respuesta.opciones().get(1);
        assertEquals(TipoCertificado.ponente, ponente.tipo());
        assertTrue(ponente.cumpleRequisitos());
        assertFalse(ponente.puedeGenerar());
        assertEquals(55L, ponente.certificado().certificadoId());
        assertEquals(List.of("Taller de Spring"), ponente.actividadesACargo());
    }

    @Test
    @DisplayName("Mis certificados: antes de terminar el evento indica cuándo estará disponible")
    void consultarMiosAntesDeTerminar() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        evento.setFechaFin(AHORA.toLocalDate().plusDays(1));
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 2, 2, 0)));

        OpcionCertificadoResponse opcion = servicio.consultarMios(1L).opciones().get(0);

        assertTrue(opcion.cumpleRequisitos());
        assertFalse(opcion.puedeGenerar());
        assertEquals("Disponible cuando termine el evento (25/11/2026).", opcion.motivo());
    }

    @Test
    @DisplayName("Mis certificados: sin participación en el evento no hay opciones")
    void consultarMiosSinParticipacion() {
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.empty());

        assertTrue(servicio.consultarMios(1L).opciones().isEmpty());
    }

    @Test
    @DisplayName("Mis certificados por evento: solo eventos terminados en los que participó, del más reciente al más antiguo")
    void listarMisEventos() {
        Evento anterior = Evento.builder().id(2L).nombre("Congreso 2025").estado(EstadoEvento.cerrado)
                .fechaInicio(LocalDate.of(2025, 5, 1)).fechaFin(LocalDate.of(2025, 5, 3)).build();
        Evento enCurso = Evento.builder().id(3L).nombre("Feria").estado(EstadoEvento.en_ejecucion)
                .fechaInicio(AHORA.toLocalDate()).fechaFin(AHORA.toLocalDate().plusDays(2)).build();
        Evento sinRol = Evento.builder().id(4L).nombre("Foro").estado(EstadoEvento.cerrado)
                .fechaInicio(LocalDate.of(2026, 1, 1)).fechaFin(LocalDate.of(2026, 1, 2)).build();
        when(certificadoRepository.eventosDeLaPersona(10L)).thenReturn(List.of(2L, 1L, 3L, 4L));
        when(eventoRepository.findAllById(List.of(2L, 1L, 3L, 4L))).thenReturn(List.of(anterior, evento, enCurso, sinRol));
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 2, 2, 60)));
        when(certificadoRepository.resumenAsistencia(2L, 10L)).thenReturn(Optional.of(resumen(10L, 4, 1, 60)));
        when(certificadoRepository.resumenAsistencia(4L, 10L)).thenReturn(Optional.empty());

        List<MisCertificadosEventoResponse> lista = servicio.listarMisEventos();

        assertEquals(List.of(1L, 2L), lista.stream().map(MisCertificadosEventoResponse::eventoId).toList());
        assertTrue(lista.get(0).opciones().get(0).cumpleRequisitos());
        assertFalse(lista.get(1).opciones().get(0).cumpleRequisitos());
        assertTrue(lista.get(1).opciones().get(0).motivo().contains("el mínimo es 80 %"));
    }

    @Test
    @DisplayName("Mis certificados por evento: sin participación devuelve una lista vacía")
    void listarMisEventosVacio() {
        when(certificadoRepository.eventosDeLaPersona(10L)).thenReturn(List.of());

        assertTrue(servicio.listarMisEventos().isEmpty());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Descarga y verificación
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Descarga: el titular recibe el PDF guardado")
    void descargaDelTitular() {
        Certificado c = certificado(55L, TipoCertificado.asistente);
        when(certificadoRepository.findWithEventoAndPersonaById(55L)).thenReturn(Optional.of(c));
        when(almacenamiento.cargar("certificados/1/x.pdf")).thenReturn(new ByteArrayResource(PDF));

        CertificadoService.ArchivoCertificado archivo = servicio.descargar(55L);

        assertArrayEquals(PDF, archivo.contenido());
        assertEquals("certificado-asistente-CERT-AAAABBBBCCCCDDDDEEEEFFFF.pdf", archivo.nombre());
        verify(generadorPdf, never()).generar(any());
    }

    @Test
    @DisplayName("Criterio 3: si el archivo se perdió, el PDF se reconstruye con los datos guardados sin tocar el registro")
    void descargaReconstruye() {
        Certificado c = certificado(55L, TipoCertificado.asistente);
        when(certificadoRepository.findWithEventoAndPersonaById(55L)).thenReturn(Optional.of(c));
        when(almacenamiento.cargar(anyString())).thenThrow(new RecursoNoEncontradoException("no está"));

        assertArrayEquals(PDF, servicio.descargar(55L).contenido());

        ArgumentCaptor<GeneradorPdfCertificado.DatosCertificado> datos =
                ArgumentCaptor.forClass(GeneradorPdfCertificado.DatosCertificado.class);
        verify(generadorPdf).generar(datos.capture());
        assertEquals("Ana María Pérez", datos.getValue().nombreParticipante());
        assertEquals("CERT-AAAABBBBCCCCDDDDEEEEFFFF", datos.getValue().codigoVerificacion());
        assertEquals(LocalDateTime.of(2026, 11, 20, 9, 0), datos.getValue().fechaGeneracion());
        verify(certificadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Descarga: el certificado de otra persona → 403; con CERTIFICADOS_EMITIR sí se descarga")
    void descargaDeOtraPersona() {
        Certificado c = certificado(55L, TipoCertificado.asistente);
        c.setPersona(Persona.builder().id(99L).build());
        when(certificadoRepository.findWithEventoAndPersonaById(55L)).thenReturn(Optional.of(c));
        when(almacenamiento.cargar(anyString())).thenReturn(new ByteArrayResource(PDF));

        assertThrows(AccessDeniedException.class, () -> servicio.descargar(55L));

        autenticar("organizador@ufps.edu.co", "CERTIFICADOS_EMITIR");
        assertArrayEquals(PDF, servicio.descargar(55L).contenido());
    }

    @Test
    @DisplayName("Verificación pública: devuelve el certificado con el documento parcialmente oculto")
    void verificar() {
        when(certificadoRepository.findByCodigoVerificacion("CERT-AAAABBBBCCCCDDDDEEEEFFFF"))
                .thenReturn(Optional.of(certificado(55L, TipoCertificado.asistente)));

        VerificacionCertificadoResponse r = servicio.verificar("  cert-aaaabbbbccccddddeeeeffff ");

        assertEquals("Ana María Pérez", r.nombreParticipante());
        assertEquals("CC ******3456", r.documento());
        assertEquals(TipoCertificado.asistente, r.tipo());
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.verificar("CERT-NOEXISTE"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Organizador (CU-23)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CU-23: genera los del evento, no duplica los existentes y excluye con motivo a quien no cumple")
    void generarDelEvento() {
        Persona beto = Persona.builder().id(11L).nombres("Beto").apellidos("Bajo").numeroDocumento("2").build();
        Persona carla = Persona.builder().id(12L).nombres("Carla").apellidos("Conferencista").numeroDocumento("3").build();
        Persona dario = Persona.builder().id(13L).nombres("Darío").apellidos("Ponente").numeroDocumento("4").build();
        when(certificadoRepository.resumenAsistenciaDelEvento(1L)).thenReturn(List.of(
                resumen(10L, 4, 4, 240), resumen(11L, 4, 1, 60)));
        Actividad conferencia = actividad(5L, "Conferencia central", 9, 10);
        conferencia.setPonente(carla);
        Actividad taller = actividad(6L, "Taller", 10, 12);
        taller.setPonente(dario);
        when(certificadoRepository.actividadesConPonente(1L)).thenReturn(List.of(conferencia, taller));
        ParticipacionConferencista pc = participacion(TipoParticipacionConferencista.conferencista);
        pc.setPersona(carla);
        when(participacionRepository.findByEvento_IdOrderByFechaRegistroAscIdAsc(1L)).thenReturn(List.of(pc));
        when(personaRepository.findAllById(any())).thenReturn(List.of(ana, beto, carla, dario));
        when(certificadoRepository.findByPersona_IdAndEvento_IdAndTipoAndActividadIsNull(13L, 1L, TipoCertificado.ponente))
                .thenReturn(Optional.of(certificado(60L, TipoCertificado.ponente)));

        GeneracionCertificadosResponse r = servicio.generarDelEvento(1L);

        assertEquals(2, r.generados().size());
        assertEquals(TipoCertificado.asistente, r.generados().get(0).tipo());
        assertEquals(TipoCertificado.conferencista, r.generados().get(1).tipo());
        assertEquals(1, r.yaEmitidos());
        assertEquals(1, r.excluidos().size());
        assertEquals(11L, r.excluidos().get(0).personaId());
        assertTrue(r.excluidos().get(0).motivo().contains("Asistió a 1 de las 4 actividades"));
        verify(certificadoRepository, times(2)).saveAndFlush(any());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CERTIFICADOS_GENERADOS_EVENTO), eq("eventos"),
                eq(1L), anyMap());
    }

    @Test
    @DisplayName("CU-23: no se generan los del evento antes de que termine")
    void generarDelEventoNoFinalizado() {
        evento.setEstado(EstadoEvento.habilitado);
        evento.setFechaFin(AHORA.toLocalDate().plusDays(3));

        CertificadoException ex = assertThrows(CertificadoException.class, () -> servicio.generarDelEvento(1L));
        assertEquals(CertificadoException.EVENTO_NO_FINALIZADO, ex.getCodigo());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Configuración (RN-01)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("RN-01: el porcentaje mínimo se cambia mientras no haya certificados de asistente")
    void actualizarConfiguracion() {
        when(plantillaService.fechaHabilitacion(1L)).thenReturn(Optional.empty());
        when(certificadoRepository.countByEvento_IdAndTipo(1L, TipoCertificado.asistente)).thenReturn(0L);

        ConfiguracionCertificadosResponse r = servicio.actualizarConfiguracion(1L, new BigDecimal("75"));

        assertEquals(new BigDecimal("75.00"), evento.getPorcentajeMinimoAsistencia());
        assertEquals(new BigDecimal("75.00"), r.porcentajeMinimoAsistencia());
        assertTrue(r.editable());
        verify(eventoRepository).save(evento);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CONFIGURACION_CERTIFICADOS_ACTUALIZADA),
                eq("eventos"), eq(1L), anyMap());
        // Serializado con «Habilitar certificados»: mismo bloqueo exclusivo, tomado antes de leer el evento.
        InOrder orden = inOrder(certificadoRepository, eventoRepository);
        orden.verify(certificadoRepository).bloquearPlantilla(1L);
        orden.verify(eventoRepository).findById(1L);
        orden.verify(eventoRepository).save(evento);
    }

    @Test
    @DisplayName("RN-01: con certificados de asistente emitidos el porcentaje ya no se cambia")
    void configuracionBloqueada() {
        when(certificadoRepository.countByEvento_IdAndTipo(1L, TipoCertificado.asistente)).thenReturn(4L);

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.actualizarConfiguracion(1L, new BigDecimal("70")));

        assertEquals(CertificadoException.CONFIGURACION_BLOQUEADA, ex.getCodigo());
        assertEquals(new BigDecimal("80.00"), evento.getPorcentajeMinimoAsistencia());
        verify(eventoRepository, never()).save(any());

        ConfiguracionCertificadosResponse consulta = servicio.consultarConfiguracion(1L);
        assertFalse(consulta.editable());
        assertNotNull(consulta.motivoNoEditable());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Habilitación
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sin habilitar: el participante no puede generar su certificado aunque cumpla")
    void noHabilitadosParticipante() {
        when(plantillaService.fechaHabilitacion(1L)).thenReturn(Optional.empty());
        when(certificadoRepository.resumenAsistencia(1L, 10L)).thenReturn(Optional.of(resumen(10L, 2, 2, 120)));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.solicitar(1L, TipoCertificado.asistente));

        assertEquals(CertificadoException.NO_HABILITADOS, ex.getCodigo());
        verify(certificadoRepository, never()).saveAndFlush(any());
        OpcionCertificadoResponse opcion = servicio.consultarMios(1L).opciones().get(0);
        assertTrue(opcion.cumpleRequisitos());
        assertFalse(opcion.puedeGenerar());
        assertEquals(CertificadoService.MENSAJE_NO_HABILITADOS, opcion.motivo());
        assertFalse(servicio.consultarMios(1L).certificadosHabilitados());
    }

    @Test
    @DisplayName("Sin habilitar: la generación del evento tampoco está disponible")
    void noHabilitadosOrganizador() {
        when(plantillaService.fechaHabilitacion(1L)).thenReturn(Optional.empty());

        CertificadoException ex = assertThrows(CertificadoException.class, () -> servicio.generarDelEvento(1L));
        assertEquals(CertificadoException.NO_HABILITADOS, ex.getCodigo());
    }

    @Test
    @DisplayName("Habilitar: deja la plantilla fija y genera los certificados de quienes cumplen")
    void habilitar() {
        LocalDateTime fecha = AHORA;
        when(plantillaService.fechaHabilitacion(1L)).thenReturn(Optional.empty());
        when(plantillaService.habilitar(evento)).thenReturn(fecha);
        when(certificadoRepository.resumenAsistenciaDelEvento(1L)).thenReturn(List.of(resumen(10L, 2, 2, 120)));
        when(personaRepository.findAllById(any())).thenReturn(List.of(ana));

        HabilitacionCertificadosResponse r = servicio.habilitar(1L);

        assertEquals(fecha, r.fechaHabilitacion());
        assertEquals(1, r.generados().size());
        assertEquals("Ana María Pérez", r.generados().get(0).nombreParticipante());
        InOrder orden = inOrder(plantillaService, certificadoRepository);
        orden.verify(plantillaService).habilitar(evento);
        orden.verify(certificadoRepository).saveAndFlush(any(Certificado.class));
        // El bloqueo exclusivo se toma antes de leer el evento (carrera con el cambio de porcentaje).
        InOrder lectura = inOrder(certificadoRepository, eventoRepository);
        lectura.verify(certificadoRepository).bloquearPlantilla(1L);
        lectura.verify(eventoRepository).findById(1L);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CERTIFICADOS_HABILITADOS), eq("eventos"),
                eq(1L), anyMap());
    }

    @Test
    @DisplayName("Habilitar: el evento debe haber terminado; la plantilla incompleta lo impide")
    void habilitarRestricciones() {
        evento.setEstado(EstadoEvento.en_ejecucion);
        evento.setFechaFin(AHORA.toLocalDate());
        CertificadoException noTermino = assertThrows(CertificadoException.class, () -> servicio.habilitar(1L));
        assertEquals(CertificadoException.EVENTO_NO_FINALIZADO, noTermino.getCodigo());
        verify(plantillaService, never()).habilitar(any());

        evento.setEstado(EstadoEvento.cerrado);
        when(plantillaService.habilitar(evento)).thenThrow(new CertificadoException(
                CertificadoException.PLANTILLA_INCOMPLETA, HttpStatus.CONFLICT, "Falta el fondo."));
        CertificadoException incompleta = assertThrows(CertificadoException.class, () -> servicio.habilitar(1L));
        assertEquals(CertificadoException.PLANTILLA_INCOMPLETA, incompleta.getCodigo());
        verify(certificadoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("RN-01: con los certificados habilitados el porcentaje ya no se cambia")
    void porcentajeBloqueadoAlHabilitar() {
        when(certificadoRepository.countByEvento_IdAndTipo(1L, TipoCertificado.asistente)).thenReturn(0L);

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.actualizarConfiguracion(1L, new BigDecimal("70")));
        assertEquals(CertificadoException.CONFIGURACION_BLOQUEADA, ex.getCodigo());
        assertFalse(servicio.consultarConfiguracion(1L).editable());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Textos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("URL de verificación: base del frontend + código; sin base, el PDF solo muestra el código")
    void urlVerificacion() {
        assertEquals("http://localhost:5173/#/verificar/CERT-1", CertificadoService.urlVerificacion("http://localhost:5173/#/verificar/", "CERT-1"));
        assertEquals("https://x/#/verificar/CERT-1", CertificadoService.urlVerificacion(" https://x/#/verificar ", "CERT-1"));
        assertNull(CertificadoService.urlVerificacion(" ", "CERT-1"));
        assertNull(CertificadoService.urlVerificacion(null, "CERT-1"));
    }

    @Test
    @DisplayName("Textos del detalle impreso y del documento oculto")
    void textos() {
        assertEquals("Asistió a la actividad en la que se inscribió (100 %), con una intensidad de 45 minutos.",
                CertificadoService.detalleAsistencia(1, 1, new BigDecimal("100.00"), 45));
        assertEquals("Asistió a las 3 actividades en las que se inscribió (100 %), con una intensidad de 4,5 horas.",
                CertificadoService.detalleAsistencia(3, 3, new BigDecimal("100.00"), 270));
        assertEquals("A cargo de las actividades «A», «B» y «C».",
                CertificadoService.detalleExpositor(List.of("A", "B", "C"), 0));
        assertEquals("CC ******3456", CertificadoService.documentoOculto("cc", "1090123456"));
        assertEquals("**3", CertificadoService.documentoOculto(null, "123"));
        assertNull(CertificadoService.documentoOculto("CC", " "));
    }

    @Test
    @DisplayName("Documento oculto en la verificación pública: casos borde")
    void documentoOcultoCasosBorde() {
        // Sin número no se muestra nada.
        assertNull(CertificadoService.documentoOculto("CC", null));
        // Con 1 o 2 caracteres se oculta todo: mostrar dígitos revelaría el documento completo.
        assertEquals("CC *", CertificadoService.documentoOculto("CC", "1"));
        assertEquals("CC **", CertificadoService.documentoOculto("CC", "12"));
        // Se dejan visibles a lo sumo los 4 últimos y siempre se ocultan al menos 2.
        assertEquals("CC **345", CertificadoService.documentoOculto("CC", "12345"));
        // Los espacios alrededor del tipo y del número no cuentan; el tipo va en mayúsculas.
        assertEquals("CC ******3456", CertificadoService.documentoOculto(" cc ", " 1090123456 "));
        // Sin tipo de documento solo se muestra el número oculto.
        assertEquals("****4455", CertificadoService.documentoOculto(null, "E-334455"));
        assertEquals("****4567", CertificadoService.documentoOculto("", "88234567"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────

    private static void autenticar(String correo, String autoridad) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                correo, "n/a", List.of(new SimpleGrantedAuthority(autoridad))));
    }

    private static ResumenAsistencia resumen(Long personaId, long consideradas, long asistidas, long minutos) {
        return new ResumenAsistencia() {
            @Override public Long getPersonaId() { return personaId; }
            @Override public Long getConsideradas() { return consideradas; }
            @Override public Long getAsistidas() { return asistidas; }
            @Override public Long getMinutosAsistidos() { return minutos; }
        };
    }

    private Actividad actividad(Long id, String nombre, int desde, int hasta) {
        return Actividad.builder().id(id).evento(evento).nombre(nombre).fecha(LocalDate.of(2026, 11, 10))
                .horaInicio(LocalTime.of(desde, 0)).horaFin(LocalTime.of(hasta, 0)).estado(Actividad.ESTADO_PROGRAMADA)
                .build();
    }

    private ParticipacionConferencista participacion(TipoParticipacionConferencista tipo) {
        return ParticipacionConferencista.builder().id(3L).evento(evento).persona(ana).tipoParticipacion(tipo).build();
    }

    private Certificado certificado(Long id, TipoCertificado tipo) {
        return Certificado.builder().id(id).persona(ana).evento(evento).tipo(tipo)
                .porcentajeAsistencia(tipo == TipoCertificado.asistente ? new BigDecimal("100.00") : null)
                .fechaGeneracion(LocalDateTime.of(2026, 11, 20, 9, 0)).archivoUrl("certificados/1/x.pdf")
                .codigoVerificacion("CERT-AAAABBBBCCCCDDDDEEEEFFFF").nombreParticipante("Ana María Pérez")
                .tipoDocumento("CC").numeroDocumento("1090123456").nombreEvento("Semana de Ingeniería")
                .fechaInicioEvento(LocalDate.of(2026, 11, 10)).fechaFinEvento(LocalDate.of(2026, 11, 12))
                .detalle("Asistió a las 2 actividades en las que se inscribió (100 %).").build();
    }
}
