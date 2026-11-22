package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra en la auditoría los intentos rechazados de modificar o eliminar información de un evento cerrado
 * formalmente (HU-28, Criterio 3, RF56).
 * <p>
 * Se llama desde el manejador global de excepciones, cuando la transacción de la operación ya se revirtió; por eso
 * usa una transacción nueva ({@link Propagation#REQUIRES_NEW}). El usuario se toma de la sesión autenticada.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistroIntentosHistoricosService {

    static final String ENTIDAD = "eventos";

    private final AuditoriaService auditoriaService;

    /**
     * @param eventoId    evento cerrado formalmente (puede ser {@code null} si no se pudo determinar).
     * @param eliminacion {@code true} si se intentó eliminar.
     * @param metodo      método HTTP de la petición.
     * @param ruta        ruta de la petición.
     * @param mensaje     mensaje devuelto al usuario.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarIntentoRechazado(Long eventoId, boolean eliminacion, String metodo, String ruta,
                                          String mensaje) {
        TipoOperacionAuditoria accion = eliminacion
                ? TipoOperacionAuditoria.ELIMINACION_HISTORICA_RECHAZADA
                : TipoOperacionAuditoria.MODIFICACION_HISTORICA_RECHAZADA;
        auditoriaService.registrar(accion, ENTIDAD, eventoId,
                DetalleAuditoria.de("metodo", metodo, "ruta", ruta, "mensaje", mensaje));
        log.warn("Intento rechazado sobre la memoria histórica del evento {}: {} {}", eventoId, metodo, ruta);
    }
}
