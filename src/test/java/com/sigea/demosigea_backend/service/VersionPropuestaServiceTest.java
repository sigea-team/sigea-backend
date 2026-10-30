package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.propuesta.ActualizarPropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorRequest;
import com.sigea.demosigea_backend.dto.propuesta.AutorizacionEdicionResponse;
import com.sigea.demosigea_backend.dto.propuesta.EnvioPropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaRequest;
import com.sigea.demosigea_backend.dto.propuesta.PropuestaResponse;
import com.sigea.demosigea_backend.dto.propuesta.VersionPropuestaResponse;
import com.sigea.demosigea_backend.exception.ArchivoInvalidoException;
import com.sigea.demosigea_backend.exception.ConvocatoriaCerradaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.PropuestaBloqueadaException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Convocatoria;
import com.sigea.demosigea_backend.model.DocumentoPropuesta;
import com.sigea.demosigea_backend.model.EstadoConvocatoria;
import com.sigea.demosigea_backend.model.EstadoPropuesta;
import com.sigea.demosigea_backend.model.Persona;
import com.sigea.demosigea_backend.model.Propuesta;
import com.sigea.demosigea_backend.model.RolAutor;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.model.Usuario;
import com.sigea.demosigea_backend.model.VersionPropuesta;
import com.sigea.demosigea_backend.repository.AsignacionEvaluacionRepository;
import com.sigea.demosigea_backend.repository.ConvocatoriaRepository;
import com.sigea.demosigea_backend.repository.DocumentoPropuestaRepository;
import com.sigea.demosigea_backend.repository.LineaTematicaRepository;
import com.sigea.demosigea_backend.repository.PropuestaAutorRepository;
import com.sigea.demosigea_backend.repository.PropuestaRepository;
import com.sigea.demosigea_backend.repository.UsuarioRepository;
import com.sigea.demosigea_backend.repository.VersionPropuestaRepository;
import com.sigea.demosigea_backend.service.ValidadorDocumentosPropuesta.DocumentoValidado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionPropuestaServiceTest {

    private static final String CORREO_AUTOR = "carlos@ufps.edu.co";

    @Mock private PropuestaService propuestaService;
    @Mock private PropuestaRepository propuestaRepository;
    @Mock private PropuestaAutorRepository propuestaAutorRepository;
    @Mock private VersionPropuestaRepository versionPropuestaRepository;
    @Mock private DocumentoPropuestaRepository documentoPropuestaRepository;
    @Mock private ConvocatoriaRepository convocatoriaRepository;
    @Mock private ConvocatoriaService convocatoriaService;
    @Mock private LineaTematicaRepository lineaTematicaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AsignacionEvaluacionRepository asignacionEvaluacionRepository;
    @Mock private ValidadorDocumentosPropuesta validadorDocumentos;
    @Mock private AlmacenamientoArchivos almacenamiento;
    @Mock private AuditoriaService auditoriaService;

    @InjectMocks
    private VersionPropuestaService service;

    @AfterEach
    void limpiarSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // Datos de prueba
    // ------------------------------------------------------------------

    private Usuario autenticar(Long usuarioId, Long personaId, String correo, String... autoridades) {
        List<SimpleGrantedAuthority> lista = Arrays.stream(autoridades).map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, lista));
        Persona persona = Persona.builder().id(personaId).nombres("Carlos").apellidos("Gómez")
                .correo(correo).tipoDocumento("CC").numeroDocumento("10" + personaId).build();
        Usuario usuario = Usuario.builder().id(usuarioId).persona(persona).build();
        when(usuarioRepository.findByPersona_CorreoIgnoreCase(correo)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private Convocatoria convocatoria() {
        return Convocatoria.builder().id(1L).titulo("Convocatoria 2026").estado(EstadoConvocatoria.publicada).build();
    }

    private Propuesta propuesta(EstadoPropuesta estado) {
        return Propuesta.builder().id(50L).convocatoria(convocatoria()).titulo("IA en la Educación")
                .resumen("Resumen v1").palabrasClave("IA").estado(estado).autores(new ArrayList<>()).build();
    }

    private DocumentoValidado pdf(String nombre) {
        MockMultipartFile archivo = new MockMultipartFile("documentos", nombre, "application/pdf",
                "%PDF-1.7 contenido".getBytes());
        return new DocumentoValidado(archivo, nombre, "pdf", "application/pdf");
    }

    private VersionPropuesta versionConDocumento(Propuesta p, int numero, Long documentoId, Usuario creador) {
        VersionPropuesta v = VersionPropuesta.builder().id(100L + numero).propuesta(p).numeroVersion(numero)
                .titulo(p.getTitulo()).documentoUrl("propuestas/50/v" + numero + "/a.pdf")
                .creadoPor(creador).fechaCreacion(LocalDateTime.now().minusDays(1)).build();
        v.agregarDocumento(DocumentoPropuesta.builder().id(documentoId).nombreOriginal("ponencia.pdf")
                .rutaAlmacenamiento("propuestas/50/v" + numero + "/a.pdf").tipoContenido("application/pdf")
                .tamanoBytes(2048L).principal(true).fechaCarga(LocalDateTime.now().minusDays(1)).build());
        return v;
    }

    private PropuestaRequest solicitudEnvio(List<AutorRequest> autores) {
        return new PropuestaRequest(1L, "IA en la Educación", "Resumen v1", "IA", null, autores);
    }

    private PropuestaResponse respuestaCreada() {
        return new PropuestaResponse(50L, 1L, "Convocatoria 2026", "IA en la Educación", "Resumen v1", "IA",
                null, null, EstadoPropuesta.recibida, LocalDateTime.now(), List.of());
    }

    private void guardarDevuelveArgumento() {
        when(versionPropuestaRepository.save(any(VersionPropuesta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // Criterio 1: envío con documentos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 1: con convocatoria activa registra la propuesta, guarda los archivos como versión 1 y confirma")
    void enviarPropuesta_ConvocatoriaActiva_RegistraVersionUnoYConfirma() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria()));
        List<DocumentoValidado> docs = List.of(pdf("ponencia.pdf"), pdf("anexo.pdf"));
        when(validadorDocumentos.validar(any(), eq(true))).thenReturn(docs);
        when(propuestaService.crearPropuesta(any(PropuestaRequest.class))).thenReturn(respuestaCreada());
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.recibida)));
        when(almacenamiento.guardar(eq("propuestas/50/v1"), eq("pdf"), any(InputStream.class)))
                .thenReturn("propuestas/50/v1/uno.pdf", "propuestas/50/v1/dos.pdf");
        guardarDevuelveArgumento();

        AutorRequest autor = new AutorRequest(null, "Carlos", "Gómez", CORREO_AUTOR, "UFPS", null,
                RolAutor.autor_principal, 1, null, null, null);
        EnvioPropuestaResponse respuesta = service.enviarPropuesta(solicitudEnvio(List.of(autor)),
                List.<MultipartFile>of());

        assertTrue(respuesta.mensaje().contains("recibida correctamente"));
        VersionPropuestaResponse version = respuesta.version();
        assertEquals(1, version.numeroVersion());
        assertEquals(4L, version.autorCambioUsuarioId());
        assertEquals(2, version.documentos().size());
        assertTrue(version.documentos().get(0).principal());
        assertFalse(version.documentos().get(1).principal());

        ArgumentCaptor<VersionPropuesta> captor = ArgumentCaptor.forClass(VersionPropuesta.class);
        verify(versionPropuestaRepository).save(captor.capture());
        assertEquals("propuestas/50/v1/uno.pdf", captor.getValue().getDocumentoUrl());
        assertSame(usuario, captor.getValue().getCreadoPor());
        assertEquals("IA en la Educación", captor.getValue().getTitulo());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PROPUESTA_VERSION_CREADA),
                eq("versiones_propuesta"), any(), anyMap());
    }

    @Test
    @DisplayName("Criterio 1: si no se envían autores, el usuario autenticado queda como autor principal")
    void enviarPropuesta_SinAutores_UsaUsuarioAutenticado() {
        autenticar(4L, 10L, CORREO_AUTOR);
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria()));
        when(validadorDocumentos.validar(any(), eq(true))).thenReturn(List.of(pdf("ponencia.pdf")));
        when(propuestaService.crearPropuesta(any(PropuestaRequest.class))).thenReturn(respuestaCreada());
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.recibida)));
        when(almacenamiento.guardar(anyString(), anyString(), any(InputStream.class))).thenReturn("propuestas/50/v1/x.pdf");
        guardarDevuelveArgumento();

        service.enviarPropuesta(solicitudEnvio(null), List.of());

        ArgumentCaptor<PropuestaRequest> captor = ArgumentCaptor.forClass(PropuestaRequest.class);
        verify(propuestaService).crearPropuesta(captor.capture());
        assertEquals(1, captor.getValue().autores().size());
        assertEquals(10L, captor.getValue().autores().get(0).personaId());
        assertEquals(RolAutor.autor_principal, captor.getValue().autores().get(0).rol());
    }

    // ------------------------------------------------------------------
    // Criterio 2: rechazos con motivo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 2: con la convocatoria cerrada rechaza el envío y no guarda nada")
    void enviarPropuesta_ConvocatoriaCerrada_Rechaza() {
        autenticar(4L, 10L, CORREO_AUTOR);
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria()));
        doThrow(new ConvocatoriaCerradaException("La fecha de cierre de la convocatoria ya se cumplió"))
                .when(convocatoriaService).validarRecepcionPropuestas(1L);

        assertThrows(ConvocatoriaCerradaException.class,
                () -> service.enviarPropuesta(solicitudEnvio(null), List.of()));

        verify(propuestaService, never()).crearPropuesta(any());
        verify(almacenamiento, never()).guardar(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Criterio 2: con un archivo de formato o tamaño no permitido rechaza el envío y no crea la propuesta")
    void enviarPropuesta_ArchivoInvalido_Rechaza() {
        autenticar(4L, 10L, CORREO_AUTOR);
        when(convocatoriaRepository.findById(1L)).thenReturn(Optional.of(convocatoria()));
        when(validadorDocumentos.validar(any(), anyBoolean())).thenThrow(new ArchivoInvalidoException(
                "Uno o varios documentos no cumplen las condiciones de carga.",
                Map.of("documentos[0]", "foto.png: formato no permitido.")));

        ArchivoInvalidoException ex = assertThrows(ArchivoInvalidoException.class,
                () -> service.enviarPropuesta(solicitudEnvio(null), List.of()));

        assertTrue(ex.getErrores().get("documentos[0]").contains("formato no permitido"));
        verify(propuestaService, never()).crearPropuesta(any());
    }

    // ------------------------------------------------------------------
    // Criterio 3: nueva versión conservando las anteriores
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 3: al reemplazar el documento se crea la versión 2 y la versión 1 no se modifica")
    void crearNuevaVersion_ReemplazaDocumento_CreaVersionDos() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        Propuesta propuesta = propuesta(EstadoPropuesta.recibida);
        VersionPropuesta v1 = versionConDocumento(propuesta, 1, 7L, usuario);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(validadorDocumentos.validar(any(), eq(false))).thenReturn(List.of(pdf("ponencia-v2.pdf")));
        when(versionPropuestaRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(50L)).thenReturn(Optional.of(v1));
        when(almacenamiento.guardar(eq("propuestas/50/v2"), eq("pdf"), any(InputStream.class)))
                .thenReturn("propuestas/50/v2/nuevo.pdf");
        guardarDevuelveArgumento();

        ActualizarPropuestaRequest cambios = new ActualizarPropuestaRequest(
                null, null, null, null, "Documento corregido", List.of());
        EnvioPropuestaResponse respuesta = service.crearNuevaVersion(50L, cambios, List.of());

        assertEquals(2, respuesta.version().numeroVersion());
        assertEquals("Documento corregido", respuesta.version().notaCambio());
        assertEquals(1, respuesta.version().documentos().size());
        assertEquals("ponencia-v2.pdf", respuesta.version().documentos().get(0).nombreOriginal());
        // La versión 1 queda intacta como evidencia.
        assertEquals(1, v1.getDocumentos().size());
        assertEquals("propuestas/50/v1/a.pdf", v1.getDocumentoUrl());
        verify(convocatoriaService).validarRecepcionPropuestas(1L);
    }

    @Test
    @DisplayName("Criterio 3: al cambiar solo el título se conservan los documentos y se guarda la copia de los datos")
    void crearNuevaVersion_CambiaTitulo_ConservaDocumentos() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        Propuesta propuesta = propuesta(EstadoPropuesta.recibida);
        VersionPropuesta v1 = versionConDocumento(propuesta, 1, 7L, usuario);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(validadorDocumentos.validar(any(), eq(false))).thenReturn(List.of());
        when(versionPropuestaRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(50L)).thenReturn(Optional.of(v1));
        guardarDevuelveArgumento();

        ActualizarPropuestaRequest cambios = new ActualizarPropuestaRequest(
                "IA en la Educación Superior", null, null, null, null, null);
        EnvioPropuestaResponse respuesta = service.crearNuevaVersion(50L, cambios, null);

        assertEquals("IA en la Educación Superior", propuesta.getTitulo());
        assertEquals("IA en la Educación Superior", respuesta.version().titulo());
        assertEquals(1, respuesta.version().documentos().size());
        assertTrue(respuesta.version().documentos().get(0).principal());
        assertEquals("IA en la Educación", v1.getTitulo());
        verify(almacenamiento, never()).guardar(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Criterio 3: si no hay cambios no se crea una versión nueva")
    void crearNuevaVersion_SinCambios_Rechaza() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        Propuesta propuesta = propuesta(EstadoPropuesta.recibida);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(validadorDocumentos.validar(any(), eq(false))).thenReturn(List.of());
        when(versionPropuestaRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(50L))
                .thenReturn(Optional.of(versionConDocumento(propuesta, 1, 7L, usuario)));

        assertThrows(SolicitudInvalidaException.class, () -> service.crearNuevaVersion(50L, null, null));
        verify(versionPropuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: un usuario que no es autor no puede modificar la propuesta")
    void crearNuevaVersion_NoAutor_AccesoDenegado() {
        autenticar(9L, 90L, "otro@ufps.edu.co");
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.recibida)));

        assertThrows(AccessDeniedException.class, () -> service.crearNuevaVersion(50L, null, null));
        verify(versionPropuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 3: con ajustes solicitados el autor puede subir la nueva versión aunque la convocatoria haya cerrado")
    void crearNuevaVersion_EstadoAjustes_PermiteSinValidarCierre() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        Propuesta propuesta = propuesta(EstadoPropuesta.ajustes);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(validadorDocumentos.validar(any(), eq(false))).thenReturn(List.of(pdf("ajustada.pdf")));
        when(versionPropuestaRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(50L))
                .thenReturn(Optional.of(versionConDocumento(propuesta, 1, 7L, usuario)));
        when(almacenamiento.guardar(anyString(), anyString(), any(InputStream.class))).thenReturn("propuestas/50/v2/b.pdf");
        guardarDevuelveArgumento();

        EnvioPropuestaResponse respuesta = service.crearNuevaVersion(50L, null, List.of());

        assertEquals(2, respuesta.version().numeroVersion());
        assertEquals(2, respuesta.version().documentos().size());
        verify(convocatoriaService, never()).validarRecepcionPropuestas(any());
    }

    @Test
    @DisplayName("Criterio 3: una propuesta rechazada no admite modificaciones")
    void crearNuevaVersion_Rechazada_NoPermite() {
        autenticar(4L, 10L, CORREO_AUTOR);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.rechazada)));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class, () -> service.crearNuevaVersion(50L, null, null));
    }

    // ------------------------------------------------------------------
    // Criterio 4: historial con fecha y autor del cambio
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 4: el administrador consulta todas las versiones con fecha y autor del cambio")
    void listarVersiones_Administrador_DevuelveHistorialCompleto() {
        autenticar(1L, 1L, "admin@ufps.edu.co", "ROLE_ADMIN");
        Propuesta propuesta = propuesta(EstadoPropuesta.en_evaluacion);
        Usuario autor = Usuario.builder().id(4L).persona(Persona.builder().id(10L).nombres("Carlos")
                .apellidos("Gómez").correo(CORREO_AUTOR).build()).build();
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta));
        when(versionPropuestaRepository.findByPropuesta_IdOrderByNumeroVersionAsc(50L)).thenReturn(List.of(
                versionConDocumento(propuesta, 1, 7L, autor),
                versionConDocumento(propuesta, 2, 8L, autor)));

        List<VersionPropuestaResponse> historial = service.listarVersiones(50L);

        assertEquals(2, historial.size());
        assertEquals(1, historial.get(0).numeroVersion());
        assertEquals(2, historial.get(1).numeroVersion());
        assertEquals("Carlos Gómez", historial.get(1).autorCambioNombre());
        assertEquals(CORREO_AUTOR, historial.get(1).autorCambioCorreo());
        assertNotNull(historial.get(1).fechaCreacion());
    }

    @Test
    @DisplayName("Criterio 4: el evaluador asignado a la propuesta puede consultar el historial")
    void listarVersiones_EvaluadorAsignado_Permite() {
        autenticar(6L, 60L, "evaluador@ufps.edu.co", "PROPUESTAS_EVALUAR");
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.en_evaluacion)));
        when(asignacionEvaluacionRepository.existsByPropuesta_IdAndComiteEvaluador_Persona_Id(50L, 60L)).thenReturn(true);
        when(versionPropuestaRepository.findByPropuesta_IdOrderByNumeroVersionAsc(50L)).thenReturn(List.of());

        assertEquals(0, service.listarVersiones(50L).size());
    }

    @Test
    @DisplayName("Criterio 4: un evaluador no asignado no puede consultar el historial (RNF08)")
    void listarVersiones_EvaluadorNoAsignado_Deniega() {
        autenticar(6L, 60L, "evaluador@ufps.edu.co", "PROPUESTAS_EVALUAR");
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.en_evaluacion)));

        assertThrows(AccessDeniedException.class, () -> service.listarVersiones(50L));
    }

    // ------------------------------------------------------------------
    // Criterio 5: versión definitiva y autorización explícita
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Criterio 5: con versión definitiva aprobada se restringe la edición")
    void crearNuevaVersion_VersionDefinitiva_Bloquea() {
        autenticar(4L, 10L, CORREO_AUTOR);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.aprobada)));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(50L)).thenReturn(true);

        assertThrows(PropuestaBloqueadaException.class, () -> service.crearNuevaVersion(50L, null, null));
        verify(versionPropuestaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criterio 5: con autorización explícita se permite la edición y la autorización se consume")
    void crearNuevaVersion_ConAutorizacion_PermiteYConsume() {
        Usuario usuario = autenticar(4L, 10L, CORREO_AUTOR);
        Propuesta propuesta = propuesta(EstadoPropuesta.aprobada);
        propuesta.setEdicionAutorizada(true);
        when(propuestaRepository.findByIdParaActualizar(50L)).thenReturn(Optional.of(propuesta));
        when(propuestaAutorRepository.existsByPropuesta_IdAndPersona_Id(50L, 10L)).thenReturn(true);
        when(versionPropuestaRepository.existsByPropuesta_IdAndEsDefinitivaTrue(50L)).thenReturn(true);
        when(validadorDocumentos.validar(any(), eq(false))).thenReturn(List.of());
        when(versionPropuestaRepository.findTopByPropuesta_IdOrderByNumeroVersionDesc(50L))
                .thenReturn(Optional.of(versionConDocumento(propuesta, 1, 7L, usuario)));
        guardarDevuelveArgumento();

        ActualizarPropuestaRequest cambios = new ActualizarPropuestaRequest(
                null, "Resumen corregido", null, null, "Corrección autorizada", null);
        EnvioPropuestaResponse respuesta = service.crearNuevaVersion(50L, cambios, null);

        assertEquals(2, respuesta.version().numeroVersion());
        assertFalse(propuesta.getEdicionAutorizada());
        verify(propuestaRepository).save(propuesta);
        verify(convocatoriaService, never()).validarRecepcionPropuestas(any());
    }

    @Test
    @DisplayName("Criterio 5: el administrador concede la autorización explícita y queda en auditoría")
    void autorizarEdicion_MarcaAutorizacionYAudita() {
        Propuesta propuesta = propuesta(EstadoPropuesta.aprobada);
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta));

        AutorizacionEdicionResponse respuesta = service.autorizarEdicion(50L, "  Corrección de forma  ");

        assertTrue(propuesta.getEdicionAutorizada());
        assertTrue(respuesta.edicionAutorizada());
        assertEquals("Corrección de forma", respuesta.motivo());
        verify(auditoriaService).registrar(eq(TipoOperacionAuditoria.PROPUESTA_EDICION_AUTORIZADA),
                eq("propuestas"), eq(50L), anyMap());
    }

    @Test
    @DisplayName("Criterio 5: no se autoriza la edición de una propuesta rechazada")
    void autorizarEdicion_Rechazada_NoPermite() {
        when(propuestaRepository.findById(50L)).thenReturn(Optional.of(propuesta(EstadoPropuesta.rechazada)));

        assertThrows(OperacionNoPermitidaException.class, () -> service.autorizarEdicion(50L, "motivo"));
        verify(propuestaRepository, never()).save(any());
    }
}
