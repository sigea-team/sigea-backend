package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.evento.EdicionesEventoResponse;
import com.sigea.demosigea_backend.dto.evento.EventoRequest;
import com.sigea.demosigea_backend.dto.evento.EventoResponse;
import com.sigea.demosigea_backend.dto.evento.NuevaEdicionRequest;
import com.sigea.demosigea_backend.exception.ConfirmacionRequeridaException;
import com.sigea.demosigea_backend.exception.OperacionNoPermitidaException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.EstadoEvento;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.ModalidadEvento;
import com.sigea.demosigea_backend.repository.EventoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private EventoService eventoService;

    private static final LocalDate INICIO = LocalDate.of(2026, 10, 20);
    private static final LocalDate FIN = LocalDate.of(2026, 10, 22);

    private EventoRequest requestValido() {
        return new EventoRequest("Congreso de Ingeniería de Sistemas", "Objetivo", "Descripción",
                "Congreso", ModalidadEvento.presencial, INICIO, FIN, null);
    }

    private Evento eventoBase(EstadoEvento estado) {
        return Evento.builder()
                .id(1L).nombre("Congreso de Ingeniería de Sistemas").objetivo("Objetivo").descripcion("Descripción")
                .tipo("Congreso").modalidad(ModalidadEvento.presencial)
                .fechaInicio(INICIO).fechaFin(FIN).semestre("2026-2").estado(estado)
                .build();
    }

    // ---------------- Criterio 1 ----------------

    @Test
    @DisplayName("Criterio 1: crea el evento en estado en_configuracion y deriva el semestre")
    void crear_quedaEnConfiguracion() {
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        EventoResponse response = eventoService.crear(requestValido());

        assertEquals(10L, response.id());
        assertEquals(EstadoEvento.en_configuracion, response.estado());
        assertEquals("2026-2", response.semestre());
        assertFalse(response.esEdicion());
        assertNull(response.eventoBaseId());
    }

    @Test
    @DisplayName("Semestre: enero-junio → AAAA-1, julio-diciembre → AAAA-2, y respeta el enviado")
    void resolverSemestre() {
        assertEquals("2026-1", EventoService.resolverSemestre(null, LocalDate.of(2026, 3, 1)));
        assertEquals("2026-2", EventoService.resolverSemestre(" ", LocalDate.of(2026, 7, 1)));
        assertEquals("2025-1", EventoService.resolverSemestre("2025-1", LocalDate.of(2026, 7, 1)));
    }

    // ---------------- Criterio 2 ----------------

    @Test
    @DisplayName("Criterio 2: actualiza la configuración de un evento en configuración")
    void actualizar_exitoso() {
        Evento evento = eventoBase(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoRequest cambios = new EventoRequest("Congreso renombrado", null, null, "Seminario",
                ModalidadEvento.hibrida, INICIO, FIN, null);

        EventoResponse response = eventoService.actualizar(1L, cambios);

        assertEquals("Congreso renombrado", response.nombre());
        assertEquals("Seminario", response.tipo());
        assertEquals(ModalidadEvento.hibrida, response.modalidad());
        assertEquals(EstadoEvento.en_configuracion, response.estado());
    }

    @Test
    @DisplayName("Criterio 2: no permite modificar un evento ya publicado")
    void actualizar_eventoPublicado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.habilitado)));

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.actualizar(1L, requestValido()));
        verify(eventoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 2: el evento base no puede cambiar a un semestre que ya usa una de sus ediciones")
    void actualizar_baseConSemestreOcupadoPorEdicion_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.existsByIdAndSemestreAndIdNot(1L, "2027-2", 1L)).thenReturn(false);
        when(eventoRepository.existsByEventoBase_IdAndSemestreAndIdNot(1L, "2027-2", 1L)).thenReturn(true);

        EventoRequest cambios = new EventoRequest("Congreso", null, null, "Congreso",
                ModalidadEvento.presencial, INICIO, FIN, "2027-2");

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.actualizar(1L, cambios));
        verify(eventoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 2: una edición no puede cambiar a un semestre ya usado en su familia")
    void actualizar_edicionConSemestreOcupado_rechaza() {
        Evento raiz = eventoBase(EstadoEvento.cerrado);
        Evento edicion = Evento.builder().id(2L).nombre("Congreso 2027").tipo("Congreso")
                .modalidad(ModalidadEvento.presencial).fechaInicio(LocalDate.of(2027, 10, 19))
                .fechaFin(LocalDate.of(2027, 10, 21)).semestre("2027-2")
                .estado(EstadoEvento.en_configuracion).eventoBase(raiz).build();
        when(eventoRepository.findById(2L)).thenReturn(Optional.of(edicion));
        when(eventoRepository.existsByIdAndSemestreAndIdNot(1L, "2026-2", 2L)).thenReturn(true);

        EventoRequest cambios = new EventoRequest("Congreso 2027", null, null, "Congreso",
                ModalidadEvento.presencial, INICIO, FIN, "2026-2");

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.actualizar(2L, cambios));
        verify(eventoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 2: la unicidad del semestre se valida siempre, excluyendo el propio registro")
    void actualizar_validaSiempreExcluyendoElPropioRegistro() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        eventoService.actualizar(1L, requestValido()); // semestre derivado: 2026-2, igual al actual

        verify(eventoRepository).existsByIdAndSemestreAndIdNot(1L, "2026-2", 1L);
        verify(eventoRepository).existsByEventoBase_IdAndSemestreAndIdNot(1L, "2026-2", 1L);
    }

    @Test
    @DisplayName("Criterio 2: si el índice único detecta un duplicado (concurrencia), responde como semestre ocupado")
    void actualizar_violacionIndiceUnico_seTraduceAOperacionNoPermitida() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.saveAndFlush(any(Evento.class)))
                .thenThrow(violacion("23505", "ux_eventos_familia_semestre"));

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> eventoService.actualizar(1L, requestValido()));
        assertTrue(ex.getMessage().contains("2026-2"));
    }

    @Test
    @DisplayName("Otras violaciones de integridad (CHECK) no se confunden con el semestre duplicado")
    void actualizar_violacionCheck_seRelanza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenThrow(violacion("23514", "eventos_check"));

        assertThrows(DataIntegrityViolationException.class, () -> eventoService.actualizar(1L, requestValido()));
    }

    @Test
    @DisplayName("Una violación de unicidad de otra restricción no se confunde con el semestre duplicado")
    void actualizar_otraRestriccionUnica_seRelanza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenThrow(violacion("23505", "eventos_pkey"));

        assertThrows(DataIntegrityViolationException.class, () -> eventoService.actualizar(1L, requestValido()));
    }

    @Test
    @DisplayName("La detección del índice usa SQLState y nombre de restricción, no el texto del mensaje")
    void esViolacionSemestreFamilia_usaDatosEstructurados() {
        assertTrue(EventoService.esViolacionSemestreFamilia(violacion("23505", "ux_eventos_familia_semestre")));
        assertTrue(EventoService.esViolacionSemestreFamilia(violacion("23505", "\"UX_EVENTOS_FAMILIA_SEMESTRE\"")));
        assertFalse(EventoService.esViolacionSemestreFamilia(violacion("23514", "ux_eventos_familia_semestre")));
        assertFalse(EventoService.esViolacionSemestreFamilia(violacion("23505", null)));
        // Un mensaje que menciona el índice, pero sin datos estructurados, ya no basta
        assertFalse(EventoService.esViolacionSemestreFamilia(new DataIntegrityViolationException(
                "x", new RuntimeException("duplicate key value violates unique constraint \"ux_eventos_familia_semestre\""))));
    }

    // ---------------- Criterio 3 ----------------

    @Test
    @DisplayName("Criterio 3: crea una edición vinculada al base, heredando su configuración general")
    void crearEdicion_exitoso() {
        Evento base = eventoBase(EstadoEvento.cerrado);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(base));
        when(eventoRepository.existsByIdAndSemestre(1L, "2027-2")).thenReturn(false);
        when(eventoRepository.existsByEventoBase_IdAndSemestre(1L, "2027-2")).thenReturn(false);
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            e.setId(20L);
            return e;
        });

        NuevaEdicionRequest request = new NuevaEdicionRequest(null,
                LocalDate.of(2027, 10, 19), LocalDate.of(2027, 10, 21), null);

        EventoResponse response = eventoService.crearEdicion(1L, request);

        ArgumentCaptor<Evento> captor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).saveAndFlush(captor.capture());
        Evento guardada = captor.getValue();

        assertEquals(20L, response.id());
        assertTrue(response.esEdicion());
        assertEquals(1L, response.eventoBaseId());
        assertEquals(EstadoEvento.en_configuracion, response.estado());
        assertEquals("2027-2", response.semestre());
        assertEquals(base.getNombre(), guardada.getNombre());
        assertEquals(base.getObjetivo(), guardada.getObjetivo());
        assertEquals(base.getDescripcion(), guardada.getDescripcion());
        assertEquals(base.getTipo(), guardada.getTipo());
        assertEquals(base.getModalidad(), guardada.getModalidad());
        // El evento origen no se modifica
        assertEquals(EstadoEvento.cerrado, base.getEstado());
        assertEquals(INICIO, base.getFechaInicio());
    }

    @Test
    @DisplayName("Criterio 3: una edición creada desde otra edición queda vinculada al evento raíz")
    void crearEdicion_desdeOtraEdicion_apuntaAlRaiz() {
        Evento raiz = eventoBase(EstadoEvento.cerrado);
        Evento edicion2026 = Evento.builder().id(5L).nombre("Congreso 2026").tipo("Congreso")
                .modalidad(ModalidadEvento.virtual).fechaInicio(INICIO).fechaFin(FIN)
                .semestre("2026-2").estado(EstadoEvento.cerrado).eventoBase(raiz).build();

        when(eventoRepository.findById(5L)).thenReturn(Optional.of(edicion2026));
        when(eventoRepository.existsByIdAndSemestre(1L, "2027-2")).thenReturn(false);
        when(eventoRepository.existsByEventoBase_IdAndSemestre(1L, "2027-2")).thenReturn(false);
        when(eventoRepository.saveAndFlush(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            e.setId(21L);
            return e;
        });

        NuevaEdicionRequest request = new NuevaEdicionRequest("Congreso 2027",
                LocalDate.of(2027, 10, 19), LocalDate.of(2027, 10, 21), "2027-2");

        EventoResponse response = eventoService.crearEdicion(5L, request);

        assertEquals(1L, response.eventoBaseId());
        assertEquals(ModalidadEvento.virtual, response.modalidad()); // hereda del origen inmediato
    }

    @Test
    @DisplayName("Criterio 3: rechaza una edición duplicada para el mismo semestre")
    void crearEdicion_semestreDuplicado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.cerrado)));
        when(eventoRepository.existsByIdAndSemestre(1L, "2026-2")).thenReturn(true);

        NuevaEdicionRequest request = new NuevaEdicionRequest(null, INICIO, FIN, null);

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.crearEdicion(1L, request));
        verify(eventoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Criterio 3: evento origen inexistente → 404")
    void crearEdicion_origenInexistente() {
        when(eventoRepository.findById(99L)).thenReturn(Optional.empty());
        NuevaEdicionRequest request = new NuevaEdicionRequest(null, INICIO, FIN, null);
        assertThrows(RecursoNoEncontradoException.class, () -> eventoService.crearEdicion(99L, request));
    }

    // ---------------- Criterio 4 ----------------

    @Test
    @DisplayName("Criterio 4: lista las ediciones de la familia aunque se consulte desde una edición")
    void listarEdiciones_desdeEdicion() {
        Evento raiz = eventoBase(EstadoEvento.cerrado);
        Evento e2 = Evento.builder().id(2L).nombre("Congreso 2027").tipo("Congreso")
                .fechaInicio(LocalDate.of(2027, 10, 19)).fechaFin(LocalDate.of(2027, 10, 21))
                .semestre("2027-2").estado(EstadoEvento.en_configuracion).eventoBase(raiz).build();

        when(eventoRepository.findById(2L)).thenReturn(Optional.of(e2));
        when(eventoRepository.findByEventoBase_IdOrderByFechaInicioAscIdAsc(1L)).thenReturn(List.of(e2));

        EdicionesEventoResponse response = eventoService.listarEdiciones(2L);

        assertEquals(1L, response.eventoBaseId());
        assertEquals(2, response.totalEdiciones());
        assertEquals(1L, response.ediciones().get(0).id());
        assertEquals(EstadoEvento.cerrado, response.ediciones().get(0).estado());
        assertEquals(EstadoEvento.en_configuracion, response.ediciones().get(1).estado());
    }

    @Test
    @DisplayName("Un ciclo en la jerarquía (A -> B -> A) se rechaza de forma controlada y sin bloquearse")
    void listarEdiciones_jerarquiaCiclica_seRechaza() {
        Evento a = Evento.builder().id(10L).nombre("A").tipo("Congreso")
                .fechaInicio(INICIO).fechaFin(FIN).estado(EstadoEvento.cerrado).build();
        Evento b = Evento.builder().id(11L).nombre("B").tipo("Congreso")
                .fechaInicio(INICIO).fechaFin(FIN).estado(EstadoEvento.cerrado).eventoBase(a).build();
        a.setEventoBase(b); // A -> B -> A
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(a));

        OperacionNoPermitidaException ex = assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertThrows(OperacionNoPermitidaException.class, () -> eventoService.listarEdiciones(10L)));
        assertTrue(ex.getMessage().contains("jerarquía"));
    }

    @Test
    @DisplayName("Un evento que figura como su propio evento base se rechaza de forma controlada")
    void listarEdiciones_autorreferencia_seRechaza() {
        Evento a = Evento.builder().id(10L).nombre("A").tipo("Congreso")
                .fechaInicio(INICIO).fechaFin(FIN).estado(EstadoEvento.cerrado).build();
        a.setEventoBase(a); // A -> A
        when(eventoRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.listarEdiciones(10L));
    }

    @Test
    @DisplayName("No se crea una edición a partir de un evento con jerarquía inválida")
    void crearEdicion_origenConJerarquiaInvalida_seRechaza() {
        Evento raiz = eventoBase(EstadoEvento.cerrado);
        Evento intermedio = Evento.builder().id(5L).nombre("Intermedio").tipo("Congreso")
                .fechaInicio(INICIO).fechaFin(FIN).estado(EstadoEvento.cerrado).eventoBase(raiz).build();
        Evento origen = Evento.builder().id(6L).nombre("Origen").tipo("Congreso")
                .fechaInicio(INICIO).fechaFin(FIN).estado(EstadoEvento.cerrado).eventoBase(intermedio).build();
        when(eventoRepository.findById(6L)).thenReturn(Optional.of(origen));

        NuevaEdicionRequest request = new NuevaEdicionRequest(null, LocalDate.of(2027, 10, 19), LocalDate.of(2027, 10, 21), null);

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.crearEdicion(6L, request));
        verify(eventoRepository, never()).saveAndFlush(any());
    }

    // ---------------- Criterio 5 ----------------

    @Test
    @DisplayName("Criterio 5: eliminar sin confirmación explícita exige confirmación")
    void eliminar_sinConfirmar_exigeConfirmacion() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.countByEventoBase_Id(1L)).thenReturn(0L);

        assertThrows(ConfirmacionRequeridaException.class, () -> eventoService.eliminar(1L, false));
        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    @Test
    @DisplayName("Criterio 5: con confirmación explícita se elimina")
    void eliminar_confirmado() {
        Evento evento = eventoBase(EstadoEvento.en_configuracion);
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(evento));
        when(eventoRepository.countByEventoBase_Id(1L)).thenReturn(0L);

        eventoService.eliminar(1L, true);

        verify(eventoRepository).delete(evento);
    }

    @Test
    @DisplayName("Criterio 5: no elimina un evento base con ediciones derivadas (integridad histórica)")
    void eliminar_baseConEdiciones_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.countByEventoBase_Id(1L)).thenReturn(2L);

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.eliminar(1L, true));
        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    @Test
    @DisplayName("Criterio 5: no elimina un evento que ya no está en configuración")
    void eliminar_eventoPublicado_rechaza() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.cerrado)));

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.eliminar(1L, true));
        verify(eventoRepository, never()).delete(any(Evento.class));
    }

    // ---------------- Publicación ----------------

    @Test
    @DisplayName("Publicar: cambia estado de en_configuracion a habilitado exitosamente")
    void publicar_eventoEnConfiguracion_cambiaAHabilitado() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.en_configuracion)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> inv.getArgument(0));

        EventoResponse response = eventoService.publicar(1L);

        assertEquals(EstadoEvento.habilitado, response.estado());
        verify(eventoRepository).save(any(Evento.class));
    }

    @Test
    @DisplayName("Publicar: si no está en configuración, lanza OperacionNoPermitidaException")
    void publicar_noEnConfiguracion_lanzaOperacionNoPermitida() {
        when(eventoRepository.findById(1L)).thenReturn(Optional.of(eventoBase(EstadoEvento.habilitado)));

        assertThrows(OperacionNoPermitidaException.class, () -> eventoService.publicar(1L));
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    // ---------------- Listado ----------------

    @Test
    @DisplayName("Listar: aplica filtros opcionales mediante Specification")
    void listar_conFiltros() {
        when(eventoRepository.findAll(ArgumentMatchers.<Specification<Evento>>any(), any(Sort.class)))
                .thenReturn(List.of(eventoBase(EstadoEvento.habilitado)));

        List<EventoResponse> resultado = eventoService.listar(EstadoEvento.habilitado, "  ");

        assertEquals(1, resultado.size());
        assertEquals(EstadoEvento.habilitado, resultado.get(0).estado());
    }

    /** Imita la excepción que entrega Spring: DataIntegrityViolationException envolviendo la de Hibernate. */
    private static DataIntegrityViolationException violacion(String sqlState, String restriccion) {
        ConstraintViolationException hibernate = new ConstraintViolationException(
                "could not execute statement", new SQLException("error de integridad", sqlState), restriccion);
        return new DataIntegrityViolationException("could not execute statement", hibernate);
    }
}
