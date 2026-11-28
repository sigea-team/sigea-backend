package com.sigea.demosigea_backend.service;

import com.sigea.demosigea_backend.dto.indicador.DashboardIndicadoresResponse;
import com.sigea.demosigea_backend.exception.RecursoNoEncontradoException;
import com.sigea.demosigea_backend.model.Evento;
import com.sigea.demosigea_backend.repository.EventoRepository;
import com.sigea.demosigea_backend.repository.IndicadorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Indicadores de participación y dashboard de un evento (HU-29, RF50, RF51, CU-25).
 * <ul>
 *   <li><b>Criterio 1:</b> calcula los indicadores del evento con la información de inscripciones, asistencia,
 *       actividades, propuestas, encuestas de satisfacción y presupuesto, y los entrega para la tabla
 *       ({@code indicadores}) y para las gráficas ({@code series}).</li>
 *   <li><b>Criterio 2:</b> lo que no tiene datos suficientes llega marcado como no disponible, con el motivo, sin
 *       error ({@link ReglasIndicadoresEvento}).</li>
 *   <li><b>Criterio 3:</b> nada se guarda: cada solicitud vuelve a calcular todo con la información más reciente.
 *       Las consultas corren en una transacción de solo lectura con {@code REPEATABLE READ}, así que todas ven la
 *       misma foto de la base de datos aunque se registren datos mientras se calculan.</li>
 * </ul>
 * <p>
 * Los indicadores generales usan el mismo cálculo del cierre formal (HU-28). En un evento archivado la información
 * ya no cambia (triggers de HU-28), así que el resultado coincide con los indicadores guardados en su memoria
 * histórica; la respuesta lo indica con {@code archivado}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Service
@RequiredArgsConstructor
public class IndicadorEventoService {

    private final EventoRepository eventoRepository;
    private final IndicadorEventoRepository indicadorEventoRepository;
    private final Clock clock;

    /**
     * Calcula los indicadores y las series del dashboard de un evento.
     *
     * @param eventoId evento.
     * @return indicadores para la tabla y series para las gráficas, calculados en este momento.
     * @throws RecursoNoEncontradoException si el evento no existe.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardIndicadoresResponse generar(Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el evento con ID: " + eventoId));

        ReglasIndicadoresEvento.Datos datos = new ReglasIndicadoresEvento.Datos(
                indicadorEventoRepository.calcular(eventoId),
                indicadorEventoRepository.inscritosPorCategoria(eventoId),
                indicadorEventoRepository.inscripcionesPorFecha(eventoId),
                indicadorEventoRepository.asistenciaPorActividad(eventoId),
                indicadorEventoRepository.propuestasPorEstado(eventoId),
                indicadorEventoRepository.satisfaccionPorEncuesta(eventoId),
                indicadorEventoRepository.presupuestoPorRubro(eventoId));

        return ReglasIndicadoresEvento.construir(evento.getId(), evento.getNombre(), evento.getEstado(),
                evento.isArchivado(), LocalDateTime.now(clock), datos);
    }
}
