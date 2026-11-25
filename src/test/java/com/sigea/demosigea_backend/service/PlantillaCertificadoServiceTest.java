package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.certificado.PlantillaCertificadoResponse;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.EventoArchivadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.FirmanteCertificado;
import com.sigea.demosigea_backend.model.PlantillaCertificado;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.CertificadoRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.FirmanteCertificadoRepository;
import com.sigea.demosigea_backend.repository.PlantillaCertificadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;

import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link PlantillaCertificadoService} (HU-26: fondo y firmantes de los certificados).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlantillaCertificadoServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 24, 19, 0);
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 1};

    @Mock private PlantillaCertificadoRepository plantillaRepository;
    @Mock private FirmanteCertificadoRepository firmanteRepository;
    @Mock private CertificadoRepository certificadoRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private ProcesadorImagenesCertificado procesador;
    @Mock private GeneradorPdfCertificado generadorPdf;
    @Mock private AlmacenamientoArchivos almacenamiento;
    @Mock private AuditoriaService auditoriaService;

    private PlantillaCertificadoService servicio;
    private Evento evento;
    private final List<FirmanteCertificado> firmantes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        servicio = new PlantillaCertificadoService(plantillaRepository, firmanteRepository, certificadoRepository,
                eventoRepository, procesador, generadorPdf, almacenamiento, auditoriaService,
                Clock.fixed(AHORA.atZone(BOGOTA).toInstant(), BOGOTA));
        evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.habilitado)
                .fechaInicio(LocalDate.of(2026, 12, 1)).fechaFin(LocalDate.of(2026, 12, 3)).build();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(certificadoRepository.countByEvento_Id(1L)).thenReturn(0L);
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.empty());
        when(firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(1L)).thenAnswer(inv -> new ArrayList<>(firmantes));
        when(procesador.normalizarFondo(any())).thenReturn(JPEG);
        when(procesador.normalizarFirma(any())).thenReturn(PNG);
        when(almacenamiento.guardar(anyString(), anyString(), any(InputStream.class)))
                .thenReturn("certificados/1/plantilla/nuevo.jpg");
        when(plantillaRepository.save(any(PlantillaCertificado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(firmanteRepository.save(any(FirmanteCertificado.class))).thenAnswer(inv -> {
            FirmanteCertificado f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(100L + firmantes.size());
            }
            return f;
        });
    }

    private FirmanteCertificado firmante(long id, int orden) {
        FirmanteCertificado f = FirmanteCertificado.builder().id(id).evento(evento).nombre("Firmante " + id)
                .cargo("Cargo").firmaUrl("firma-" + id + ".png").orden(orden).fechaRegistro(AHORA).build();
        firmantes.add(f);
        when(firmanteRepository.findByIdAndEvento_Id(id, 1L)).thenReturn(Optional.of(f));
        return f;
    }

    @Test
    @DisplayName("Consulta: sin certificados la plantilla es editable y trae las rutas de las imágenes")
    void consultar() {
        firmante(4L, 1);
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.of(
                PlantillaCertificado.builder().id(9L).evento(evento).fondoUrl("f.jpg").fechaActualizacion(AHORA).build()));

        PlantillaCertificadoResponse r = servicio.consultar(1L);

        assertTrue(r.editable());
        assertTrue(r.tieneFondo());
        assertEquals("/api/v1/eventos/1/certificados/plantilla/fondo", r.urlFondo());
        assertEquals("/api/v1/eventos/1/certificados/plantilla/firmantes/4/firma", r.firmantes().get(0).urlFirma());
        assertEquals(4, r.maximoFirmantes());
    }

    @Test
    @DisplayName("Fondo: se normaliza, se guarda y el anterior se elimina")
    void subirFondo() {
        PlantillaCertificado existente = PlantillaCertificado.builder().id(9L).evento(evento).fondoUrl("viejo.jpg")
                .fechaActualizacion(AHORA.minusDays(1)).build();
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.of(existente));

        servicio.subirFondo(1L, new byte[]{1, 2, 3});

        assertEquals("certificados/1/plantilla/nuevo.jpg", existente.getFondoUrl());
        assertEquals(AHORA, existente.getFechaActualizacion());
        verify(almacenamiento).guardar(eq("certificados/1/plantilla"), eq("jpg"), any(InputStream.class));
        verify(almacenamiento).eliminar("viejo.jpg");
        verify(certificadoRepository).bloquearPlantilla(1L);
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PLANTILLA_CERTIFICADO_ACTUALIZADA),
                eq("plantillas_certificado"), eq(9L), anyMap());
    }

    @Test
    @DisplayName("Con certificados emitidos la plantilla no se modifica: todos los certificados deben ser iguales")
    void bloqueadaConCertificados() {
        when(certificadoRepository.countByEvento_Id(1L)).thenReturn(3L);

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.subirFondo(1L, new byte[]{1}));
        assertEquals(CertificadoException.PLANTILLA_BLOQUEADA, ex.getCodigo());
        assertThrows(CertificadoException.class, () -> servicio.agregarFirmante(1L, "A", "B", new byte[]{1}));
        assertThrows(CertificadoException.class, () -> servicio.eliminarFondo(1L));

        verify(procesador, never()).normalizarFondo(any());
        verify(almacenamiento, never()).guardar(anyString(), anyString(), any());
        PlantillaCertificadoResponse consulta = servicio.consultar(1L);
        assertFalse(consulta.editable());
        assertTrue(consulta.motivoNoEditable().contains("3 certificados"));
    }

    @Test
    @DisplayName("Habilitar: exige fondo y al menos una firma; deja la plantilla fija con la fecha")
    void habilitar() {
        CertificadoException sinNada = assertThrows(CertificadoException.class, () -> servicio.habilitar(evento));
        assertEquals(CertificadoException.PLANTILLA_INCOMPLETA, sinNada.getCodigo());
        assertTrue(sinNada.getMessage().contains("fondo") && sinNada.getMessage().contains("firmante"));

        PlantillaCertificado plantilla = PlantillaCertificado.builder().id(9L).evento(evento).fondoUrl("f.jpg")
                .fechaActualizacion(AHORA).build();
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.of(plantilla));
        CertificadoException sinFirma = assertThrows(CertificadoException.class, () -> servicio.habilitar(evento));
        assertFalse(sinFirma.getMessage().contains("fondo"));

        firmante(4L, 1);
        PlantillaCertificadoResponse antes = servicio.consultar(1L);
        assertTrue(antes.completa());
        assertTrue(antes.faltantes().isEmpty());

        assertEquals(AHORA, servicio.habilitar(evento));
        assertEquals(AHORA, plantilla.getFechaHabilitacion());
        verify(plantillaRepository).save(plantilla);
        verify(certificadoRepository, org.mockito.Mockito.atLeastOnce()).bloquearPlantilla(1L);

        CertificadoException otraVez = assertThrows(CertificadoException.class, () -> servicio.habilitar(evento));
        assertEquals(CertificadoException.YA_HABILITADOS, otraVez.getCodigo());
    }

    @Test
    @DisplayName("Habilitada: la plantilla ya no se modifica y la consulta lo indica")
    void habilitadaNoSeModifica() {
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.of(PlantillaCertificado.builder().id(9L)
                .evento(evento).fondoUrl("f.jpg").fechaActualizacion(AHORA).fechaHabilitacion(AHORA).build()));

        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.agregarFirmante(1L, "A", "B", new byte[]{1}));
        assertEquals(CertificadoException.PLANTILLA_BLOQUEADA, ex.getCodigo());
        assertTrue(ex.getMessage().contains("24/11/2026 19:00"));
        PlantillaCertificadoResponse r = servicio.consultar(1L);
        assertTrue(r.habilitada());
        assertFalse(r.editable());
        assertEquals(AHORA, r.fechaHabilitacion());
    }

    @Test
    @DisplayName("Consulta: sin fondo ni firmas indica lo que falta para habilitar")
    void faltantes() {
        PlantillaCertificadoResponse r = servicio.consultar(1L);
        assertFalse(r.completa());
        assertEquals(2, r.faltantes().size());
        assertFalse(r.habilitada());
    }

    @Test
    @DisplayName("HU-28: con el evento archivado la plantilla no se modifica")
    void archivado() {
        Evento archivado = Evento.builder().id(1L).nombre("Semana").estado(EstadoEvento.cerrado)
                .fechaInicio(LocalDate.of(2026, 11, 1)).fechaFin(LocalDate.of(2026, 11, 2)).memoriasHistoricas(1L).build();
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(archivado));

        assertThrows(EventoArchivadoException.class, () -> servicio.subirFondo(1L, new byte[]{1}));
        assertFalse(servicio.consultar(1L).editable());
    }

    @Test
    @DisplayName("Firmantes: se agregan al final, con nombre y cargo limpios; máximo 4")
    void agregarFirmante() {
        firmante(4L, 1);

        servicio.agregarFirmante(1L, "  Judith   Rodríguez ", " Directora ", new byte[]{1});

        ArgumentCaptor<FirmanteCertificado> captor = ArgumentCaptor.forClass(FirmanteCertificado.class);
        verify(firmanteRepository).save(captor.capture());
        assertEquals("Judith Rodríguez", captor.getValue().getNombre());
        assertEquals("Directora", captor.getValue().getCargo());
        assertEquals(2, captor.getValue().getOrden());
        verify(almacenamiento).guardar(eq("certificados/1/plantilla"), eq("png"), any(InputStream.class));

        firmante(5L, 2);
        firmante(6L, 3);
        firmante(7L, 4);
        CertificadoException ex = assertThrows(CertificadoException.class,
                () -> servicio.agregarFirmante(1L, "Quinto", "Cargo", new byte[]{1}));
        assertEquals(CertificadoException.MAXIMO_FIRMANTES, ex.getCodigo());
    }

    @Test
    @DisplayName("Firmantes: nombre o cargo vacíos o muy largos → 400")
    void textosInvalidos() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.agregarFirmante(1L, " ", "Cargo", new byte[]{1}));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.agregarFirmante(1L, "A", null, new byte[]{1}));
        assertThrows(SolicitudInvalidaException.class,
                () -> servicio.agregarFirmante(1L, "A".repeat(151), "Cargo", new byte[]{1}));
        verify(firmanteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Firmantes: al modificar sin imagen se conserva la firma; con imagen se reemplaza")
    void actualizarFirmante() {
        FirmanteCertificado f = firmante(4L, 1);

        servicio.actualizarFirmante(1L, 4L, "Nuevo nombre", "Nuevo cargo", null);
        assertEquals("Nuevo nombre", f.getNombre());
        assertEquals("firma-4.png", f.getFirmaUrl());
        verify(procesador, never()).normalizarFirma(any());

        servicio.actualizarFirmante(1L, 4L, "Nuevo nombre", "Nuevo cargo", new byte[]{1});
        assertEquals("certificados/1/plantilla/nuevo.jpg", f.getFirmaUrl());
        verify(almacenamiento).eliminar("firma-4.png");

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.actualizarFirmante(1L, 99L, "A", "B", null));
    }

    @Test
    @DisplayName("Firmantes: al quitar uno los demás se corren a la izquierda")
    void eliminarFirmante() {
        FirmanteCertificado primero = firmante(4L, 1);
        FirmanteCertificado segundo = firmante(5L, 2);
        FirmanteCertificado tercero = firmante(6L, 3);

        servicio.eliminarFirmante(1L, 5L);

        verify(firmanteRepository).delete(segundo);
        verify(almacenamiento).eliminar("firma-5.png");
        assertEquals(1, primero.getOrden());
        assertEquals(2, tercero.getOrden());
    }

    @Test
    @DisplayName("Firmantes: el nuevo orden debe incluir a todos, una vez")
    void reordenar() {
        FirmanteCertificado a = firmante(4L, 1);
        FirmanteCertificado b = firmante(5L, 2);

        servicio.reordenarFirmantes(1L, List.of(5L, 4L));
        assertEquals(2, a.getOrden());
        assertEquals(1, b.getOrden());

        assertThrows(SolicitudInvalidaException.class, () -> servicio.reordenarFirmantes(1L, List.of(5L)));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.reordenarFirmantes(1L, List.of(5L, 5L)));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.reordenarFirmantes(1L, List.of(5L, 99L)));
    }

    @Test
    @DisplayName("Emisión: carga el fondo y las firmas en orden; una imagen perdida se omite sin fallar")
    void cargarParaPdf() {
        firmante(4L, 1);
        firmante(5L, 2);
        when(plantillaRepository.findByEvento_Id(1L)).thenReturn(Optional.of(
                PlantillaCertificado.builder().id(9L).evento(evento).fondoUrl("f.jpg").fechaActualizacion(AHORA).build()));
        when(almacenamiento.cargar("f.jpg")).thenReturn(new ByteArrayResource(JPEG));
        when(almacenamiento.cargar("firma-4.png")).thenReturn(new ByteArrayResource(PNG));
        when(almacenamiento.cargar("firma-5.png")).thenThrow(new RecursoNoEncontradoException("no está"));

        PlantillaCertificadoService.PlantillaPdf p = servicio.cargarParaPdf(1L);

        assertArrayEquals(JPEG, p.fondoJpeg());
        assertEquals(2, p.firmas().size());
        assertEquals("Firmante 4", p.firmas().get(0).nombre());
        assertArrayEquals(PNG, p.firmas().get(0).imagenPng());
        assertNull(p.firmas().get(1).imagenPng());
    }

    @Test
    @DisplayName("Vista previa: usa la plantilla actual con datos de ejemplo")
    void vistaPrevia() {
        when(generadorPdf.generar(any())).thenReturn(new byte[]{'%'});

        servicio.vistaPrevia(1L, null);

        ArgumentCaptor<GeneradorPdfCertificado.DatosCertificado> datos =
                ArgumentCaptor.forClass(GeneradorPdfCertificado.DatosCertificado.class);
        verify(generadorPdf).generar(datos.capture());
        assertEquals("Nombre del participante", datos.getValue().nombreParticipante());
        assertEquals("Semana de Ingeniería", datos.getValue().nombreEvento());
        verify(certificadoRepository, never()).bloquearPlantilla(anyLong());
    }
}
