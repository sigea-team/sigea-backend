package com.sigea.demosigea_backend.dto.auditoria;

import com.sigea.demosigea_backend.model.TipoOperacionAuditoria;

/**
 * Elemento del catálogo de tipos de operación (para el selector de filtro del frontend).
 */
public record TipoOperacionResponse(String codigo, String modulo, String descripcion) {

    public static TipoOperacionResponse fromEnum(TipoOperacionAuditoria t) {
        return new TipoOperacionResponse(t.name(), t.getModulo(), t.getDescripcion());
    }
}
