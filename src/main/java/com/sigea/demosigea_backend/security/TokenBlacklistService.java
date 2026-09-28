package com.sigea.demosigea_backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio encargado de gestionar la invalidación de sesiones y tokens activos.
 * <p>
 * Permite marcar roles o usuarios específicos como invalidados registrando el timestamp del evento.
 * Cualquier token emitido antes de dicho timestamp será rechazado de inmediato por los filtros de seguridad,
 * forzando a los usuarios con sesiones activas a re-autenticarse tras un cambio de permisos (Criterio 2).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /**
     * Almacena el timestamp de la última invalidación por ID de rol.
     * Clave: rolId, Valor: Instant en el que el rol fue modificado.
     */
    private final Map<Long, Instant> rolInvalidaciones = new ConcurrentHashMap<>();

    /**
     * Invalida todas las sesiones activas asociadas a los usuarios que posean el rol especificado.
     *
     * @param rolId Identificador único del rol modificado.
     */
    public void invalidarSesionesDeRol(Long rolId) {
        Instant ahora = Instant.now();
        rolInvalidaciones.put(rolId, ahora);
        log.info("Sesiones del rol ID {} marcadas para invalidación inmediata en {}", rolId, ahora);
    }

    /**
     * Verifica si un token emitido en una fecha determinada ha sido invalidado debido a una
     * modificación posterior en alguno de los roles asignados al usuario.
     *
     * @param rolIds Lista de identificadores de los roles del usuario.
     * @param fechaEmisionToken Fecha y hora de emisión del token JWT (claim iat).
     * @return {@code true} si al menos uno de los roles fue modificado con posterioridad a la emisión del token,
     *         {@code false} en caso contrario.
     */
    public boolean esTokenInvalidoPorRoles(Iterable<Long> rolIds, Instant fechaEmisionToken) {
        if (rolIds == null || fechaEmisionToken == null) {
            return false;
        }

        for (Long rolId : rolIds) {
            Instant fechaInvalidacion = rolInvalidaciones.get(rolId);
            if (fechaInvalidacion != null && fechaInvalidacion.isAfter(fechaEmisionToken)) {
                log.debug("Token rechazado para rol ID {}: emitido en {}, invalidado en {}",
                        rolId, fechaEmisionToken, fechaInvalidacion);
                return true;
            }
        }
        return false;
    }

    /**
     * Limpia el registro de invalidaciones en memoria (útil para pruebas unitarias o de integración).
     */
    public void limpiarCache() {
        rolInvalidaciones.clear();
    }
}
