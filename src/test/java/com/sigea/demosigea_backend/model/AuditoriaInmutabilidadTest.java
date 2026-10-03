package com.sigea.demosigea_backend.model;

import com.sigea.demosigea_backend.exception.RegistroAuditoriaInmutableException;
import com.sigea.demosigea_backend.repository.AuditoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU-03, Criterio 3: un registro de auditoría no puede modificarse ni eliminarse.
 * (La tercera capa, el trigger de PostgreSQL, se valida contra la base de datos real.)
 */
class AuditoriaInmutabilidadTest {

    @Test
    @DisplayName("Criterio 3: la entidad rechaza actualizaciones y eliminaciones")
    void entidad_rechazaUpdateYDelete() {
        Auditoria registro = Auditoria.builder().accion("ROL_CREADO").entidad("roles").build();

        assertThrows(RegistroAuditoriaInmutableException.class, registro::antesDeActualizar);
        assertThrows(RegistroAuditoriaInmutableException.class, registro::antesDeEliminar);
    }

    @Test
    @DisplayName("Criterio 3: la entidad no expone setters")
    void entidad_sinSetters() {
        boolean tieneSetters = Arrays.stream(Auditoria.class.getMethods())
                .map(Method::getName)
                .anyMatch(n -> n.startsWith("set"));
        assertFalse(tieneSetters);
    }

    @Test
    @DisplayName("Criterio 3: el repositorio no ofrece métodos de borrado")
    void repositorio_sinDelete() {
        boolean tieneDelete = Arrays.stream(AuditoriaRepository.class.getMethods())
                .map(Method::getName)
                .anyMatch(n -> n.startsWith("delete") || n.equals("saveAll"));
        assertFalse(tieneDelete);
    }

    @Test
    @DisplayName("Criterio 1: la fecha y hora se asignan automáticamente al insertar")
    void prePersist_asignaFechaHora() {
        Auditoria registro = Auditoria.builder().accion("ROL_CREADO").entidad("roles").build();
        registro.antesDeInsertar();
        assertNotNull(registro.getFechaHora());
    }
}
