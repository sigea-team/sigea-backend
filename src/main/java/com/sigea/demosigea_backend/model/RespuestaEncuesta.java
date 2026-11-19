package com.sigea.demosigea_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Respuesta de un participante a una pregunta (HU-25, Criterio 2). Mapea la tabla {@code respuestas_encuesta}.
 * <p>
 * En una encuesta anónima {@code persona} es {@code null} y la fecha se guarda sin hora, para no poder
 * cruzarla con el registro de quién respondió ({@link ParticipacionEncuesta}).
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "respuestas_encuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespuestaEncuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "respuesta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private PreguntaEncuesta pregunta;

    /** Persona que respondió; {@code null} si la encuesta es anónima. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    /** Texto legible de la respuesta (el valor, la opción elegida o el texto libre). */
    @Column(name = "respuesta", columnDefinition = "TEXT")
    private String respuesta;

    /** Valor elegido en las preguntas de escala. */
    @Column(name = "valor_escala")
    private Integer valorEscala;

    /** Opción elegida en las preguntas de opción. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id")
    private OpcionPreguntaEncuesta opcion;

    @Column(name = "fecha_respuesta", nullable = false)
    private LocalDateTime fechaRespuesta;
}
