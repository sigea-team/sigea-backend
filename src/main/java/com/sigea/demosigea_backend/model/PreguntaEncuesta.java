package com.sigea.demosigea_backend.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pregunta de una encuesta de satisfacción (HU-25). Mapea la tabla {@code preguntas_encuesta}.
 * <p>
 * Las de tipo {@code escala} tienen rango (CHECK {@code preguntas_encuesta_escala_check}: 0 a 10, mínimo &lt; máximo);
 * las de tipo {@code opcion} tienen sus opciones en {@code opciones_pregunta_encuesta}.
 * </p>
 *
 * @author SIGEA Development Team
 * @version 1.0
 * @since 2026
 */
@Entity
@Table(name = "preguntas_encuesta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreguntaEncuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pregunta_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encuesta_id", nullable = false)
    private Encuesta encuesta;

    /** Enunciado de la pregunta. */
    @Column(name = "texto", columnDefinition = "TEXT", nullable = false)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_respuesta", length = 20, nullable = false)
    private TipoRespuestaEncuesta tipoRespuesta;

    /** Posición de la pregunta (1, 2, 3...). */
    @Builder.Default
    @Column(name = "orden", nullable = false)
    private Integer orden = 1;

    /** Si es {@code true}, no se puede enviar la encuesta sin responderla. */
    @Builder.Default
    @Column(name = "obligatoria", nullable = false)
    private boolean obligatoria = true;

    /** Valor mínimo de la escala (solo tipo escala). */
    @Column(name = "escala_minima")
    private Integer escalaMinima;

    /** Valor máximo de la escala (solo tipo escala). */
    @Column(name = "escala_maxima")
    private Integer escalaMaxima;

    /** Opciones de respuesta (solo tipo opcion). */
    @Builder.Default
    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    private List<OpcionPreguntaEncuesta> opciones = new ArrayList<>();

    /** Agrega una opción y fija la relación inversa. */
    public void agregarOpcion(OpcionPreguntaEncuesta opcion) {
        opcion.setPregunta(this);
        opciones.add(opcion);
    }
}
