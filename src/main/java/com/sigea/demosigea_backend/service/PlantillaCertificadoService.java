package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.certificado.FirmanteCertificadoResponse;
import com.sigea.demosigea_backend.dto.certificado.PlantillaCertificadoResponse;
import com.sigea.demosigea_backend.exception.CertificadoException;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.exception.SolicitudInvalidaException;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.model.FirmanteCertificado;
import com.sigea.demosigea_backend.model.PlantillaCertificado;
import com.sigea.demosigea_backend.model.TipoCertificado;
import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import com.sigea.demosigea_backend.repository.CertificadoRepository;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.FirmanteCertificadoRepository;
import com.sigea.demosigea_backend.repository.PlantillaCertificadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Plantilla de los certificados de un evento (HU-26, RF47): imagen de fondo y de 1 a
 * {@value GeneradorPdfCertificado#MAXIMO_FIRMAS} firmantes (nombre, cargo e imagen de la firma).
 * <p>
 * Todos los certificados de un evento deben ser iguales: la plantilla solo se modifica mientras los certificados no se
 * hayan habilitado ({@link #habilitar}, que exige fondo y al menos una firma) ni emitido (409
 * {@value CertificadoException#PLANTILLA_BLOQUEADA}). Las modificaciones toman un bloqueo exclusivo de la
 * plantilla y la emisión uno compartido, así una emisión y un cambio simultáneos no se mezclan. Con el evento cerrado
 * formalmente (HU-28) tampoco se modifica.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlantillaCertificadoService {

    static final int LARGO_MAXIMO_TEXTO = 150;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String ENTIDAD = "plantillas_certificado";

    private final PlantillaCertificadoRepository plantillaRepository;
    private final FirmanteCertificadoRepository firmanteRepository;
    private final CertificadoRepository certificadoRepository;
    private final EventoRepository eventoRepository;
    private final ProcesadorImagenesCertificado procesadorImagenes;
    private final GeneradorPdfCertificado generadorPdf;
    private final AlmacenamientoArchivos almacenamiento;
    private final AuditoriaService auditoriaService;
    private final Clock clock;

    @Value("${app.certificados.url-verificacion:http://localhost:5173/#/verificar/}")
    private String urlVerificacionBase;

    /** Fondo y firmas listos para el PDF. */
    public record PlantillaPdf(byte[] fondoJpeg, List<GeneradorPdfCertificado.Firma> firmas) {
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Consulta
    // ──────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlantillaCertificadoResponse consultar(Long eventoId) {
        return respuesta(buscarEvento(eventoId));
    }

    /** Imagen de fondo (JPEG) del evento. */
    @Transactional(readOnly = true)
    public byte[] imagenFondo(Long eventoId) {
        buscarEvento(eventoId);
        PlantillaCertificado plantilla = plantillaRepository.findByEvento_Id(eventoId)
                .filter(p -> p.getFondoUrl() != null)
                .orElseThrow(() -> new RecursoNoEncontradoException("El evento no tiene imagen de fondo para los certificados."));
        return leer(plantilla.getFondoUrl());
    }

    /** Imagen de la firma (PNG) de un firmante. */
    @Transactional(readOnly = true)
    public byte[] imagenFirma(Long eventoId, Long firmanteId) {
        return leer(buscarFirmante(eventoId, firmanteId).getFirmaUrl());
    }

    /**
     * PDF de muestra con la plantilla actual y datos de ejemplo, para revisar el diseño antes de emitir.
     *
     * @param tipo rol de ejemplo (asistente si es null).
     */
    @Transactional(readOnly = true)
    public byte[] vistaPrevia(Long eventoId, TipoCertificado tipo) {
        Evento evento = buscarEvento(eventoId);
        TipoCertificado t = tipo == null || !tipo.isEmisionDisponible() ? TipoCertificado.asistente : tipo;
        String detalle = t == TipoCertificado.asistente
                ? "Asistió a 5 de las 6 actividades en las que se inscribió (83,33 %), con una intensidad de 10 horas."
                : "A cargo de la actividad «Nombre de la actividad», con una intensidad de 2 horas.";
        PlantillaPdf plantilla = cargarParaPdf(eventoId);
        String codigo = "CERT-VISTAPREVIA0000000000";
        return generadorPdf.generar(new GeneradorPdfCertificado.DatosCertificado(t, "Nombre del participante", "CC",
                "0000000000", evento.getNombre(), evento.getFechaInicio(), evento.getFechaFin(), detalle,
                LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS), codigo,
                CertificadoService.urlVerificacion(urlVerificacionBase, codigo),
                plantilla.fondoJpeg(), plantilla.firmas()));
    }

    /**
     * Fondo y firmas del evento para generar sus certificados. Una imagen que ya no está en el almacenamiento se omite
     * (se registra la advertencia) en lugar de impedir la entrega del certificado.
     */
    @Transactional(readOnly = true)
    public PlantillaPdf cargarParaPdf(Long eventoId) {
        byte[] fondo = plantillaRepository.findByEvento_Id(eventoId)
                .map(PlantillaCertificado::getFondoUrl)
                .map(this::leerOpcional)
                .orElse(null);
        List<GeneradorPdfCertificado.Firma> firmas = new ArrayList<>();
        for (FirmanteCertificado f : firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId)) {
            firmas.add(new GeneradorPdfCertificado.Firma(f.getNombre(), f.getCargo(), leerOpcional(f.getFirmaUrl())));
        }
        return new PlantillaPdf(fondo, firmas);
    }

    /** Fecha en que se habilitaron los certificados del evento, o vacío si aún no se han habilitado. */
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> fechaHabilitacion(Long eventoId) {
        return plantillaRepository.findByEvento_Id(eventoId).map(PlantillaCertificado::getFechaHabilitacion);
    }

    /**
     * Deja la plantilla fija y marca los certificados del evento como habilitados. Exige fondo y al menos una firma.
     * Lo llama {@link CertificadoService#habilitar(Long)} dentro de su transacción, que después genera los certificados.
     *
     * @return fecha de habilitación.
     * @throws CertificadoException si falta el fondo o las firmas, o si ya estaban habilitados.
     */
    @Transactional
    public LocalDateTime habilitar(Evento evento) {
        Long eventoId = evento.getId();
        certificadoRepository.bloquearPlantilla(eventoId);
        Optional<PlantillaCertificado> plantilla = plantillaRepository.findByEvento_Id(eventoId);
        if (plantilla.isPresent() && plantilla.get().estaHabilitada()) {
            throw new CertificadoException(CertificadoException.YA_HABILITADOS, HttpStatus.CONFLICT,
                    "Los certificados de este evento ya fueron habilitados el "
                            + plantilla.get().getFechaHabilitacion().format(FORMATO_FECHA_HORA) + ".");
        }
        List<String> faltantes = faltantes(plantilla.orElse(null),
                firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId).size());
        if (!faltantes.isEmpty()) {
            throw new CertificadoException(CertificadoException.PLANTILLA_INCOMPLETA, HttpStatus.CONFLICT,
                    "Para habilitar los certificados complete la plantilla: " + String.join(" ", faltantes));
        }
        PlantillaCertificado p = plantilla.get();
        LocalDateTime fecha = ahora();
        p.setFechaHabilitacion(fecha);
        plantillaRepository.save(p);
        return fecha;
    }

    /** Lo que falta en la plantilla para habilitar los certificados. */
    static List<String> faltantes(PlantillaCertificado plantilla, int firmantes) {
        List<String> faltan = new ArrayList<>();
        if (plantilla == null || plantilla.getFondoUrl() == null) {
            faltan.add("Suba la imagen de fondo del certificado.");
        }
        if (firmantes < 1) {
            faltan.add("Agregue al menos un firmante con su firma.");
        }
        return faltan;
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Fondo
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Sube o reemplaza la imagen de fondo (PNG o JPG horizontal).
     */
    @Transactional
    public PlantillaCertificadoResponse subirFondo(Long eventoId, byte[] archivo) {
        Evento evento = prepararModificacion(eventoId);
        byte[] jpeg = procesadorImagenes.normalizarFondo(archivo);
        String ruta = guardar(eventoId, "jpg", jpeg);

        PlantillaCertificado plantilla = plantillaRepository.findByEvento_Id(eventoId)
                .orElseGet(() -> PlantillaCertificado.builder().evento(evento).build());
        String anterior = plantilla.getFondoUrl();
        plantilla.setFondoUrl(ruta);
        plantilla.setFechaActualizacion(ahora());
        plantillaRepository.save(plantilla);
        eliminarDespuesDelCommit(anterior);

        auditar(eventoId, plantilla.getId(), "FONDO_ACTUALIZADO", null);
        return respuesta(evento);
    }

    /** Quita la imagen de fondo: los certificados vuelven al diseño predeterminado. */
    @Transactional
    public PlantillaCertificadoResponse eliminarFondo(Long eventoId) {
        Evento evento = prepararModificacion(eventoId);
        plantillaRepository.findByEvento_Id(eventoId).filter(p -> p.getFondoUrl() != null).ifPresent(p -> {
            String anterior = p.getFondoUrl();
            p.setFondoUrl(null);
            p.setFechaActualizacion(ahora());
            plantillaRepository.save(p);
            eliminarDespuesDelCommit(anterior);
            auditar(eventoId, p.getId(), "FONDO_ELIMINADO", null);
        });
        return respuesta(evento);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Firmantes
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * Agrega un firmante al final del pie.
     */
    @Transactional
    public PlantillaCertificadoResponse agregarFirmante(Long eventoId, String nombre, String cargo, byte[] firma) {
        Evento evento = prepararModificacion(eventoId);
        String nombreLimpio = textoObligatorio("nombre", nombre, "el nombre del firmante");
        String cargoLimpio = textoObligatorio("cargo", cargo, "el cargo del firmante");
        List<FirmanteCertificado> actuales = firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId);
        if (actuales.size() >= GeneradorPdfCertificado.MAXIMO_FIRMAS) {
            throw new CertificadoException(CertificadoException.MAXIMO_FIRMANTES, HttpStatus.CONFLICT,
                    "El certificado admite máximo " + GeneradorPdfCertificado.MAXIMO_FIRMAS + " firmantes.");
        }
        byte[] png = procesadorImagenes.normalizarFirma(firma);
        String ruta = guardar(eventoId, "png", png);

        FirmanteCertificado firmante = firmanteRepository.save(FirmanteCertificado.builder()
                .evento(evento)
                .nombre(nombreLimpio)
                .cargo(cargoLimpio)
                .firmaUrl(ruta)
                .orden(actuales.size() + 1)
                .fechaRegistro(ahora())
                .build());
        auditar(eventoId, firmante.getId(), "FIRMANTE_AGREGADO", nombreLimpio);
        return respuesta(evento);
    }

    /**
     * Cambia el nombre, el cargo y, si se envía, la imagen de la firma.
     */
    @Transactional
    public PlantillaCertificadoResponse actualizarFirmante(Long eventoId, Long firmanteId, String nombre, String cargo,
                                                           byte[] firma) {
        Evento evento = prepararModificacion(eventoId);
        FirmanteCertificado firmante = buscarFirmante(eventoId, firmanteId);
        firmante.setNombre(textoObligatorio("nombre", nombre, "el nombre del firmante"));
        firmante.setCargo(textoObligatorio("cargo", cargo, "el cargo del firmante"));
        if (firma != null && firma.length > 0) {
            byte[] png = procesadorImagenes.normalizarFirma(firma);
            String anterior = firmante.getFirmaUrl();
            firmante.setFirmaUrl(guardar(eventoId, "png", png));
            eliminarDespuesDelCommit(anterior);
        }
        firmanteRepository.save(firmante);
        auditar(eventoId, firmante.getId(), "FIRMANTE_ACTUALIZADO", firmante.getNombre());
        return respuesta(evento);
    }

    /** Quita un firmante; los demás se corren a la izquierda. */
    @Transactional
    public PlantillaCertificadoResponse eliminarFirmante(Long eventoId, Long firmanteId) {
        Evento evento = prepararModificacion(eventoId);
        FirmanteCertificado firmante = buscarFirmante(eventoId, firmanteId);
        firmanteRepository.delete(firmante);
        eliminarDespuesDelCommit(firmante.getFirmaUrl());
        int orden = 1;
        for (FirmanteCertificado f : firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId)) {
            if (Objects.equals(f.getId(), firmanteId)) {
                continue;
            }
            if (!Objects.equals(f.getOrden(), orden)) {
                f.setOrden(orden);
                firmanteRepository.save(f);
            }
            orden++;
        }
        auditar(eventoId, firmanteId, "FIRMANTE_ELIMINADO", firmante.getNombre());
        return respuesta(evento);
    }

    /**
     * Cambia el orden de los firmantes (de izquierda a derecha). La lista debe tener a todos, sin repetir.
     */
    @Transactional
    public PlantillaCertificadoResponse reordenarFirmantes(Long eventoId, List<Long> firmanteIds) {
        Evento evento = prepararModificacion(eventoId);
        List<FirmanteCertificado> actuales = firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId);
        List<Long> ids = firmanteIds == null ? List.of() : firmanteIds;
        HashSet<Long> esperados = new HashSet<>();
        actuales.forEach(f -> esperados.add(f.getId()));
        if (ids.size() != actuales.size() || !esperados.equals(new HashSet<>(ids))) {
            throw new SolicitudInvalidaException("El orden debe incluir una vez a cada firmante del evento.");
        }
        for (FirmanteCertificado f : actuales) {
            int orden = ids.indexOf(f.getId()) + 1;
            if (!Objects.equals(f.getOrden(), orden)) {
                f.setOrden(orden);
                firmanteRepository.save(f);
            }
        }
        auditar(eventoId, null, "FIRMANTES_REORDENADOS", null);
        return respuesta(evento);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Auxiliares
    // ──────────────────────────────────────────────────────────────────────────────

    /** Evento editable y sin certificados emitidos; toma el bloqueo exclusivo de la plantilla. */
    private Evento prepararModificacion(Long eventoId) {
        Evento evento = buscarEvento(eventoId);
        MemoriaHistoricaReglas.verificarEditable(evento);
        certificadoRepository.bloquearPlantilla(eventoId);
        Optional<PlantillaCertificado> plantilla = plantillaRepository.findByEvento_Id(eventoId);
        if (plantilla.isPresent() && plantilla.get().estaHabilitada()) {
            throw new CertificadoException(CertificadoException.PLANTILLA_BLOQUEADA, HttpStatus.CONFLICT,
                    mensajeHabilitada(plantilla.get().getFechaHabilitacion()));
        }
        long emitidos = certificadoRepository.countByEvento_Id(eventoId);
        if (emitidos > 0) {
            throw new CertificadoException(CertificadoException.PLANTILLA_BLOQUEADA, HttpStatus.CONFLICT,
                    mensajeBloqueo(emitidos));
        }
        return evento;
    }

    private static String mensajeHabilitada(LocalDateTime fecha) {
        return "Los certificados del evento se habilitaron el " + fecha.format(FORMATO_FECHA_HORA)
                + "; la plantilla quedó fija porque todos los certificados deben ser iguales.";
    }

    private static String mensajeBloqueo(long emitidos) {
        return "Ya se " + (emitidos == 1 ? "emitió 1 certificado" : "emitieron " + emitidos + " certificados")
                + " con esta plantilla; no se puede cambiar porque todos los certificados del evento deben ser iguales.";
    }

    private PlantillaCertificadoResponse respuesta(Evento evento) {
        Long eventoId = evento.getId();
        String base = "/api/v1/eventos/" + eventoId + "/certificados/plantilla";
        Optional<PlantillaCertificado> plantilla = plantillaRepository.findByEvento_Id(eventoId);
        boolean tieneFondo = plantilla.map(p -> p.getFondoUrl() != null).orElse(false);
        LocalDateTime habilitada = plantilla.map(PlantillaCertificado::getFechaHabilitacion).orElse(null);
        List<FirmanteCertificadoResponse> firmantes = firmanteRepository.findByEvento_IdOrderByOrdenAscIdAsc(eventoId)
                .stream()
                .map(f -> new FirmanteCertificadoResponse(f.getId(), f.getNombre(), f.getCargo(), f.getOrden(),
                        base + "/firmantes/" + f.getId() + "/firma"))
                .toList();
        long emitidos = certificadoRepository.countByEvento_Id(eventoId);
        String motivo = null;
        if (evento.isArchivado()) {
            motivo = "El evento fue cerrado formalmente; su información solo se consulta.";
        } else if (habilitada != null) {
            motivo = mensajeHabilitada(habilitada);
        } else if (emitidos > 0) {
            motivo = mensajeBloqueo(emitidos);
        }
        List<String> faltan = faltantes(plantilla.orElse(null), firmantes.size());
        return new PlantillaCertificadoResponse(eventoId, tieneFondo, tieneFondo ? base + "/fondo" : null, firmantes,
                GeneradorPdfCertificado.MAXIMO_FIRMAS, motivo == null, motivo, base + "/vista-previa",
                faltan.isEmpty(), faltan, habilitada != null, habilitada);
    }

    private static String textoObligatorio(String campo, String valor, String descripcion) {
        String limpio = valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new SolicitudInvalidaException("Indique " + descripcion + ".");
        }
        if (limpio.length() > LARGO_MAXIMO_TEXTO) {
            throw new SolicitudInvalidaException("El " + campo + " del firmante admite máximo " + LARGO_MAXIMO_TEXTO
                    + " caracteres.");
        }
        return limpio;
    }

    private String guardar(Long eventoId, String extension, byte[] contenido) {
        String ruta = almacenamiento.guardar("certificados/" + eventoId + "/plantilla", extension,
                new ByteArrayInputStream(contenido));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        almacenamiento.eliminar(ruta);
                    }
                }
            });
        }
        return ruta;
    }

    /** El archivo reemplazado se borra solo si el cambio se confirma. */
    private void eliminarDespuesDelCommit(String ruta) {
        if (ruta == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            almacenamiento.eliminar(ruta);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                almacenamiento.eliminar(ruta);
            }
        });
    }

    private byte[] leer(String ruta) {
        try (InputStream entrada = almacenamiento.cargar(ruta).getInputStream()) {
            return entrada.readAllBytes();
        } catch (IOException ex) {
            throw new RecursoNoEncontradoException("No se pudo leer la imagen del almacenamiento.");
        }
    }

    private byte[] leerOpcional(String ruta) {
        try {
            return leer(ruta);
        } catch (RuntimeException ex) {
            log.warn("No se encontró la imagen {} de la plantilla de certificados: {}", ruta, ex.getMessage());
            return null;
        }
    }

    private void auditar(Long eventoId, Long entidadId, String operacion, String firmante) {
        auditoriaService.registrar(TipoOperacionAuditoria.PLANTILLA_CERTIFICADO_ACTUALIZADA, ENTIDAD,
                entidadId != null ? entidadId : eventoId,
                DetalleAuditoria.de("eventoId", eventoId, "operacion", operacion, "firmante", firmante));
    }

    private FirmanteCertificado buscarFirmante(Long eventoId, Long firmanteId) {
        return firmanteRepository.findByIdAndEvento_Id(firmanteId, eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el firmante con ID " + firmanteId + " en este evento."));
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID " + eventoId + "."));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
