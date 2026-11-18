package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.asistencia.CodigoQrResponse;
import com.sigea.demosigea_backend.dto.asistencia.ReenvioQrResponse;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.CategoriaParticipante;
import com.sigea.demosigea_backend.model.CodigoQr;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.InscripcionEvento;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.repository.CodigoQrRepository;
import com.sigea.demosigea_backend.repository.InscripcionEventoRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de {@link CodigoQrService} (HU-24, Criterios 1 y 4).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CodigoQrServiceTest {

    private static final String CORREO = "maria.perez@ufps.edu.co";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G'};

    @Mock private CodigoQrRepository codigoQrRepository;
    @Mock private InscripcionEventoRepository inscripcionEventoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmailService emailService;
    @Mock private GeneradorImagenQr generadorImagenQr;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private CodigoQrService servicio;

    private Persona persona;
    private InscripcionEvento inscripcion;

    @BeforeEach
    void setUp() {
        Evento evento = Evento.builder().id(1L).nombre("Semana de Ingeniería").estado(EstadoEvento.habilitado).build();
        persona = Persona.builder().id(10L).nombres("María").apellidos("Pérez").correo(CORREO).build();
        inscripcion = InscripcionEvento.builder()
                .id(501L).evento(evento).persona(persona)
                .categoriaParticipante(CategoriaParticipante.estudiante)
                .fechaInscripcion(LocalDateTime.now())
                .build();

        when(codigoQrRepository.save(any(CodigoQr.class))).thenAnswer(inv -> {
            CodigoQr qr = inv.getArgument(0);
            qr.setId(77L);
            return qr;
        });
        when(generadorImagenQr.generarPng(anyString())).thenReturn(PNG);
        when(generadorImagenQr.generarPng(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(PNG);
        when(emailService.enviarCodigoQrInscripcion(anyString(), anyString(), anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(true);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                CORREO, "n/a", List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANTE"))));
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(CORREO))
                .thenReturn(Optional.of(Usuario.builder().id(100L).persona(persona).build()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private CodigoQr codigoExistente() {
        return CodigoQr.builder().id(77L).inscripcion(inscripcion)
                .codigo("SIGEA-11112222333344445555666677778888")
                .fechaGeneracion(LocalDateTime.now().minusDays(2)).vigente(true).build();
    }

    @Test
    @DisplayName("Criterio 1: genera un código único con prefijo SIGEA- y lo envía por correo con la imagen")
    void generaCodigoYLoEnvia() {
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.empty());
        when(codigoQrRepository.existsByCodigo(anyString())).thenReturn(false);

        String codigo = servicio.generarAlConfirmarInscripcion(inscripcion);

        assertTrue(codigo.matches("SIGEA-[0-9a-f]{32}"), codigo);
        ArgumentCaptor<CodigoQr> guardado = ArgumentCaptor.forClass(CodigoQr.class);
        verify(codigoQrRepository).save(guardado.capture());
        assertSame(inscripcion, guardado.getValue().getInscripcion());
        assertEquals(codigo, guardado.getValue().getCodigo());
        assertTrue(guardado.getValue().estaVigente());
        // Sin transacción activa el correo se envía de inmediato
        verify(emailService).enviarCodigoQrInscripcion(eq(CORREO), eq("María Pérez"), eq("Semana de Ingeniería"),
                eq(codigo), eq(PNG), eq(false));
    }

    @Test
    @DisplayName("Criterio 1: si la inscripción ya tiene código, se conserva y no se crea otro")
    void conservaCodigoExistente() {
        CodigoQr existente = codigoExistente();
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(existente));

        CodigoQr resultado = servicio.obtenerOCrear(inscripcion);

        assertSame(existente, resultado);
        verify(codigoQrRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 1: si el código generado ya existe, se genera otro")
    void reintentaSiElCodigoYaExiste() {
        when(codigoQrRepository.existsByCodigo(anyString())).thenReturn(true, false);

        String codigo = servicio.nuevoCodigoUnico();

        assertTrue(codigo.startsWith(CodigoQrService.PREFIJO));
        verify(codigoQrRepository, times(2)).existsByCodigo(anyString());
    }

    @Test
    @DisplayName("Criterio 1: los códigos generados no se repiten")
    void codigosNoSeRepiten() {
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            codigos.add(CodigoQrService.generarCodigo());
        }
        assertEquals(2000, codigos.size());
        assertTrue(codigos.stream().allMatch(c -> c.length() == 38 && c.length() <= 100));
    }

    @Test
    @DisplayName("Criterio 1: sin correo registrado se genera el código pero no se envía nada")
    void sinCorreoNoEnvia() {
        persona.setCorreo(null);
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.empty());

        servicio.generarAlConfirmarInscripcion(inscripcion);

        verify(codigoQrRepository).save(any());
        verify(emailService, never()).enviarCodigoQrInscripcion(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Consulta mi código QR")
    void consultaMiCodigo() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));

        CodigoQrResponse resp = servicio.consultarMiCodigo(1L);

        assertEquals("SIGEA-11112222333344445555666677778888", resp.codigo());
        assertEquals(501L, resp.inscripcionId());
        assertEquals(1L, resp.eventoId());
        assertEquals("María Pérez", resp.participanteNombre());
        assertTrue(resp.vigente());
    }

    @Test
    @DisplayName("Mi código QR: 404 si el usuario no está inscrito en el evento")
    void miCodigoSinInscripcion() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.consultarMiCodigo(1L));
    }

    @Test
    @DisplayName("Imagen de mi código QR con el tamaño pedido")
    void imagenMiCodigo() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));

        byte[] png = servicio.imagenMiCodigo(1L, 400);

        assertSame(PNG, png);
        verify(generadorImagenQr).generarPng("SIGEA-11112222333344445555666677778888", 400);
    }

    @Test
    @DisplayName("Criterio 4: el reenvío manda el mismo código, no genera uno distinto y queda auditado")
    void reenvioMismoCodigo() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));

        ReenvioQrResponse resp = servicio.reenviarMiCodigo(1L);

        assertEquals("SIGEA-11112222333344445555666677778888", resp.codigo());
        assertEquals(CORREO, resp.correoDestino());
        assertTrue(resp.enviado());
        verify(codigoQrRepository, never()).save(any());
        verify(emailService).enviarCodigoQrInscripcion(eq(CORREO), eq("María Pérez"), eq("Semana de Ingeniería"),
                eq("SIGEA-11112222333344445555666677778888"), eq(PNG), eq(true));
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.CODIGO_QR_REENVIADO), eq("codigos_qr"), eq(77L), any());
    }

    @Test
    @DisplayName("Criterio 4: dos reenvíos seguidos mandan el mismo código")
    void dosReenviosMismoCodigo() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));

        String primero = servicio.reenviarMiCodigo(1L).codigo();
        String segundo = servicio.reenviarMiCodigo(1L).codigo();

        assertEquals(primero, segundo);
        verify(codigoQrRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 4: si el correo falla, se informa sin cambiar el código")
    void reenvioFallido() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));
        when(emailService.enviarCodigoQrInscripcion(anyString(), anyString(), anyString(), anyString(), any(), anyBoolean()))
                .thenReturn(false);

        ReenvioQrResponse resp = servicio.reenviarMiCodigo(1L);

        assertFalse(resp.enviado());
        assertEquals("SIGEA-11112222333344445555666677778888", resp.codigo());
        assertTrue(resp.mensaje().contains("no cambió"));
    }

    @Test
    @DisplayName("Criterio 4: inscripción anterior a HU-24 sin código: el reenvío lo crea una sola vez")
    void reenvioCreaCodigoSiNoExiste() {
        when(inscripcionEventoRepository.findByEvento_IdAndPersona_Id(1L, 10L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.empty());

        ReenvioQrResponse resp = servicio.reenviarMiCodigo(1L);

        assertTrue(resp.codigo().startsWith(CodigoQrService.PREFIJO));
        verify(codigoQrRepository).save(any());
    }

    @Test
    @DisplayName("Criterio 4: sin correo registrado no se puede reenviar")
    void reenvioSinCorreo() {
        persona.setCorreo(" ");
        when(inscripcionEventoRepository.findByIdAndEvento_IdConPersona(501L, 1L)).thenReturn(Optional.of(inscripcion));

        assertThrows(OperacionNoPermitidaException.class, () -> servicio.reenviarCodigo(1L, 501L));
        verify(emailService, never()).enviarCodigoQrInscripcion(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Criterio 4: el organizador reenvía el QR de una inscripción del evento")
    void organizadorReenvia() {
        when(inscripcionEventoRepository.findByIdAndEvento_IdConPersona(501L, 1L)).thenReturn(Optional.of(inscripcion));
        when(codigoQrRepository.findByInscripcion_Id(501L)).thenReturn(Optional.of(codigoExistente()));

        ReenvioQrResponse resp = servicio.reenviarCodigo(1L, 501L);

        assertEquals(501L, resp.inscripcionId());
        assertTrue(resp.enviado());
    }

    @Test
    @DisplayName("El organizador no puede reenviar el QR de una inscripción de otro evento")
    void organizadorInscripcionDeOtroEvento() {
        when(inscripcionEventoRepository.findByIdAndEvento_IdConPersona(501L, 2L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.reenviarCodigo(2L, 501L));
    }
}
