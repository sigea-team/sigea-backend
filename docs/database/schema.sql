--
-- PostgreSQL database dump
--

\restrict OFlTchfxHsnuhTBQk2dW01oY2VobwdDqe888g88ovuRCmGBQjbeqdUpMwgNi3Nm

-- Dumped from database version 18.6 (6569466)
-- Dumped by pg_dump version 18.6

-- Started on 2026-09-27 14:15:14

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 277 (class 1259 OID 25004)
-- Name: actividades; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.actividades (
    actividad_id integer NOT NULL,
    evento_id integer NOT NULL,
    propuesta_id integer,
    tipo_actividad_id integer NOT NULL,
    linea_tematica_id integer,
    nombre character varying(200) NOT NULL,
    fecha date NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    sala_id integer NOT NULL,
    ponente_id integer,
    modalidad character varying(20),
    permite_simultaneidad boolean DEFAULT false NOT NULL,
    estado character varying(20) DEFAULT 'programada'::character varying NOT NULL,
    CONSTRAINT actividades_check CHECK ((hora_fin > hora_inicio)),
    CONSTRAINT actividades_modalidad_check CHECK (((modalidad)::text = ANY ((ARRAY['presencial'::character varying, 'virtual'::character varying, 'hibrida'::character varying])::text[])))
);


ALTER TABLE public.actividades OWNER TO neondb_owner;

--
-- TOC entry 276 (class 1259 OID 25003)
-- Name: actividades_actividad_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.actividades_actividad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.actividades_actividad_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4027 (class 0 OID 0)
-- Dependencies: 276
-- Name: actividades_actividad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.actividades_actividad_id_seq OWNED BY public.actividades.actividad_id;


--
-- TOC entry 303 (class 1259 OID 49153)
-- Name: afiliaciones; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.afiliaciones (
    id integer NOT NULL,
    nombre_afiliacion character varying(100) NOT NULL
);


ALTER TABLE public.afiliaciones OWNER TO neondb_owner;

--
-- TOC entry 302 (class 1259 OID 49152)
-- Name: afiliaciones_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.afiliaciones_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.afiliaciones_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4028 (class 0 OID 0)
-- Dependencies: 302
-- Name: afiliaciones_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.afiliaciones_id_seq OWNED BY public.afiliaciones.id;


--
-- TOC entry 279 (class 1259 OID 25025)
-- Name: agendas_publicadas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.agendas_publicadas (
    agenda_id integer NOT NULL,
    evento_id integer NOT NULL,
    fecha_dia date NOT NULL,
    fecha_publicacion timestamp without time zone,
    estado character varying(20) DEFAULT 'no_publicada'::character varying NOT NULL,
    CONSTRAINT agendas_publicadas_estado_check CHECK (((estado)::text = ANY ((ARRAY['no_publicada'::character varying, 'publicada'::character varying])::text[])))
);


ALTER TABLE public.agendas_publicadas OWNER TO neondb_owner;

--
-- TOC entry 278 (class 1259 OID 25024)
-- Name: agendas_publicadas_agenda_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.agendas_publicadas_agenda_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.agendas_publicadas_agenda_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4029 (class 0 OID 0)
-- Dependencies: 278
-- Name: agendas_publicadas_agenda_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.agendas_publicadas_agenda_id_seq OWNED BY public.agendas_publicadas.agenda_id;


--
-- TOC entry 263 (class 1259 OID 24900)
-- Name: asignaciones_evaluacion; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.asignaciones_evaluacion (
    asignacion_id integer NOT NULL,
    propuesta_id integer NOT NULL,
    comite_evaluador_id integer NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    estado character varying(20) DEFAULT 'pendiente'::character varying NOT NULL,
    CONSTRAINT asignaciones_evaluacion_estado_check CHECK (((estado)::text = ANY ((ARRAY['pendiente'::character varying, 'en_proceso'::character varying, 'completada'::character varying])::text[])))
);


ALTER TABLE public.asignaciones_evaluacion OWNER TO neondb_owner;

--
-- TOC entry 262 (class 1259 OID 24899)
-- Name: asignaciones_evaluacion_asignacion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.asignaciones_evaluacion_asignacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.asignaciones_evaluacion_asignacion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4030 (class 0 OID 0)
-- Dependencies: 262
-- Name: asignaciones_evaluacion_asignacion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.asignaciones_evaluacion_asignacion_id_seq OWNED BY public.asignaciones_evaluacion.asignacion_id;


--
-- TOC entry 287 (class 1259 OID 25082)
-- Name: asistencias; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.asistencias (
    asistencia_id integer NOT NULL,
    inscripcion_id integer NOT NULL,
    actividad_id integer,
    fecha_hora_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    metodo_registro character varying(20) NOT NULL,
    CONSTRAINT asistencias_metodo_registro_check CHECK (((metodo_registro)::text = ANY ((ARRAY['qr'::character varying, 'manual'::character varying])::text[])))
);


ALTER TABLE public.asistencias OWNER TO neondb_owner;

--
-- TOC entry 286 (class 1259 OID 25081)
-- Name: asistencias_asistencia_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.asistencias_asistencia_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.asistencias_asistencia_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4031 (class 0 OID 0)
-- Dependencies: 286
-- Name: asistencias_asistencia_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.asistencias_asistencia_id_seq OWNED BY public.asistencias.asistencia_id;


--
-- TOC entry 232 (class 1259 OID 24676)
-- Name: auditoria; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.auditoria (
    auditoria_id integer NOT NULL,
    usuario_id integer,
    accion character varying(100) NOT NULL,
    entidad character varying(100) NOT NULL,
    entidad_id integer,
    fecha_hora timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    detalle text
);


ALTER TABLE public.auditoria OWNER TO neondb_owner;

--
-- TOC entry 231 (class 1259 OID 24675)
-- Name: auditoria_auditoria_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.auditoria_auditoria_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.auditoria_auditoria_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4032 (class 0 OID 0)
-- Dependencies: 231
-- Name: auditoria_auditoria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.auditoria_auditoria_id_seq OWNED BY public.auditoria.auditoria_id;


--
-- TOC entry 295 (class 1259 OID 25137)
-- Name: certificados; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.certificados (
    certificado_id integer NOT NULL,
    persona_id integer NOT NULL,
    evento_id integer NOT NULL,
    actividad_id integer,
    tipo_certificado character varying(20) NOT NULL,
    porcentaje_asistencia numeric(5,2),
    fecha_generacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archivo_url character varying(255) NOT NULL,
    CONSTRAINT certificados_tipo_certificado_check CHECK (((tipo_certificado)::text = ANY ((ARRAY['asistente'::character varying, 'ponente'::character varying, 'evaluador'::character varying, 'organizador'::character varying])::text[])))
);


ALTER TABLE public.certificados OWNER TO neondb_owner;

--
-- TOC entry 294 (class 1259 OID 25136)
-- Name: certificados_certificado_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.certificados_certificado_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.certificados_certificado_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4033 (class 0 OID 0)
-- Dependencies: 294
-- Name: certificados_certificado_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.certificados_certificado_id_seq OWNED BY public.certificados.certificado_id;


--
-- TOC entry 285 (class 1259 OID 25064)
-- Name: codigos_qr; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.codigos_qr (
    qr_id integer NOT NULL,
    inscripcion_id integer NOT NULL,
    codigo character varying(100) NOT NULL,
    fecha_generacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    vigente boolean DEFAULT true NOT NULL
);


ALTER TABLE public.codigos_qr OWNER TO neondb_owner;

--
-- TOC entry 284 (class 1259 OID 25063)
-- Name: codigos_qr_qr_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.codigos_qr_qr_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.codigos_qr_qr_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4034 (class 0 OID 0)
-- Dependencies: 284
-- Name: codigos_qr_qr_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.codigos_qr_qr_id_seq OWNED BY public.codigos_qr.qr_id;


--
-- TOC entry 236 (class 1259 OID 24708)
-- Name: comite_organizador; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.comite_organizador (
    comite_id integer NOT NULL,
    evento_id integer NOT NULL,
    persona_id integer NOT NULL,
    rol_comite character varying(50) NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


ALTER TABLE public.comite_organizador OWNER TO neondb_owner;

--
-- TOC entry 235 (class 1259 OID 24707)
-- Name: comite_organizador_comite_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.comite_organizador_comite_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comite_organizador_comite_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4035 (class 0 OID 0)
-- Dependencies: 235
-- Name: comite_organizador_comite_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.comite_organizador_comite_id_seq OWNED BY public.comite_organizador.comite_id;


--
-- TOC entry 261 (class 1259 OID 24890)
-- Name: comites_evaluadores; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.comites_evaluadores (
    comite_evaluador_id integer NOT NULL,
    convocatoria_id integer NOT NULL,
    persona_id integer NOT NULL,
    area_experticia character varying(150)
);


ALTER TABLE public.comites_evaluadores OWNER TO neondb_owner;

--
-- TOC entry 260 (class 1259 OID 24889)
-- Name: comites_evaluadores_comite_evaluador_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.comites_evaluadores_comite_evaluador_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comites_evaluadores_comite_evaluador_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4036 (class 0 OID 0)
-- Dependencies: 260
-- Name: comites_evaluadores_comite_evaluador_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.comites_evaluadores_comite_evaluador_id_seq OWNED BY public.comites_evaluadores.comite_evaluador_id;


--
-- TOC entry 248 (class 1259 OID 24793)
-- Name: convocatorias; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.convocatorias (
    convocatoria_id integer NOT NULL,
    evento_id integer NOT NULL,
    titulo character varying(150) NOT NULL,
    descripcion text,
    requisitos text,
    fecha_apertura timestamp without time zone NOT NULL,
    fecha_cierre timestamp without time zone NOT NULL,
    estado character varying(20) DEFAULT 'borrador'::character varying NOT NULL,
    CONSTRAINT convocatorias_check CHECK ((fecha_cierre > fecha_apertura)),
    CONSTRAINT convocatorias_estado_check CHECK (((estado)::text = ANY ((ARRAY['borrador'::character varying, 'publicada'::character varying, 'cerrada'::character varying])::text[])))
);


ALTER TABLE public.convocatorias OWNER TO neondb_owner;

--
-- TOC entry 247 (class 1259 OID 24792)
-- Name: convocatorias_convocatoria_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.convocatorias_convocatoria_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.convocatorias_convocatoria_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4037 (class 0 OID 0)
-- Dependencies: 247
-- Name: convocatorias_convocatoria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.convocatorias_convocatoria_id_seq OWNED BY public.convocatorias.convocatoria_id;


--
-- TOC entry 259 (class 1259 OID 24878)
-- Name: criterios_rubrica; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.criterios_rubrica (
    criterio_id integer NOT NULL,
    rubrica_id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    peso_porcentual numeric(5,2) NOT NULL,
    CONSTRAINT criterios_rubrica_peso_porcentual_check CHECK (((peso_porcentual > (0)::numeric) AND (peso_porcentual <= (100)::numeric)))
);


ALTER TABLE public.criterios_rubrica OWNER TO neondb_owner;

--
-- TOC entry 258 (class 1259 OID 24877)
-- Name: criterios_rubrica_criterio_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.criterios_rubrica_criterio_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.criterios_rubrica_criterio_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4038 (class 0 OID 0)
-- Dependencies: 258
-- Name: criterios_rubrica_criterio_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.criterios_rubrica_criterio_id_seq OWNED BY public.criterios_rubrica.criterio_id;


--
-- TOC entry 300 (class 1259 OID 25519)
-- Name: databasechangelog; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.databasechangelog (
    id character varying(255) NOT NULL,
    author character varying(255) NOT NULL,
    filename character varying(255) NOT NULL,
    dateexecuted timestamp without time zone NOT NULL,
    orderexecuted integer NOT NULL,
    exectype character varying(10) NOT NULL,
    md5sum character varying(35),
    description character varying(255),
    comments character varying(255),
    tag character varying(255),
    liquibase character varying(20),
    contexts character varying(255),
    labels character varying(255),
    deployment_id character varying(10)
);


ALTER TABLE public.databasechangelog OWNER TO neondb_owner;

--
-- TOC entry 301 (class 1259 OID 25530)
-- Name: databasechangeloglock; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.databasechangeloglock (
    id integer NOT NULL,
    locked boolean NOT NULL,
    lockgranted timestamp without time zone,
    lockedby character varying(255)
);


ALTER TABLE public.databasechangeloglock OWNER TO neondb_owner;

--
-- TOC entry 275 (class 1259 OID 24990)
-- Name: disponibilidad_ponentes; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.disponibilidad_ponentes (
    disponibilidad_id integer NOT NULL,
    persona_id integer NOT NULL,
    evento_id integer NOT NULL,
    fecha date NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    CONSTRAINT disponibilidad_ponentes_check CHECK ((hora_fin > hora_inicio))
);


ALTER TABLE public.disponibilidad_ponentes OWNER TO neondb_owner;

--
-- TOC entry 274 (class 1259 OID 24989)
-- Name: disponibilidad_ponentes_disponibilidad_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.disponibilidad_ponentes_disponibilidad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.disponibilidad_ponentes_disponibilidad_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4039 (class 0 OID 0)
-- Dependencies: 274
-- Name: disponibilidad_ponentes_disponibilidad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.disponibilidad_ponentes_disponibilidad_id_seq OWNED BY public.disponibilidad_ponentes.disponibilidad_id;


--
-- TOC entry 273 (class 1259 OID 24977)
-- Name: disponibilidad_salas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.disponibilidad_salas (
    disponibilidad_id integer NOT NULL,
    sala_id integer NOT NULL,
    fecha date NOT NULL,
    hora_inicio time without time zone NOT NULL,
    hora_fin time without time zone NOT NULL,
    CONSTRAINT disponibilidad_salas_check CHECK ((hora_fin > hora_inicio))
);


ALTER TABLE public.disponibilidad_salas OWNER TO neondb_owner;

--
-- TOC entry 272 (class 1259 OID 24976)
-- Name: disponibilidad_salas_disponibilidad_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.disponibilidad_salas_disponibilidad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.disponibilidad_salas_disponibilidad_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4040 (class 0 OID 0)
-- Dependencies: 272
-- Name: disponibilidad_salas_disponibilidad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.disponibilidad_salas_disponibilidad_id_seq OWNED BY public.disponibilidad_salas.disponibilidad_id;


--
-- TOC entry 289 (class 1259 OID 25095)
-- Name: encuestas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.encuestas (
    encuesta_id integer NOT NULL,
    evento_id integer NOT NULL,
    actividad_id integer,
    titulo character varying(150) NOT NULL,
    estado character varying(20) DEFAULT 'activa'::character varying NOT NULL
);


ALTER TABLE public.encuestas OWNER TO neondb_owner;

--
-- TOC entry 288 (class 1259 OID 25094)
-- Name: encuestas_encuesta_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.encuestas_encuesta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.encuestas_encuesta_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4041 (class 0 OID 0)
-- Dependencies: 288
-- Name: encuestas_encuesta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.encuestas_encuesta_id_seq OWNED BY public.encuestas.encuesta_id;


--
-- TOC entry 265 (class 1259 OID 24915)
-- Name: evaluaciones; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.evaluaciones (
    evaluacion_id integer NOT NULL,
    asignacion_id integer NOT NULL,
    criterio_id integer NOT NULL,
    calificacion numeric(5,2) NOT NULL,
    observaciones text,
    fecha_evaluacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.evaluaciones OWNER TO neondb_owner;

--
-- TOC entry 264 (class 1259 OID 24914)
-- Name: evaluaciones_evaluacion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.evaluaciones_evaluacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.evaluaciones_evaluacion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4042 (class 0 OID 0)
-- Dependencies: 264
-- Name: evaluaciones_evaluacion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.evaluaciones_evaluacion_id_seq OWNED BY public.evaluaciones.evaluacion_id;


--
-- TOC entry 234 (class 1259 OID 24690)
-- Name: eventos; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.eventos (
    evento_id integer NOT NULL,
    nombre character varying(200) NOT NULL,
    objetivo text,
    descripcion text,
    tipo character varying(50),
    modalidad character varying(20),
    fecha_inicio date NOT NULL,
    fecha_fin date NOT NULL,
    semestre character varying(10),
    estado character varying(30) DEFAULT 'en_configuracion'::character varying NOT NULL,
    evento_base_id integer,
    CONSTRAINT eventos_check CHECK ((fecha_fin >= fecha_inicio)),
    CONSTRAINT eventos_estado_check CHECK (((estado)::text = ANY ((ARRAY['en_configuracion'::character varying, 'habilitado'::character varying, 'en_ejecucion'::character varying, 'cerrado'::character varying])::text[]))),
    CONSTRAINT eventos_modalidad_check CHECK (((modalidad)::text = ANY ((ARRAY['presencial'::character varying, 'virtual'::character varying, 'hibrida'::character varying])::text[])))
);


ALTER TABLE public.eventos OWNER TO neondb_owner;

--
-- TOC entry 233 (class 1259 OID 24689)
-- Name: eventos_evento_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.eventos_evento_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.eventos_evento_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4043 (class 0 OID 0)
-- Dependencies: 233
-- Name: eventos_evento_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.eventos_evento_id_seq OWNED BY public.eventos.evento_id;


--
-- TOC entry 246 (class 1259 OID 24778)
-- Name: gastos_ejecutados; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.gastos_ejecutados (
    gasto_id integer NOT NULL,
    rubro_id integer NOT NULL,
    descripcion character varying(255),
    valor numeric(14,2) NOT NULL,
    fecha_gasto date NOT NULL,
    soporte_url character varying(255),
    registrado_por integer NOT NULL,
    CONSTRAINT gastos_ejecutados_valor_check CHECK ((valor >= (0)::numeric))
);


ALTER TABLE public.gastos_ejecutados OWNER TO neondb_owner;

--
-- TOC entry 245 (class 1259 OID 24777)
-- Name: gastos_ejecutados_gasto_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.gastos_ejecutados_gasto_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.gastos_ejecutados_gasto_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4044 (class 0 OID 0)
-- Dependencies: 245
-- Name: gastos_ejecutados_gasto_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.gastos_ejecutados_gasto_id_seq OWNED BY public.gastos_ejecutados.gasto_id;


--
-- TOC entry 297 (class 1259 OID 25152)
-- Name: indicadores; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.indicadores (
    indicador_id integer NOT NULL,
    evento_id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    valor numeric(14,2) NOT NULL,
    unidad character varying(20),
    fecha_calculo timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.indicadores OWNER TO neondb_owner;

--
-- TOC entry 296 (class 1259 OID 25151)
-- Name: indicadores_indicador_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.indicadores_indicador_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.indicadores_indicador_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4045 (class 0 OID 0)
-- Dependencies: 296
-- Name: indicadores_indicador_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.indicadores_indicador_id_seq OWNED BY public.indicadores.indicador_id;


--
-- TOC entry 283 (class 1259 OID 25052)
-- Name: inscripciones_actividad; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.inscripciones_actividad (
    inscripcion_actividad_id integer NOT NULL,
    inscripcion_id integer NOT NULL,
    actividad_id integer NOT NULL,
    fecha_inscripcion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.inscripciones_actividad OWNER TO neondb_owner;

--
-- TOC entry 282 (class 1259 OID 25051)
-- Name: inscripciones_actividad_inscripcion_actividad_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.inscripciones_actividad_inscripcion_actividad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.inscripciones_actividad_inscripcion_actividad_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4046 (class 0 OID 0)
-- Dependencies: 282
-- Name: inscripciones_actividad_inscripcion_actividad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.inscripciones_actividad_inscripcion_actividad_id_seq OWNED BY public.inscripciones_actividad.inscripcion_actividad_id;


--
-- TOC entry 281 (class 1259 OID 25038)
-- Name: inscripciones_evento; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.inscripciones_evento (
    inscripcion_id integer NOT NULL,
    persona_id integer NOT NULL,
    evento_id integer NOT NULL,
    categoria_participante character varying(30) NOT NULL,
    fecha_inscripcion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT inscripciones_evento_categoria_participante_check CHECK (((categoria_participante)::text = ANY ((ARRAY['estudiante'::character varying, 'egresado'::character varying, 'docente'::character varying, 'particular'::character varying])::text[])))
);


ALTER TABLE public.inscripciones_evento OWNER TO neondb_owner;

--
-- TOC entry 280 (class 1259 OID 25037)
-- Name: inscripciones_evento_inscripcion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.inscripciones_evento_inscripcion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.inscripciones_evento_inscripcion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4047 (class 0 OID 0)
-- Dependencies: 280
-- Name: inscripciones_evento_inscripcion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.inscripciones_evento_inscripcion_id_seq OWNED BY public.inscripciones_evento.inscripcion_id;


--
-- TOC entry 240 (class 1259 OID 24733)
-- Name: lineas_tematicas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.lineas_tematicas (
    linea_tematica_id integer NOT NULL,
    evento_id integer NOT NULL,
    nombre character varying(120) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE public.lineas_tematicas OWNER TO neondb_owner;

--
-- TOC entry 239 (class 1259 OID 24732)
-- Name: lineas_tematicas_linea_tematica_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.lineas_tematicas_linea_tematica_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.lineas_tematicas_linea_tematica_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4048 (class 0 OID 0)
-- Dependencies: 239
-- Name: lineas_tematicas_linea_tematica_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.lineas_tematicas_linea_tematica_id_seq OWNED BY public.lineas_tematicas.linea_tematica_id;


--
-- TOC entry 299 (class 1259 OID 25165)
-- Name: memorias_historicas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.memorias_historicas (
    memoria_id integer NOT NULL,
    evento_id integer NOT NULL,
    fecha_consolidacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archivo_url character varying(255),
    resumen text
);


ALTER TABLE public.memorias_historicas OWNER TO neondb_owner;

--
-- TOC entry 298 (class 1259 OID 25164)
-- Name: memorias_historicas_memoria_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.memorias_historicas_memoria_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.memorias_historicas_memoria_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4049 (class 0 OID 0)
-- Dependencies: 298
-- Name: memorias_historicas_memoria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.memorias_historicas_memoria_id_seq OWNED BY public.memorias_historicas.memoria_id;


--
-- TOC entry 269 (class 1259 OID 24946)
-- Name: observaciones_propuesta; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.observaciones_propuesta (
    observacion_id integer NOT NULL,
    version_id integer NOT NULL,
    comite_evaluador_id integer NOT NULL,
    descripcion text NOT NULL,
    fecha timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    resuelto boolean DEFAULT false NOT NULL
);


ALTER TABLE public.observaciones_propuesta OWNER TO neondb_owner;

--
-- TOC entry 268 (class 1259 OID 24945)
-- Name: observaciones_propuesta_observacion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.observaciones_propuesta_observacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.observaciones_propuesta_observacion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4050 (class 0 OID 0)
-- Dependencies: 268
-- Name: observaciones_propuesta_observacion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.observaciones_propuesta_observacion_id_seq OWNED BY public.observaciones_propuesta.observacion_id;


--
-- TOC entry 250 (class 1259 OID 24811)
-- Name: participaciones_conferencista; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.participaciones_conferencista (
    participacion_id integer NOT NULL,
    persona_id integer NOT NULL,
    evento_id integer NOT NULL,
    tipo_participacion character varying(20) NOT NULL,
    tema character varying(200),
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT participaciones_conferencista_tipo_participacion_check CHECK (((tipo_participacion)::text = ANY ((ARRAY['conferencista'::character varying, 'ponente'::character varying])::text[])))
);


ALTER TABLE public.participaciones_conferencista OWNER TO neondb_owner;

--
-- TOC entry 249 (class 1259 OID 24810)
-- Name: participaciones_conferencista_participacion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.participaciones_conferencista_participacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.participaciones_conferencista_participacion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4051 (class 0 OID 0)
-- Dependencies: 249
-- Name: participaciones_conferencista_participacion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.participaciones_conferencista_participacion_id_seq OWNED BY public.participaciones_conferencista.participacion_id;


--
-- TOC entry 226 (class 1259 OID 24629)
-- Name: permisos; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.permisos (
    permiso_id integer NOT NULL,
    codigo character varying(50) NOT NULL,
    modulo character varying(50) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE public.permisos OWNER TO neondb_owner;

--
-- TOC entry 225 (class 1259 OID 24628)
-- Name: permisos_permiso_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.permisos_permiso_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.permisos_permiso_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4052 (class 0 OID 0)
-- Dependencies: 225
-- Name: permisos_permiso_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.permisos_permiso_id_seq OWNED BY public.permisos.permiso_id;


--
-- TOC entry 220 (class 1259 OID 24577)
-- Name: personas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.personas (
    persona_id bigint NOT NULL,
    tipo_documento character varying(20) NOT NULL,
    numero_documento character varying(30) NOT NULL,
    nombres character varying(100) NOT NULL,
    apellidos character varying(100) NOT NULL,
    correo character varying(150) NOT NULL,
    telefono character varying(30),
    afiliacion_id integer
);


ALTER TABLE public.personas OWNER TO neondb_owner;

--
-- TOC entry 219 (class 1259 OID 24576)
-- Name: personas_persona_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.personas_persona_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.personas_persona_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4053 (class 0 OID 0)
-- Dependencies: 219
-- Name: personas_persona_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.personas_persona_id_seq OWNED BY public.personas.persona_id;


--
-- TOC entry 291 (class 1259 OID 25107)
-- Name: preguntas_encuesta; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.preguntas_encuesta (
    pregunta_id integer NOT NULL,
    encuesta_id integer NOT NULL,
    texto text NOT NULL,
    tipo_respuesta character varying(20) NOT NULL,
    orden integer DEFAULT 1 NOT NULL,
    CONSTRAINT preguntas_encuesta_tipo_respuesta_check CHECK (((tipo_respuesta)::text = ANY ((ARRAY['escala'::character varying, 'texto'::character varying, 'opcion'::character varying])::text[])))
);


ALTER TABLE public.preguntas_encuesta OWNER TO neondb_owner;

--
-- TOC entry 290 (class 1259 OID 25106)
-- Name: preguntas_encuesta_pregunta_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.preguntas_encuesta_pregunta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.preguntas_encuesta_pregunta_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4054 (class 0 OID 0)
-- Dependencies: 290
-- Name: preguntas_encuesta_pregunta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.preguntas_encuesta_pregunta_id_seq OWNED BY public.preguntas_encuesta.pregunta_id;


--
-- TOC entry 244 (class 1259 OID 24759)
-- Name: presupuestos_aprobados; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.presupuestos_aprobados (
    aprobacion_id integer NOT NULL,
    evento_id integer NOT NULL,
    valor_total_aprobado numeric(14,2) NOT NULL,
    evidencia_url character varying(255) NOT NULL,
    fecha_aprobacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    aprobado_por integer NOT NULL,
    estado character varying(20) DEFAULT 'aprobado'::character varying NOT NULL,
    CONSTRAINT presupuestos_aprobados_valor_total_aprobado_check CHECK ((valor_total_aprobado >= (0)::numeric))
);


ALTER TABLE public.presupuestos_aprobados OWNER TO neondb_owner;

--
-- TOC entry 243 (class 1259 OID 24758)
-- Name: presupuestos_aprobados_aprobacion_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.presupuestos_aprobados_aprobacion_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.presupuestos_aprobados_aprobacion_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4055 (class 0 OID 0)
-- Dependencies: 243
-- Name: presupuestos_aprobados_aprobacion_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.presupuestos_aprobados_aprobacion_id_seq OWNED BY public.presupuestos_aprobados.aprobacion_id;


--
-- TOC entry 252 (class 1259 OID 24825)
-- Name: propuestas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.propuestas (
    propuesta_id integer NOT NULL,
    convocatoria_id integer NOT NULL,
    titulo character varying(200) NOT NULL,
    resumen text,
    palabras_clave character varying(255),
    linea_tematica_id integer,
    estado character varying(20) DEFAULT 'recibida'::character varying NOT NULL,
    fecha_envio timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT propuestas_estado_check CHECK (((estado)::text = ANY ((ARRAY['recibida'::character varying, 'en_evaluacion'::character varying, 'aprobada'::character varying, 'ajustes'::character varying, 'rechazada'::character varying])::text[])))
);


ALTER TABLE public.propuestas OWNER TO neondb_owner;

--
-- TOC entry 253 (class 1259 OID 24841)
-- Name: propuestas_autores; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.propuestas_autores (
    propuesta_id integer NOT NULL,
    persona_id integer NOT NULL,
    orden integer DEFAULT 1 NOT NULL
);


ALTER TABLE public.propuestas_autores OWNER TO neondb_owner;

--
-- TOC entry 251 (class 1259 OID 24824)
-- Name: propuestas_propuesta_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.propuestas_propuesta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.propuestas_propuesta_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4056 (class 0 OID 0)
-- Dependencies: 251
-- Name: propuestas_propuesta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.propuestas_propuesta_id_seq OWNED BY public.propuestas.propuesta_id;


--
-- TOC entry 293 (class 1259 OID 25123)
-- Name: respuestas_encuesta; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.respuestas_encuesta (
    respuesta_id integer NOT NULL,
    pregunta_id integer NOT NULL,
    persona_id integer NOT NULL,
    respuesta text,
    fecha_respuesta timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.respuestas_encuesta OWNER TO neondb_owner;

--
-- TOC entry 292 (class 1259 OID 25122)
-- Name: respuestas_encuesta_respuesta_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.respuestas_encuesta_respuesta_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.respuestas_encuesta_respuesta_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4057 (class 0 OID 0)
-- Dependencies: 292
-- Name: respuestas_encuesta_respuesta_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.respuestas_encuesta_respuesta_id_seq OWNED BY public.respuestas_encuesta.respuesta_id;


--
-- TOC entry 267 (class 1259 OID 24930)
-- Name: resultados_evaluacion; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.resultados_evaluacion (
    resultado_id integer NOT NULL,
    propuesta_id integer NOT NULL,
    puntuacion_ponderada numeric(5,2) NOT NULL,
    clasificacion character varying(30) NOT NULL,
    fecha_calculo timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT resultados_evaluacion_clasificacion_check CHECK (((clasificacion)::text = ANY ((ARRAY['aprobada'::character varying, 'aprobada_con_ajustes'::character varying, 'rechazada'::character varying])::text[])))
);


ALTER TABLE public.resultados_evaluacion OWNER TO neondb_owner;

--
-- TOC entry 266 (class 1259 OID 24929)
-- Name: resultados_evaluacion_resultado_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.resultados_evaluacion_resultado_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.resultados_evaluacion_resultado_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4058 (class 0 OID 0)
-- Dependencies: 266
-- Name: resultados_evaluacion_resultado_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.resultados_evaluacion_resultado_id_seq OWNED BY public.resultados_evaluacion.resultado_id;


--
-- TOC entry 224 (class 1259 OID 24618)
-- Name: roles; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.roles (
    rol_id bigint NOT NULL,
    nombre character varying(50) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE public.roles OWNER TO neondb_owner;

--
-- TOC entry 227 (class 1259 OID 24640)
-- Name: roles_permisos; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.roles_permisos (
    rol_id integer NOT NULL,
    permiso_id integer NOT NULL
);


ALTER TABLE public.roles_permisos OWNER TO neondb_owner;

--
-- TOC entry 223 (class 1259 OID 24617)
-- Name: roles_rol_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.roles_rol_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.roles_rol_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4059 (class 0 OID 0)
-- Dependencies: 223
-- Name: roles_rol_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.roles_rol_id_seq OWNED BY public.roles.rol_id;


--
-- TOC entry 257 (class 1259 OID 24866)
-- Name: rubricas_evaluacion; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.rubricas_evaluacion (
    rubrica_id integer NOT NULL,
    convocatoria_id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    activo boolean DEFAULT true NOT NULL
);


ALTER TABLE public.rubricas_evaluacion OWNER TO neondb_owner;

--
-- TOC entry 256 (class 1259 OID 24865)
-- Name: rubricas_evaluacion_rubrica_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.rubricas_evaluacion_rubrica_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rubricas_evaluacion_rubrica_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4060 (class 0 OID 0)
-- Dependencies: 256
-- Name: rubricas_evaluacion_rubrica_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.rubricas_evaluacion_rubrica_id_seq OWNED BY public.rubricas_evaluacion.rubrica_id;


--
-- TOC entry 242 (class 1259 OID 24743)
-- Name: rubros_presupuestales; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.rubros_presupuestales (
    rubro_id integer NOT NULL,
    evento_id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    cantidad numeric(10,2) NOT NULL,
    valor_unitario_proyectado numeric(14,2) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT rubros_presupuestales_cantidad_check CHECK ((cantidad >= (0)::numeric)),
    CONSTRAINT rubros_presupuestales_valor_unitario_proyectado_check CHECK ((valor_unitario_proyectado >= (0)::numeric))
);


ALTER TABLE public.rubros_presupuestales OWNER TO neondb_owner;

--
-- TOC entry 241 (class 1259 OID 24742)
-- Name: rubros_presupuestales_rubro_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.rubros_presupuestales_rubro_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.rubros_presupuestales_rubro_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4061 (class 0 OID 0)
-- Dependencies: 241
-- Name: rubros_presupuestales_rubro_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.rubros_presupuestales_rubro_id_seq OWNED BY public.rubros_presupuestales.rubro_id;


--
-- TOC entry 271 (class 1259 OID 24963)
-- Name: salas; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.salas (
    sala_id integer NOT NULL,
    evento_id integer NOT NULL,
    nombre character varying(100) NOT NULL,
    ubicacion character varying(150),
    tipo character varying(20) NOT NULL,
    aforo_maximo integer NOT NULL,
    CONSTRAINT salas_aforo_maximo_check CHECK ((aforo_maximo > 0)),
    CONSTRAINT salas_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['fisica'::character varying, 'virtual'::character varying])::text[])))
);


ALTER TABLE public.salas OWNER TO neondb_owner;

--
-- TOC entry 270 (class 1259 OID 24962)
-- Name: salas_sala_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.salas_sala_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.salas_sala_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4062 (class 0 OID 0)
-- Dependencies: 270
-- Name: salas_sala_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.salas_sala_id_seq OWNED BY public.salas.sala_id;


--
-- TOC entry 238 (class 1259 OID 24723)
-- Name: tipos_actividad; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.tipos_actividad (
    tipo_actividad_id integer NOT NULL,
    evento_id integer NOT NULL,
    nombre character varying(80) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE public.tipos_actividad OWNER TO neondb_owner;

--
-- TOC entry 237 (class 1259 OID 24722)
-- Name: tipos_actividad_tipo_actividad_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.tipos_actividad_tipo_actividad_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tipos_actividad_tipo_actividad_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4063 (class 0 OID 0)
-- Dependencies: 237
-- Name: tipos_actividad_tipo_actividad_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.tipos_actividad_tipo_actividad_id_seq OWNED BY public.tipos_actividad.tipo_actividad_id;


--
-- TOC entry 230 (class 1259 OID 24657)
-- Name: tokens_recuperacion; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.tokens_recuperacion (
    token_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    token character varying(255) NOT NULL,
    tipo character varying(20) NOT NULL,
    fecha_generacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_expiracion timestamp without time zone NOT NULL,
    usado boolean DEFAULT false NOT NULL,
    CONSTRAINT tokens_recuperacion_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['verificacion'::character varying, 'recuperacion'::character varying])::text[])))
);


ALTER TABLE public.tokens_recuperacion OWNER TO neondb_owner;

--
-- TOC entry 229 (class 1259 OID 24656)
-- Name: tokens_recuperacion_token_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.tokens_recuperacion_token_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.tokens_recuperacion_token_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4064 (class 0 OID 0)
-- Dependencies: 229
-- Name: tokens_recuperacion_token_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.tokens_recuperacion_token_id_seq OWNED BY public.tokens_recuperacion.token_id;


--
-- TOC entry 222 (class 1259 OID 24596)
-- Name: usuarios; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.usuarios (
    usuario_id bigint NOT NULL,
    persona_id bigint NOT NULL,
    contrasena_hash character varying(255) NOT NULL,
    correo_verificado boolean DEFAULT false NOT NULL,
    estado character varying(20) DEFAULT 'activo'::character varying NOT NULL,
    fecha_registro timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    intentos_fallidos integer DEFAULT 0 NOT NULL,
    bloqueado_hasta timestamp without time zone,
    CONSTRAINT usuarios_estado_check CHECK (((estado)::text = ANY ((ARRAY['activo'::character varying, 'bloqueado'::character varying, 'inactivo'::character varying])::text[])))
);


ALTER TABLE public.usuarios OWNER TO neondb_owner;

--
-- TOC entry 228 (class 1259 OID 24647)
-- Name: usuarios_roles; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.usuarios_roles (
    usuario_id bigint NOT NULL,
    rol_id bigint NOT NULL,
    fecha_asignacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.usuarios_roles OWNER TO neondb_owner;

--
-- TOC entry 221 (class 1259 OID 24595)
-- Name: usuarios_usuario_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.usuarios_usuario_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuarios_usuario_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4065 (class 0 OID 0)
-- Dependencies: 221
-- Name: usuarios_usuario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.usuarios_usuario_id_seq OWNED BY public.usuarios.usuario_id;


--
-- TOC entry 255 (class 1259 OID 24851)
-- Name: versiones_propuesta; Type: TABLE; Schema: public; Owner: neondb_owner
--

CREATE TABLE public.versiones_propuesta (
    version_id integer NOT NULL,
    propuesta_id integer NOT NULL,
    numero_version integer NOT NULL,
    documento_url character varying(255) NOT NULL,
    observaciones text,
    fecha_creacion timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.versiones_propuesta OWNER TO neondb_owner;

--
-- TOC entry 254 (class 1259 OID 24850)
-- Name: versiones_propuesta_version_id_seq; Type: SEQUENCE; Schema: public; Owner: neondb_owner
--

CREATE SEQUENCE public.versiones_propuesta_version_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.versiones_propuesta_version_id_seq OWNER TO neondb_owner;

--
-- TOC entry 4066 (class 0 OID 0)
-- Dependencies: 254
-- Name: versiones_propuesta_version_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: neondb_owner
--

ALTER SEQUENCE public.versiones_propuesta_version_id_seq OWNED BY public.versiones_propuesta.version_id;


--
-- TOC entry 3540 (class 2604 OID 25007)
-- Name: actividades actividad_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades ALTER COLUMN actividad_id SET DEFAULT nextval('public.actividades_actividad_id_seq'::regclass);


--
-- TOC entry 3566 (class 2604 OID 49156)
-- Name: afiliaciones id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.afiliaciones ALTER COLUMN id SET DEFAULT nextval('public.afiliaciones_id_seq'::regclass);


--
-- TOC entry 3543 (class 2604 OID 25028)
-- Name: agendas_publicadas agenda_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.agendas_publicadas ALTER COLUMN agenda_id SET DEFAULT nextval('public.agendas_publicadas_agenda_id_seq'::regclass);


--
-- TOC entry 3527 (class 2604 OID 24903)
-- Name: asignaciones_evaluacion asignacion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asignaciones_evaluacion ALTER COLUMN asignacion_id SET DEFAULT nextval('public.asignaciones_evaluacion_asignacion_id_seq'::regclass);


--
-- TOC entry 3552 (class 2604 OID 25085)
-- Name: asistencias asistencia_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asistencias ALTER COLUMN asistencia_id SET DEFAULT nextval('public.asistencias_asistencia_id_seq'::regclass);


--
-- TOC entry 3498 (class 2604 OID 24679)
-- Name: auditoria auditoria_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.auditoria ALTER COLUMN auditoria_id SET DEFAULT nextval('public.auditoria_auditoria_id_seq'::regclass);


--
-- TOC entry 3560 (class 2604 OID 25140)
-- Name: certificados certificado_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.certificados ALTER COLUMN certificado_id SET DEFAULT nextval('public.certificados_certificado_id_seq'::regclass);


--
-- TOC entry 3549 (class 2604 OID 25067)
-- Name: codigos_qr qr_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.codigos_qr ALTER COLUMN qr_id SET DEFAULT nextval('public.codigos_qr_qr_id_seq'::regclass);


--
-- TOC entry 3502 (class 2604 OID 24711)
-- Name: comite_organizador comite_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comite_organizador ALTER COLUMN comite_id SET DEFAULT nextval('public.comite_organizador_comite_id_seq'::regclass);


--
-- TOC entry 3526 (class 2604 OID 24893)
-- Name: comites_evaluadores comite_evaluador_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comites_evaluadores ALTER COLUMN comite_evaluador_id SET DEFAULT nextval('public.comites_evaluadores_comite_evaluador_id_seq'::regclass);


--
-- TOC entry 3513 (class 2604 OID 24796)
-- Name: convocatorias convocatoria_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.convocatorias ALTER COLUMN convocatoria_id SET DEFAULT nextval('public.convocatorias_convocatoria_id_seq'::regclass);


--
-- TOC entry 3525 (class 2604 OID 24881)
-- Name: criterios_rubrica criterio_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.criterios_rubrica ALTER COLUMN criterio_id SET DEFAULT nextval('public.criterios_rubrica_criterio_id_seq'::regclass);


--
-- TOC entry 3539 (class 2604 OID 24993)
-- Name: disponibilidad_ponentes disponibilidad_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_ponentes ALTER COLUMN disponibilidad_id SET DEFAULT nextval('public.disponibilidad_ponentes_disponibilidad_id_seq'::regclass);


--
-- TOC entry 3538 (class 2604 OID 24980)
-- Name: disponibilidad_salas disponibilidad_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_salas ALTER COLUMN disponibilidad_id SET DEFAULT nextval('public.disponibilidad_salas_disponibilidad_id_seq'::regclass);


--
-- TOC entry 3554 (class 2604 OID 25098)
-- Name: encuestas encuesta_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.encuestas ALTER COLUMN encuesta_id SET DEFAULT nextval('public.encuestas_encuesta_id_seq'::regclass);


--
-- TOC entry 3530 (class 2604 OID 24918)
-- Name: evaluaciones evaluacion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.evaluaciones ALTER COLUMN evaluacion_id SET DEFAULT nextval('public.evaluaciones_evaluacion_id_seq'::regclass);


--
-- TOC entry 3500 (class 2604 OID 24693)
-- Name: eventos evento_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.eventos ALTER COLUMN evento_id SET DEFAULT nextval('public.eventos_evento_id_seq'::regclass);


--
-- TOC entry 3512 (class 2604 OID 24781)
-- Name: gastos_ejecutados gasto_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.gastos_ejecutados ALTER COLUMN gasto_id SET DEFAULT nextval('public.gastos_ejecutados_gasto_id_seq'::regclass);


--
-- TOC entry 3562 (class 2604 OID 25155)
-- Name: indicadores indicador_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.indicadores ALTER COLUMN indicador_id SET DEFAULT nextval('public.indicadores_indicador_id_seq'::regclass);


--
-- TOC entry 3547 (class 2604 OID 25055)
-- Name: inscripciones_actividad inscripcion_actividad_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_actividad ALTER COLUMN inscripcion_actividad_id SET DEFAULT nextval('public.inscripciones_actividad_inscripcion_actividad_id_seq'::regclass);


--
-- TOC entry 3545 (class 2604 OID 25041)
-- Name: inscripciones_evento inscripcion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_evento ALTER COLUMN inscripcion_id SET DEFAULT nextval('public.inscripciones_evento_inscripcion_id_seq'::regclass);


--
-- TOC entry 3506 (class 2604 OID 24736)
-- Name: lineas_tematicas linea_tematica_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.lineas_tematicas ALTER COLUMN linea_tematica_id SET DEFAULT nextval('public.lineas_tematicas_linea_tematica_id_seq'::regclass);


--
-- TOC entry 3564 (class 2604 OID 25168)
-- Name: memorias_historicas memoria_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.memorias_historicas ALTER COLUMN memoria_id SET DEFAULT nextval('public.memorias_historicas_memoria_id_seq'::regclass);


--
-- TOC entry 3534 (class 2604 OID 24949)
-- Name: observaciones_propuesta observacion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.observaciones_propuesta ALTER COLUMN observacion_id SET DEFAULT nextval('public.observaciones_propuesta_observacion_id_seq'::regclass);


--
-- TOC entry 3515 (class 2604 OID 24814)
-- Name: participaciones_conferencista participacion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.participaciones_conferencista ALTER COLUMN participacion_id SET DEFAULT nextval('public.participaciones_conferencista_participacion_id_seq'::regclass);


--
-- TOC entry 3493 (class 2604 OID 24632)
-- Name: permisos permiso_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.permisos ALTER COLUMN permiso_id SET DEFAULT nextval('public.permisos_permiso_id_seq'::regclass);


--
-- TOC entry 3486 (class 2604 OID 40960)
-- Name: personas persona_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.personas ALTER COLUMN persona_id SET DEFAULT nextval('public.personas_persona_id_seq'::regclass);


--
-- TOC entry 3556 (class 2604 OID 25110)
-- Name: preguntas_encuesta pregunta_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.preguntas_encuesta ALTER COLUMN pregunta_id SET DEFAULT nextval('public.preguntas_encuesta_pregunta_id_seq'::regclass);


--
-- TOC entry 3509 (class 2604 OID 24762)
-- Name: presupuestos_aprobados aprobacion_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.presupuestos_aprobados ALTER COLUMN aprobacion_id SET DEFAULT nextval('public.presupuestos_aprobados_aprobacion_id_seq'::regclass);


--
-- TOC entry 3517 (class 2604 OID 24828)
-- Name: propuestas propuesta_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas ALTER COLUMN propuesta_id SET DEFAULT nextval('public.propuestas_propuesta_id_seq'::regclass);


--
-- TOC entry 3558 (class 2604 OID 25126)
-- Name: respuestas_encuesta respuesta_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.respuestas_encuesta ALTER COLUMN respuesta_id SET DEFAULT nextval('public.respuestas_encuesta_respuesta_id_seq'::regclass);


--
-- TOC entry 3532 (class 2604 OID 24933)
-- Name: resultados_evaluacion resultado_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.resultados_evaluacion ALTER COLUMN resultado_id SET DEFAULT nextval('public.resultados_evaluacion_resultado_id_seq'::regclass);


--
-- TOC entry 3492 (class 2604 OID 41022)
-- Name: roles rol_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles ALTER COLUMN rol_id SET DEFAULT nextval('public.roles_rol_id_seq'::regclass);


--
-- TOC entry 3523 (class 2604 OID 24869)
-- Name: rubricas_evaluacion rubrica_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubricas_evaluacion ALTER COLUMN rubrica_id SET DEFAULT nextval('public.rubricas_evaluacion_rubrica_id_seq'::regclass);


--
-- TOC entry 3507 (class 2604 OID 24746)
-- Name: rubros_presupuestales rubro_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubros_presupuestales ALTER COLUMN rubro_id SET DEFAULT nextval('public.rubros_presupuestales_rubro_id_seq'::regclass);


--
-- TOC entry 3537 (class 2604 OID 24966)
-- Name: salas sala_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.salas ALTER COLUMN sala_id SET DEFAULT nextval('public.salas_sala_id_seq'::regclass);


--
-- TOC entry 3505 (class 2604 OID 24726)
-- Name: tipos_actividad tipo_actividad_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tipos_actividad ALTER COLUMN tipo_actividad_id SET DEFAULT nextval('public.tipos_actividad_tipo_actividad_id_seq'::regclass);


--
-- TOC entry 3495 (class 2604 OID 41041)
-- Name: tokens_recuperacion token_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tokens_recuperacion ALTER COLUMN token_id SET DEFAULT nextval('public.tokens_recuperacion_token_id_seq'::regclass);


--
-- TOC entry 3487 (class 2604 OID 41061)
-- Name: usuarios usuario_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN usuario_id SET DEFAULT nextval('public.usuarios_usuario_id_seq'::regclass);


--
-- TOC entry 3521 (class 2604 OID 24854)
-- Name: versiones_propuesta version_id; Type: DEFAULT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.versiones_propuesta ALTER COLUMN version_id SET DEFAULT nextval('public.versiones_propuesta_version_id_seq'::regclass);


--
-- TOC entry 3995 (class 0 OID 25004)
-- Dependencies: 277
-- Data for Name: actividades; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.actividades (actividad_id, evento_id, propuesta_id, tipo_actividad_id, linea_tematica_id, nombre, fecha, hora_inicio, hora_fin, sala_id, ponente_id, modalidad, permite_simultaneidad, estado) FROM stdin;
\.


--
-- TOC entry 4021 (class 0 OID 49153)
-- Dependencies: 303
-- Data for Name: afiliaciones; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.afiliaciones (id, nombre_afiliacion) FROM stdin;
1	Estudiante UFPS
2	Particular
3	Estudiante Internacional
4	Estudiante Externo UFPS
\.


--
-- TOC entry 3997 (class 0 OID 25025)
-- Dependencies: 279
-- Data for Name: agendas_publicadas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.agendas_publicadas (agenda_id, evento_id, fecha_dia, fecha_publicacion, estado) FROM stdin;
\.


--
-- TOC entry 3981 (class 0 OID 24900)
-- Dependencies: 263
-- Data for Name: asignaciones_evaluacion; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.asignaciones_evaluacion (asignacion_id, propuesta_id, comite_evaluador_id, fecha_asignacion, estado) FROM stdin;
\.


--
-- TOC entry 4005 (class 0 OID 25082)
-- Dependencies: 287
-- Data for Name: asistencias; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.asistencias (asistencia_id, inscripcion_id, actividad_id, fecha_hora_registro, metodo_registro) FROM stdin;
\.


--
-- TOC entry 3950 (class 0 OID 24676)
-- Dependencies: 232
-- Data for Name: auditoria; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.auditoria (auditoria_id, usuario_id, accion, entidad, entidad_id, fecha_hora, detalle) FROM stdin;
\.


--
-- TOC entry 4013 (class 0 OID 25137)
-- Dependencies: 295
-- Data for Name: certificados; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.certificados (certificado_id, persona_id, evento_id, actividad_id, tipo_certificado, porcentaje_asistencia, fecha_generacion, archivo_url) FROM stdin;
\.


--
-- TOC entry 4003 (class 0 OID 25064)
-- Dependencies: 285
-- Data for Name: codigos_qr; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.codigos_qr (qr_id, inscripcion_id, codigo, fecha_generacion, vigente) FROM stdin;
\.


--
-- TOC entry 3954 (class 0 OID 24708)
-- Dependencies: 236
-- Data for Name: comite_organizador; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.comite_organizador (comite_id, evento_id, persona_id, rol_comite, fecha_asignacion, activo) FROM stdin;
\.


--
-- TOC entry 3979 (class 0 OID 24890)
-- Dependencies: 261
-- Data for Name: comites_evaluadores; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.comites_evaluadores (comite_evaluador_id, convocatoria_id, persona_id, area_experticia) FROM stdin;
\.


--
-- TOC entry 3966 (class 0 OID 24793)
-- Dependencies: 248
-- Data for Name: convocatorias; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.convocatorias (convocatoria_id, evento_id, titulo, descripcion, requisitos, fecha_apertura, fecha_cierre, estado) FROM stdin;
\.


--
-- TOC entry 3977 (class 0 OID 24878)
-- Dependencies: 259
-- Data for Name: criterios_rubrica; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.criterios_rubrica (criterio_id, rubrica_id, nombre, peso_porcentual) FROM stdin;
\.


--
-- TOC entry 4018 (class 0 OID 25519)
-- Dependencies: 300
-- Data for Name: databasechangelog; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) FROM stdin;
001-eliminar-nombre-usuario	DF	db/changelog/changes/1-afil-usrs.sql	2026-09-21 15:03:31.87507	1	EXECUTED	9:a92458d44c27d5b6b0e99acae7bd02e3	sql		\N	5.0.3	\N	\N	0003006524
002-crear-tabla-dom-afiliaciones	DF	db/changelog/changes/1-afil-usrs.sql	2026-09-21 15:03:32.530774	2	EXECUTED	9:8ea1600071acbcda618f35d93d5bc056	sql		\N	5.0.3	\N	\N	0003006524
003-insertar-opciones-combobox	DF	db/changelog/changes/1-afil-usrs.sql	2026-09-21 15:03:33.051189	3	EXECUTED	9:88c366815c378bf05b41fe3d47399775	sql		\N	5.0.3	\N	\N	0003006524
004-vincular-afiliacion-con-persona	DF	db/changelog/changes/1-afil-usrs.sql	2026-09-21 15:03:33.699017	4	EXECUTED	9:4ee3591863a80625db339fd29a6e4ba2	sql		\N	5.0.3	\N	\N	0003006524
002-eliminar-afiliacion-institucional	DF	db/changelog/changes/2-eliminar-afiliacion.sql	2026-09-21 15:36:14.181084	5	EXECUTED	9:f812ed675e83bdb89aab15c2778ff4c7	sql		\N	5.0.3	\N	\N	0004969979
001-agregar-bloqueo-login	HU01	db/changelog/changes/3-bloqueo-login.sql	2026-09-27 16:17:52.299475	6	EXECUTED	9:594a30cca4a6e9c73b2b38c6ce6261bd	sql		\N	5.0.3	\N	\N	0525863888
\.


--
-- TOC entry 4019 (class 0 OID 25530)
-- Dependencies: 301
-- Data for Name: databasechangeloglock; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.databasechangeloglock (id, locked, lockgranted, lockedby) FROM stdin;
1	f	\N	\N
\.


--
-- TOC entry 3993 (class 0 OID 24990)
-- Dependencies: 275
-- Data for Name: disponibilidad_ponentes; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.disponibilidad_ponentes (disponibilidad_id, persona_id, evento_id, fecha, hora_inicio, hora_fin) FROM stdin;
\.


--
-- TOC entry 3991 (class 0 OID 24977)
-- Dependencies: 273
-- Data for Name: disponibilidad_salas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.disponibilidad_salas (disponibilidad_id, sala_id, fecha, hora_inicio, hora_fin) FROM stdin;
\.


--
-- TOC entry 4007 (class 0 OID 25095)
-- Dependencies: 289
-- Data for Name: encuestas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.encuestas (encuesta_id, evento_id, actividad_id, titulo, estado) FROM stdin;
\.


--
-- TOC entry 3983 (class 0 OID 24915)
-- Dependencies: 265
-- Data for Name: evaluaciones; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.evaluaciones (evaluacion_id, asignacion_id, criterio_id, calificacion, observaciones, fecha_evaluacion) FROM stdin;
\.


--
-- TOC entry 3952 (class 0 OID 24690)
-- Dependencies: 234
-- Data for Name: eventos; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.eventos (evento_id, nombre, objetivo, descripcion, tipo, modalidad, fecha_inicio, fecha_fin, semestre, estado, evento_base_id) FROM stdin;
\.


--
-- TOC entry 3964 (class 0 OID 24778)
-- Dependencies: 246
-- Data for Name: gastos_ejecutados; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.gastos_ejecutados (gasto_id, rubro_id, descripcion, valor, fecha_gasto, soporte_url, registrado_por) FROM stdin;
\.


--
-- TOC entry 4015 (class 0 OID 25152)
-- Dependencies: 297
-- Data for Name: indicadores; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.indicadores (indicador_id, evento_id, nombre, valor, unidad, fecha_calculo) FROM stdin;
\.


--
-- TOC entry 4001 (class 0 OID 25052)
-- Dependencies: 283
-- Data for Name: inscripciones_actividad; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.inscripciones_actividad (inscripcion_actividad_id, inscripcion_id, actividad_id, fecha_inscripcion) FROM stdin;
\.


--
-- TOC entry 3999 (class 0 OID 25038)
-- Dependencies: 281
-- Data for Name: inscripciones_evento; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.inscripciones_evento (inscripcion_id, persona_id, evento_id, categoria_participante, fecha_inscripcion) FROM stdin;
\.


--
-- TOC entry 3958 (class 0 OID 24733)
-- Dependencies: 240
-- Data for Name: lineas_tematicas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.lineas_tematicas (linea_tematica_id, evento_id, nombre, descripcion) FROM stdin;
\.


--
-- TOC entry 4017 (class 0 OID 25165)
-- Dependencies: 299
-- Data for Name: memorias_historicas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.memorias_historicas (memoria_id, evento_id, fecha_consolidacion, archivo_url, resumen) FROM stdin;
\.


--
-- TOC entry 3987 (class 0 OID 24946)
-- Dependencies: 269
-- Data for Name: observaciones_propuesta; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.observaciones_propuesta (observacion_id, version_id, comite_evaluador_id, descripcion, fecha, resuelto) FROM stdin;
\.


--
-- TOC entry 3968 (class 0 OID 24811)
-- Dependencies: 250
-- Data for Name: participaciones_conferencista; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.participaciones_conferencista (participacion_id, persona_id, evento_id, tipo_participacion, tema, fecha_registro) FROM stdin;
\.


--
-- TOC entry 3944 (class 0 OID 24629)
-- Dependencies: 226
-- Data for Name: permisos; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.permisos (permiso_id, codigo, modulo, descripcion) FROM stdin;
\.


--
-- TOC entry 3938 (class 0 OID 24577)
-- Dependencies: 220
-- Data for Name: personas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.personas (persona_id, tipo_documento, numero_documento, nombres, apellidos, correo, telefono, afiliacion_id) FROM stdin;
1	CC	1098765432	Carlos Andrés	Gómez Pérez	carlos.gomez@universidad.edu.co	+573001234567	\N
2	CC	1111111111	Juan David	Diaz Diaz	david@universidad.edu.co	+3000000000	\N
3	CC	10	claudio	puelles	cp@gmail.com	123	1
4	CC	11	arman	sadico	arman@gmail.com	11	2
5	CC	1	prueba	1	p@gmail.com	10	3
6	CC	1112151461	Carlos Daniel	Gomez	correo@correo.com	3001106934	\N
7	CC	1127045208	scharid	Maldonado	schavalemg@gmail.com	3213237822	1
\.


--
-- TOC entry 4009 (class 0 OID 25107)
-- Dependencies: 291
-- Data for Name: preguntas_encuesta; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.preguntas_encuesta (pregunta_id, encuesta_id, texto, tipo_respuesta, orden) FROM stdin;
\.


--
-- TOC entry 3962 (class 0 OID 24759)
-- Dependencies: 244
-- Data for Name: presupuestos_aprobados; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.presupuestos_aprobados (aprobacion_id, evento_id, valor_total_aprobado, evidencia_url, fecha_aprobacion, aprobado_por, estado) FROM stdin;
\.


--
-- TOC entry 3970 (class 0 OID 24825)
-- Dependencies: 252
-- Data for Name: propuestas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.propuestas (propuesta_id, convocatoria_id, titulo, resumen, palabras_clave, linea_tematica_id, estado, fecha_envio) FROM stdin;
\.


--
-- TOC entry 3971 (class 0 OID 24841)
-- Dependencies: 253
-- Data for Name: propuestas_autores; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.propuestas_autores (propuesta_id, persona_id, orden) FROM stdin;
\.


--
-- TOC entry 4011 (class 0 OID 25123)
-- Dependencies: 293
-- Data for Name: respuestas_encuesta; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.respuestas_encuesta (respuesta_id, pregunta_id, persona_id, respuesta, fecha_respuesta) FROM stdin;
\.


--
-- TOC entry 3985 (class 0 OID 24930)
-- Dependencies: 267
-- Data for Name: resultados_evaluacion; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.resultados_evaluacion (resultado_id, propuesta_id, puntuacion_ponderada, clasificacion, fecha_calculo) FROM stdin;
\.


--
-- TOC entry 3942 (class 0 OID 24618)
-- Dependencies: 224
-- Data for Name: roles; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.roles (rol_id, nombre, descripcion) FROM stdin;
1	PARTICIPANTE	Rol inicial para participantes y asistentes registrados
\.


--
-- TOC entry 3945 (class 0 OID 24640)
-- Dependencies: 227
-- Data for Name: roles_permisos; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.roles_permisos (rol_id, permiso_id) FROM stdin;
\.


--
-- TOC entry 3975 (class 0 OID 24866)
-- Dependencies: 257
-- Data for Name: rubricas_evaluacion; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.rubricas_evaluacion (rubrica_id, convocatoria_id, nombre, activo) FROM stdin;
\.


--
-- TOC entry 3960 (class 0 OID 24743)
-- Dependencies: 242
-- Data for Name: rubros_presupuestales; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.rubros_presupuestales (rubro_id, evento_id, nombre, cantidad, valor_unitario_proyectado, activo) FROM stdin;
\.


--
-- TOC entry 3989 (class 0 OID 24963)
-- Dependencies: 271
-- Data for Name: salas; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.salas (sala_id, evento_id, nombre, ubicacion, tipo, aforo_maximo) FROM stdin;
\.


--
-- TOC entry 3956 (class 0 OID 24723)
-- Dependencies: 238
-- Data for Name: tipos_actividad; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.tipos_actividad (tipo_actividad_id, evento_id, nombre, descripcion) FROM stdin;
\.


--
-- TOC entry 3948 (class 0 OID 24657)
-- Dependencies: 230
-- Data for Name: tokens_recuperacion; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.tokens_recuperacion (token_id, usuario_id, token, tipo, fecha_generacion, fecha_expiracion, usado) FROM stdin;
1	1	48b30657-1ba9-4c4b-9b8f-424d143a4589	verificacion	2026-09-17 17:23:00.098049	2026-09-18 17:23:00.098077	f
2	2	5a67ab09-08ac-4c3f-8375-00a427a07a02	verificacion	2026-09-19 23:35:36.441416	2026-09-20 23:35:36.44143	t
3	2	fdf753db-180e-4d8f-be71-b5353bf24e51	verificacion	2026-09-19 23:37:50.214786	2026-09-20 23:37:50.214792	t
5	4	12371649-e7e9-4933-ade1-95bc092bbe8b	verificacion	2026-09-21 12:55:16.553404	2026-09-22 12:55:16.553404	f
6	5	e11de1f1-e29c-4fa9-b24f-aa93a0b8a02f	verificacion	2026-09-21 13:01:38.116729	2026-09-22 13:01:38.116729	f
4	3	bd15fdc5-a5af-41f8-9d4c-fd66d74ed837	verificacion	2026-09-21 12:48:40.091841	2026-09-22 12:48:40.091841	t
7	6	5eaa812e-d2f7-4745-985e-c2571165ab3a	verificacion	2026-09-27 11:05:04.044448	2026-09-28 11:05:04.044463	f
9	7	81ecb1a8-b354-4202-b2c6-92ebcabd14f3	verificacion	2026-09-27 12:56:08.589975	2026-09-28 12:56:08.589975	f
8	7	d7dc8459-4cc8-4341-b6b7-ddf48028d05c	verificacion	2026-09-27 12:53:17.201533	2026-09-28 12:53:17.201533	t
\.


--
-- TOC entry 3940 (class 0 OID 24596)
-- Dependencies: 222
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.usuarios (usuario_id, persona_id, contrasena_hash, correo_verificado, estado, fecha_registro, intentos_fallidos, bloqueado_hasta) FROM stdin;
1	1	$2a$10$Jn9hIEFgn3dCzsVwxF363OajHOgw6LvCy2ZWheDTSnnA0WZ2frN5S	f	activo	2026-09-17 17:23:00.00377	0	\N
2	2	$2a$10$PyuKbOB68xQK67gRXiB7XeK11xsIoEIg4RaH1qkqmSiHByp9QgpAa	t	activo	2026-09-19 23:35:36.346451	0	\N
4	4	$2a$10$1dFMKgwloAW2bRc7jRoghOPXiaYfxpyQDVRL9BvpzIwBORuoFg/3e	f	activo	2026-09-21 12:55:16.454535	0	\N
5	5	$2a$10$hYbIGgTDICE1Qju2FWaYz.fk3aSt5oTVy63YdxWUTSbp4FKnYpfeq	f	activo	2026-09-21 13:01:38.014717	0	\N
3	3	$2a$10$lVc..k.50QpoBiifHQaq0utIJBZJc38kezGy.9koBZWWsM71fP8tq	t	activo	2026-09-21 12:48:39.993627	0	\N
6	6	$2a$10$E9j9YkoUdeijk0fTbdoqe.MTN7jtzIvMHaeuwEFBoAdny3Qxpo4W6	f	activo	2026-09-27 11:05:03.945482	0	\N
7	7	$2a$10$es0WsfKqQAjMj4cI6GRSYOtHVKKP8ana232/3esta9/dfkOsipuua	f	activo	2026-09-27 12:53:17.021284	0	\N
\.


--
-- TOC entry 3946 (class 0 OID 24647)
-- Dependencies: 228
-- Data for Name: usuarios_roles; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.usuarios_roles (usuario_id, rol_id, fecha_asignacion) FROM stdin;
1	1	2026-09-17 22:22:59.421323
2	1	2026-09-20 04:35:35.85498
3	1	2026-09-21 17:48:57.655769
4	1	2026-09-21 17:55:34.158712
5	1	2026-09-21 18:01:55.665946
6	1	2026-09-27 16:05:03.52594
7	1	2026-09-27 17:53:15.147776
\.


--
-- TOC entry 3973 (class 0 OID 24851)
-- Dependencies: 255
-- Data for Name: versiones_propuesta; Type: TABLE DATA; Schema: public; Owner: neondb_owner
--

COPY public.versiones_propuesta (version_id, propuesta_id, numero_version, documento_url, observaciones, fecha_creacion) FROM stdin;
\.


--
-- TOC entry 4067 (class 0 OID 0)
-- Dependencies: 276
-- Name: actividades_actividad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.actividades_actividad_id_seq', 1, false);


--
-- TOC entry 4068 (class 0 OID 0)
-- Dependencies: 302
-- Name: afiliaciones_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.afiliaciones_id_seq', 4, true);


--
-- TOC entry 4069 (class 0 OID 0)
-- Dependencies: 278
-- Name: agendas_publicadas_agenda_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.agendas_publicadas_agenda_id_seq', 1, false);


--
-- TOC entry 4070 (class 0 OID 0)
-- Dependencies: 262
-- Name: asignaciones_evaluacion_asignacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.asignaciones_evaluacion_asignacion_id_seq', 1, false);


--
-- TOC entry 4071 (class 0 OID 0)
-- Dependencies: 286
-- Name: asistencias_asistencia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.asistencias_asistencia_id_seq', 1, false);


--
-- TOC entry 4072 (class 0 OID 0)
-- Dependencies: 231
-- Name: auditoria_auditoria_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.auditoria_auditoria_id_seq', 1, false);


--
-- TOC entry 4073 (class 0 OID 0)
-- Dependencies: 294
-- Name: certificados_certificado_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.certificados_certificado_id_seq', 1, false);


--
-- TOC entry 4074 (class 0 OID 0)
-- Dependencies: 284
-- Name: codigos_qr_qr_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.codigos_qr_qr_id_seq', 1, false);


--
-- TOC entry 4075 (class 0 OID 0)
-- Dependencies: 235
-- Name: comite_organizador_comite_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.comite_organizador_comite_id_seq', 1, false);


--
-- TOC entry 4076 (class 0 OID 0)
-- Dependencies: 260
-- Name: comites_evaluadores_comite_evaluador_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.comites_evaluadores_comite_evaluador_id_seq', 1, false);


--
-- TOC entry 4077 (class 0 OID 0)
-- Dependencies: 247
-- Name: convocatorias_convocatoria_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.convocatorias_convocatoria_id_seq', 1, false);


--
-- TOC entry 4078 (class 0 OID 0)
-- Dependencies: 258
-- Name: criterios_rubrica_criterio_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.criterios_rubrica_criterio_id_seq', 1, false);


--
-- TOC entry 4079 (class 0 OID 0)
-- Dependencies: 274
-- Name: disponibilidad_ponentes_disponibilidad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.disponibilidad_ponentes_disponibilidad_id_seq', 1, false);


--
-- TOC entry 4080 (class 0 OID 0)
-- Dependencies: 272
-- Name: disponibilidad_salas_disponibilidad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.disponibilidad_salas_disponibilidad_id_seq', 1, false);


--
-- TOC entry 4081 (class 0 OID 0)
-- Dependencies: 288
-- Name: encuestas_encuesta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.encuestas_encuesta_id_seq', 1, false);


--
-- TOC entry 4082 (class 0 OID 0)
-- Dependencies: 264
-- Name: evaluaciones_evaluacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.evaluaciones_evaluacion_id_seq', 1, false);


--
-- TOC entry 4083 (class 0 OID 0)
-- Dependencies: 233
-- Name: eventos_evento_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.eventos_evento_id_seq', 1, false);


--
-- TOC entry 4084 (class 0 OID 0)
-- Dependencies: 245
-- Name: gastos_ejecutados_gasto_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.gastos_ejecutados_gasto_id_seq', 1, false);


--
-- TOC entry 4085 (class 0 OID 0)
-- Dependencies: 296
-- Name: indicadores_indicador_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.indicadores_indicador_id_seq', 1, false);


--
-- TOC entry 4086 (class 0 OID 0)
-- Dependencies: 282
-- Name: inscripciones_actividad_inscripcion_actividad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.inscripciones_actividad_inscripcion_actividad_id_seq', 1, false);


--
-- TOC entry 4087 (class 0 OID 0)
-- Dependencies: 280
-- Name: inscripciones_evento_inscripcion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.inscripciones_evento_inscripcion_id_seq', 1, false);


--
-- TOC entry 4088 (class 0 OID 0)
-- Dependencies: 239
-- Name: lineas_tematicas_linea_tematica_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.lineas_tematicas_linea_tematica_id_seq', 1, false);


--
-- TOC entry 4089 (class 0 OID 0)
-- Dependencies: 298
-- Name: memorias_historicas_memoria_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.memorias_historicas_memoria_id_seq', 1, false);


--
-- TOC entry 4090 (class 0 OID 0)
-- Dependencies: 268
-- Name: observaciones_propuesta_observacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.observaciones_propuesta_observacion_id_seq', 1, false);


--
-- TOC entry 4091 (class 0 OID 0)
-- Dependencies: 249
-- Name: participaciones_conferencista_participacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.participaciones_conferencista_participacion_id_seq', 1, false);


--
-- TOC entry 4092 (class 0 OID 0)
-- Dependencies: 225
-- Name: permisos_permiso_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.permisos_permiso_id_seq', 1, false);


--
-- TOC entry 4093 (class 0 OID 0)
-- Dependencies: 219
-- Name: personas_persona_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.personas_persona_id_seq', 7, true);


--
-- TOC entry 4094 (class 0 OID 0)
-- Dependencies: 290
-- Name: preguntas_encuesta_pregunta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.preguntas_encuesta_pregunta_id_seq', 1, false);


--
-- TOC entry 4095 (class 0 OID 0)
-- Dependencies: 243
-- Name: presupuestos_aprobados_aprobacion_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.presupuestos_aprobados_aprobacion_id_seq', 1, false);


--
-- TOC entry 4096 (class 0 OID 0)
-- Dependencies: 251
-- Name: propuestas_propuesta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.propuestas_propuesta_id_seq', 1, false);


--
-- TOC entry 4097 (class 0 OID 0)
-- Dependencies: 292
-- Name: respuestas_encuesta_respuesta_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.respuestas_encuesta_respuesta_id_seq', 1, false);


--
-- TOC entry 4098 (class 0 OID 0)
-- Dependencies: 266
-- Name: resultados_evaluacion_resultado_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.resultados_evaluacion_resultado_id_seq', 1, false);


--
-- TOC entry 4099 (class 0 OID 0)
-- Dependencies: 223
-- Name: roles_rol_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.roles_rol_id_seq', 1, true);


--
-- TOC entry 4100 (class 0 OID 0)
-- Dependencies: 256
-- Name: rubricas_evaluacion_rubrica_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.rubricas_evaluacion_rubrica_id_seq', 1, false);


--
-- TOC entry 4101 (class 0 OID 0)
-- Dependencies: 241
-- Name: rubros_presupuestales_rubro_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.rubros_presupuestales_rubro_id_seq', 1, false);


--
-- TOC entry 4102 (class 0 OID 0)
-- Dependencies: 270
-- Name: salas_sala_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.salas_sala_id_seq', 1, false);


--
-- TOC entry 4103 (class 0 OID 0)
-- Dependencies: 237
-- Name: tipos_actividad_tipo_actividad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.tipos_actividad_tipo_actividad_id_seq', 1, false);


--
-- TOC entry 4104 (class 0 OID 0)
-- Dependencies: 229
-- Name: tokens_recuperacion_token_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.tokens_recuperacion_token_id_seq', 9, true);


--
-- TOC entry 4105 (class 0 OID 0)
-- Dependencies: 221
-- Name: usuarios_usuario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.usuarios_usuario_id_seq', 7, true);


--
-- TOC entry 4106 (class 0 OID 0)
-- Dependencies: 254
-- Name: versiones_propuesta_version_id_seq; Type: SEQUENCE SET; Schema: public; Owner: neondb_owner
--

SELECT pg_catalog.setval('public.versiones_propuesta_version_id_seq', 1, false);


--
-- TOC entry 3683 (class 2606 OID 25023)
-- Name: actividades actividades_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_pkey PRIMARY KEY (actividad_id);


--
-- TOC entry 3722 (class 2606 OID 49162)
-- Name: afiliaciones afiliaciones_nombre_afiliacion_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.afiliaciones
    ADD CONSTRAINT afiliaciones_nombre_afiliacion_key UNIQUE (nombre_afiliacion);


--
-- TOC entry 3724 (class 2606 OID 49160)
-- Name: afiliaciones afiliaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.afiliaciones
    ADD CONSTRAINT afiliaciones_pkey PRIMARY KEY (id);


--
-- TOC entry 3687 (class 2606 OID 25036)
-- Name: agendas_publicadas agendas_publicadas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.agendas_publicadas
    ADD CONSTRAINT agendas_publicadas_pkey PRIMARY KEY (agenda_id);


--
-- TOC entry 3664 (class 2606 OID 24913)
-- Name: asignaciones_evaluacion asignaciones_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asignaciones_evaluacion
    ADD CONSTRAINT asignaciones_evaluacion_pkey PRIMARY KEY (asignacion_id);


--
-- TOC entry 3703 (class 2606 OID 25093)
-- Name: asistencias asistencias_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT asistencias_pkey PRIMARY KEY (asistencia_id);


--
-- TOC entry 3621 (class 2606 OID 24688)
-- Name: auditoria auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_pkey PRIMARY KEY (auditoria_id);


--
-- TOC entry 3712 (class 2606 OID 25150)
-- Name: certificados certificados_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.certificados
    ADD CONSTRAINT certificados_pkey PRIMARY KEY (certificado_id);


--
-- TOC entry 3696 (class 2606 OID 25080)
-- Name: codigos_qr codigos_qr_codigo_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.codigos_qr
    ADD CONSTRAINT codigos_qr_codigo_key UNIQUE (codigo);


--
-- TOC entry 3698 (class 2606 OID 25078)
-- Name: codigos_qr codigos_qr_inscripcion_id_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.codigos_qr
    ADD CONSTRAINT codigos_qr_inscripcion_id_key UNIQUE (inscripcion_id);


--
-- TOC entry 3700 (class 2606 OID 25076)
-- Name: codigos_qr codigos_qr_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.codigos_qr
    ADD CONSTRAINT codigos_qr_pkey PRIMARY KEY (qr_id);


--
-- TOC entry 3627 (class 2606 OID 24721)
-- Name: comite_organizador comite_organizador_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comite_organizador
    ADD CONSTRAINT comite_organizador_pkey PRIMARY KEY (comite_id);


--
-- TOC entry 3662 (class 2606 OID 24898)
-- Name: comites_evaluadores comites_evaluadores_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comites_evaluadores
    ADD CONSTRAINT comites_evaluadores_pkey PRIMARY KEY (comite_evaluador_id);


--
-- TOC entry 3644 (class 2606 OID 24809)
-- Name: convocatorias convocatorias_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.convocatorias
    ADD CONSTRAINT convocatorias_pkey PRIMARY KEY (convocatoria_id);


--
-- TOC entry 3659 (class 2606 OID 24888)
-- Name: criterios_rubrica criterios_rubrica_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.criterios_rubrica
    ADD CONSTRAINT criterios_rubrica_pkey PRIMARY KEY (criterio_id);


--
-- TOC entry 3720 (class 2606 OID 25536)
-- Name: databasechangeloglock databasechangeloglock_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.databasechangeloglock
    ADD CONSTRAINT databasechangeloglock_pkey PRIMARY KEY (id);


--
-- TOC entry 3681 (class 2606 OID 25002)
-- Name: disponibilidad_ponentes disponibilidad_ponentes_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_ponentes
    ADD CONSTRAINT disponibilidad_ponentes_pkey PRIMARY KEY (disponibilidad_id);


--
-- TOC entry 3679 (class 2606 OID 24988)
-- Name: disponibilidad_salas disponibilidad_salas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_salas
    ADD CONSTRAINT disponibilidad_salas_pkey PRIMARY KEY (disponibilidad_id);


--
-- TOC entry 3705 (class 2606 OID 25105)
-- Name: encuestas encuestas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.encuestas
    ADD CONSTRAINT encuestas_pkey PRIMARY KEY (encuesta_id);


--
-- TOC entry 3668 (class 2606 OID 24928)
-- Name: evaluaciones evaluaciones_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT evaluaciones_pkey PRIMARY KEY (evaluacion_id);


--
-- TOC entry 3623 (class 2606 OID 24706)
-- Name: eventos eventos_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.eventos
    ADD CONSTRAINT eventos_pkey PRIMARY KEY (evento_id);


--
-- TOC entry 3642 (class 2606 OID 24791)
-- Name: gastos_ejecutados gastos_ejecutados_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.gastos_ejecutados
    ADD CONSTRAINT gastos_ejecutados_pkey PRIMARY KEY (gasto_id);


--
-- TOC entry 3714 (class 2606 OID 25163)
-- Name: indicadores indicadores_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.indicadores
    ADD CONSTRAINT indicadores_pkey PRIMARY KEY (indicador_id);


--
-- TOC entry 3694 (class 2606 OID 25062)
-- Name: inscripciones_actividad inscripciones_actividad_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_actividad
    ADD CONSTRAINT inscripciones_actividad_pkey PRIMARY KEY (inscripcion_actividad_id);


--
-- TOC entry 3691 (class 2606 OID 25050)
-- Name: inscripciones_evento inscripciones_evento_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_evento
    ADD CONSTRAINT inscripciones_evento_pkey PRIMARY KEY (inscripcion_id);


--
-- TOC entry 3633 (class 2606 OID 24741)
-- Name: lineas_tematicas lineas_tematicas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.lineas_tematicas
    ADD CONSTRAINT lineas_tematicas_pkey PRIMARY KEY (linea_tematica_id);


--
-- TOC entry 3716 (class 2606 OID 25178)
-- Name: memorias_historicas memorias_historicas_evento_id_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.memorias_historicas
    ADD CONSTRAINT memorias_historicas_evento_id_key UNIQUE (evento_id);


--
-- TOC entry 3718 (class 2606 OID 25176)
-- Name: memorias_historicas memorias_historicas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.memorias_historicas
    ADD CONSTRAINT memorias_historicas_pkey PRIMARY KEY (memoria_id);


--
-- TOC entry 3674 (class 2606 OID 24961)
-- Name: observaciones_propuesta observaciones_propuesta_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.observaciones_propuesta
    ADD CONSTRAINT observaciones_propuesta_pkey PRIMARY KEY (observacion_id);


--
-- TOC entry 3648 (class 2606 OID 24823)
-- Name: participaciones_conferencista participaciones_conferencista_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.participaciones_conferencista
    ADD CONSTRAINT participaciones_conferencista_pkey PRIMARY KEY (participacion_id);


--
-- TOC entry 3609 (class 2606 OID 24639)
-- Name: permisos permisos_codigo_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.permisos
    ADD CONSTRAINT permisos_codigo_key UNIQUE (codigo);


--
-- TOC entry 3611 (class 2606 OID 24637)
-- Name: permisos permisos_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.permisos
    ADD CONSTRAINT permisos_pkey PRIMARY KEY (permiso_id);


--
-- TOC entry 3595 (class 2606 OID 24594)
-- Name: personas personas_correo_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_correo_key UNIQUE (correo);


--
-- TOC entry 3597 (class 2606 OID 24592)
-- Name: personas personas_numero_documento_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_numero_documento_key UNIQUE (numero_documento);


--
-- TOC entry 3599 (class 2606 OID 40962)
-- Name: personas personas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT personas_pkey PRIMARY KEY (persona_id);


--
-- TOC entry 3707 (class 2606 OID 25121)
-- Name: preguntas_encuesta preguntas_encuesta_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.preguntas_encuesta
    ADD CONSTRAINT preguntas_encuesta_pkey PRIMARY KEY (pregunta_id);


--
-- TOC entry 3638 (class 2606 OID 24776)
-- Name: presupuestos_aprobados presupuestos_aprobados_evento_id_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.presupuestos_aprobados
    ADD CONSTRAINT presupuestos_aprobados_evento_id_key UNIQUE (evento_id);


--
-- TOC entry 3640 (class 2606 OID 24774)
-- Name: presupuestos_aprobados presupuestos_aprobados_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.presupuestos_aprobados
    ADD CONSTRAINT presupuestos_aprobados_pkey PRIMARY KEY (aprobacion_id);


--
-- TOC entry 3652 (class 2606 OID 24849)
-- Name: propuestas_autores propuestas_autores_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas_autores
    ADD CONSTRAINT propuestas_autores_pkey PRIMARY KEY (propuesta_id, persona_id);


--
-- TOC entry 3650 (class 2606 OID 24840)
-- Name: propuestas propuestas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas
    ADD CONSTRAINT propuestas_pkey PRIMARY KEY (propuesta_id);


--
-- TOC entry 3709 (class 2606 OID 25135)
-- Name: respuestas_encuesta respuestas_encuesta_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.respuestas_encuesta
    ADD CONSTRAINT respuestas_encuesta_pkey PRIMARY KEY (respuesta_id);


--
-- TOC entry 3670 (class 2606 OID 24942)
-- Name: resultados_evaluacion resultados_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.resultados_evaluacion
    ADD CONSTRAINT resultados_evaluacion_pkey PRIMARY KEY (resultado_id);


--
-- TOC entry 3672 (class 2606 OID 24944)
-- Name: resultados_evaluacion resultados_evaluacion_propuesta_id_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.resultados_evaluacion
    ADD CONSTRAINT resultados_evaluacion_propuesta_id_key UNIQUE (propuesta_id);


--
-- TOC entry 3605 (class 2606 OID 24627)
-- Name: roles roles_nombre_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_nombre_key UNIQUE (nombre);


--
-- TOC entry 3613 (class 2606 OID 24646)
-- Name: roles_permisos roles_permisos_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles_permisos
    ADD CONSTRAINT roles_permisos_pkey PRIMARY KEY (rol_id, permiso_id);


--
-- TOC entry 3607 (class 2606 OID 41024)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (rol_id);


--
-- TOC entry 3657 (class 2606 OID 24876)
-- Name: rubricas_evaluacion rubricas_evaluacion_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubricas_evaluacion
    ADD CONSTRAINT rubricas_evaluacion_pkey PRIMARY KEY (rubrica_id);


--
-- TOC entry 3636 (class 2606 OID 24757)
-- Name: rubros_presupuestales rubros_presupuestales_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubros_presupuestales
    ADD CONSTRAINT rubros_presupuestales_pkey PRIMARY KEY (rubro_id);


--
-- TOC entry 3677 (class 2606 OID 24975)
-- Name: salas salas_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.salas
    ADD CONSTRAINT salas_pkey PRIMARY KEY (sala_id);


--
-- TOC entry 3630 (class 2606 OID 24731)
-- Name: tipos_actividad tipos_actividad_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tipos_actividad
    ADD CONSTRAINT tipos_actividad_pkey PRIMARY KEY (tipo_actividad_id);


--
-- TOC entry 3617 (class 2606 OID 41043)
-- Name: tokens_recuperacion tokens_recuperacion_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tokens_recuperacion
    ADD CONSTRAINT tokens_recuperacion_pkey PRIMARY KEY (token_id);


--
-- TOC entry 3619 (class 2606 OID 24674)
-- Name: tokens_recuperacion tokens_recuperacion_token_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tokens_recuperacion
    ADD CONSTRAINT tokens_recuperacion_token_key UNIQUE (token);


--
-- TOC entry 3601 (class 2606 OID 41097)
-- Name: usuarios usuarios_persona_id_key; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_persona_id_key UNIQUE (persona_id);


--
-- TOC entry 3603 (class 2606 OID 41063)
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (usuario_id);


--
-- TOC entry 3615 (class 2606 OID 41123)
-- Name: usuarios_roles usuarios_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios_roles
    ADD CONSTRAINT usuarios_roles_pkey PRIMARY KEY (usuario_id, rol_id);


--
-- TOC entry 3654 (class 2606 OID 24864)
-- Name: versiones_propuesta versiones_propuesta_pkey; Type: CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.versiones_propuesta
    ADD CONSTRAINT versiones_propuesta_pkey PRIMARY KEY (version_id);


--
-- TOC entry 3685 (class 1259 OID 25192)
-- Name: agendas_publicadas_evento_id_fecha_dia_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX agendas_publicadas_evento_id_fecha_dia_idx ON public.agendas_publicadas USING btree (evento_id, fecha_dia);


--
-- TOC entry 3665 (class 1259 OID 25188)
-- Name: asignaciones_evaluacion_propuesta_id_comite_evaluador_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX asignaciones_evaluacion_propuesta_id_comite_evaluador_id_idx ON public.asignaciones_evaluacion USING btree (propuesta_id, comite_evaluador_id);


--
-- TOC entry 3701 (class 1259 OID 25196)
-- Name: asistencias_inscripcion_id_actividad_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX asistencias_inscripcion_id_actividad_id_idx ON public.asistencias USING btree (inscripcion_id, actividad_id);


--
-- TOC entry 3625 (class 1259 OID 25180)
-- Name: comite_organizador_evento_id_persona_id_rol_comite_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX comite_organizador_evento_id_persona_id_rol_comite_idx ON public.comite_organizador USING btree (evento_id, persona_id, rol_comite);


--
-- TOC entry 3660 (class 1259 OID 25187)
-- Name: comites_evaluadores_convocatoria_id_persona_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX comites_evaluadores_convocatoria_id_persona_id_idx ON public.comites_evaluadores USING btree (convocatoria_id, persona_id);


--
-- TOC entry 3666 (class 1259 OID 25189)
-- Name: evaluaciones_asignacion_id_criterio_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX evaluaciones_asignacion_id_criterio_id_idx ON public.evaluaciones USING btree (asignacion_id, criterio_id);


--
-- TOC entry 3684 (class 1259 OID 25191)
-- Name: idx_actividades_evento; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_actividades_evento ON public.actividades USING btree (evento_id, fecha);


--
-- TOC entry 3624 (class 1259 OID 25179)
-- Name: idx_eventos_semestre; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_eventos_semestre ON public.eventos USING btree (semestre);


--
-- TOC entry 3688 (class 1259 OID 25194)
-- Name: idx_inscripciones_evento; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_inscripciones_evento ON public.inscripciones_evento USING btree (evento_id);


--
-- TOC entry 3645 (class 1259 OID 25185)
-- Name: idx_participaciones_persona; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE INDEX idx_participaciones_persona ON public.participaciones_conferencista USING btree (persona_id);


--
-- TOC entry 3692 (class 1259 OID 25195)
-- Name: inscripciones_actividad_inscripcion_id_actividad_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX inscripciones_actividad_inscripcion_id_actividad_id_idx ON public.inscripciones_actividad USING btree (inscripcion_id, actividad_id);


--
-- TOC entry 3689 (class 1259 OID 25193)
-- Name: inscripciones_evento_persona_id_evento_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX inscripciones_evento_persona_id_evento_id_idx ON public.inscripciones_evento USING btree (persona_id, evento_id);


--
-- TOC entry 3631 (class 1259 OID 25182)
-- Name: lineas_tematicas_evento_id_nombre_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX lineas_tematicas_evento_id_nombre_idx ON public.lineas_tematicas USING btree (evento_id, nombre);


--
-- TOC entry 3646 (class 1259 OID 25184)
-- Name: participaciones_conferencista_persona_id_evento_id_tipo_par_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX participaciones_conferencista_persona_id_evento_id_tipo_par_idx ON public.participaciones_conferencista USING btree (persona_id, evento_id, tipo_participacion, tema);


--
-- TOC entry 3710 (class 1259 OID 25197)
-- Name: respuestas_encuesta_pregunta_id_persona_id_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX respuestas_encuesta_pregunta_id_persona_id_idx ON public.respuestas_encuesta USING btree (pregunta_id, persona_id);


--
-- TOC entry 3634 (class 1259 OID 25183)
-- Name: rubros_presupuestales_evento_id_nombre_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX rubros_presupuestales_evento_id_nombre_idx ON public.rubros_presupuestales USING btree (evento_id, nombre);


--
-- TOC entry 3675 (class 1259 OID 25190)
-- Name: salas_evento_id_nombre_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX salas_evento_id_nombre_idx ON public.salas USING btree (evento_id, nombre);


--
-- TOC entry 3628 (class 1259 OID 25181)
-- Name: tipos_actividad_evento_id_nombre_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX tipos_actividad_evento_id_nombre_idx ON public.tipos_actividad USING btree (evento_id, nombre);


--
-- TOC entry 3655 (class 1259 OID 25186)
-- Name: versiones_propuesta_propuesta_id_numero_version_idx; Type: INDEX; Schema: public; Owner: neondb_owner
--

CREATE UNIQUE INDEX versiones_propuesta_propuesta_id_numero_version_idx ON public.versiones_propuesta USING btree (propuesta_id, numero_version);


--
-- TOC entry 3766 (class 2606 OID 25398)
-- Name: actividades actividades_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3767 (class 2606 OID 25413)
-- Name: actividades actividades_linea_tematica_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_linea_tematica_id_fkey FOREIGN KEY (linea_tematica_id) REFERENCES public.lineas_tematicas(linea_tematica_id) DEFERRABLE;


--
-- TOC entry 3768 (class 2606 OID 40994)
-- Name: actividades actividades_ponente_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_ponente_id_fkey FOREIGN KEY (ponente_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3769 (class 2606 OID 25403)
-- Name: actividades actividades_propuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_propuesta_id_fkey FOREIGN KEY (propuesta_id) REFERENCES public.propuestas(propuesta_id) DEFERRABLE;


--
-- TOC entry 3770 (class 2606 OID 25418)
-- Name: actividades actividades_sala_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_sala_id_fkey FOREIGN KEY (sala_id) REFERENCES public.salas(sala_id) DEFERRABLE;


--
-- TOC entry 3771 (class 2606 OID 25408)
-- Name: actividades actividades_tipo_actividad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.actividades
    ADD CONSTRAINT actividades_tipo_actividad_id_fkey FOREIGN KEY (tipo_actividad_id) REFERENCES public.tipos_actividad(tipo_actividad_id) DEFERRABLE;


--
-- TOC entry 3772 (class 2606 OID 25428)
-- Name: agendas_publicadas agendas_publicadas_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.agendas_publicadas
    ADD CONSTRAINT agendas_publicadas_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3755 (class 2606 OID 25348)
-- Name: asignaciones_evaluacion asignaciones_evaluacion_comite_evaluador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asignaciones_evaluacion
    ADD CONSTRAINT asignaciones_evaluacion_comite_evaluador_id_fkey FOREIGN KEY (comite_evaluador_id) REFERENCES public.comites_evaluadores(comite_evaluador_id) DEFERRABLE;


--
-- TOC entry 3756 (class 2606 OID 25343)
-- Name: asignaciones_evaluacion asignaciones_evaluacion_propuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asignaciones_evaluacion
    ADD CONSTRAINT asignaciones_evaluacion_propuesta_id_fkey FOREIGN KEY (propuesta_id) REFERENCES public.propuestas(propuesta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3778 (class 2606 OID 25463)
-- Name: asistencias asistencias_actividad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT asistencias_actividad_id_fkey FOREIGN KEY (actividad_id) REFERENCES public.actividades(actividad_id) DEFERRABLE;


--
-- TOC entry 3779 (class 2606 OID 25458)
-- Name: asistencias asistencias_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.asistencias
    ADD CONSTRAINT asistencias_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripciones_evento(inscripcion_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3732 (class 2606 OID 41070)
-- Name: auditoria auditoria_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.auditoria
    ADD CONSTRAINT auditoria_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) DEFERRABLE;


--
-- TOC entry 3785 (class 2606 OID 25503)
-- Name: certificados certificados_actividad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.certificados
    ADD CONSTRAINT certificados_actividad_id_fkey FOREIGN KEY (actividad_id) REFERENCES public.actividades(actividad_id) DEFERRABLE;


--
-- TOC entry 3786 (class 2606 OID 25498)
-- Name: certificados certificados_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.certificados
    ADD CONSTRAINT certificados_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3787 (class 2606 OID 41009)
-- Name: certificados certificados_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.certificados
    ADD CONSTRAINT certificados_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3777 (class 2606 OID 25453)
-- Name: codigos_qr codigos_qr_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.codigos_qr
    ADD CONSTRAINT codigos_qr_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripciones_evento(inscripcion_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3734 (class 2606 OID 25238)
-- Name: comite_organizador comite_organizador_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comite_organizador
    ADD CONSTRAINT comite_organizador_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3735 (class 2606 OID 40969)
-- Name: comite_organizador comite_organizador_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comite_organizador
    ADD CONSTRAINT comite_organizador_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3753 (class 2606 OID 25333)
-- Name: comites_evaluadores comites_evaluadores_convocatoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comites_evaluadores
    ADD CONSTRAINT comites_evaluadores_convocatoria_id_fkey FOREIGN KEY (convocatoria_id) REFERENCES public.convocatorias(convocatoria_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3754 (class 2606 OID 40984)
-- Name: comites_evaluadores comites_evaluadores_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.comites_evaluadores
    ADD CONSTRAINT comites_evaluadores_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3743 (class 2606 OID 25283)
-- Name: convocatorias convocatorias_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.convocatorias
    ADD CONSTRAINT convocatorias_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3752 (class 2606 OID 25328)
-- Name: criterios_rubrica criterios_rubrica_rubrica_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.criterios_rubrica
    ADD CONSTRAINT criterios_rubrica_rubrica_id_fkey FOREIGN KEY (rubrica_id) REFERENCES public.rubricas_evaluacion(rubrica_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3764 (class 2606 OID 25393)
-- Name: disponibilidad_ponentes disponibilidad_ponentes_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_ponentes
    ADD CONSTRAINT disponibilidad_ponentes_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3765 (class 2606 OID 40989)
-- Name: disponibilidad_ponentes disponibilidad_ponentes_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_ponentes
    ADD CONSTRAINT disponibilidad_ponentes_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3763 (class 2606 OID 25383)
-- Name: disponibilidad_salas disponibilidad_salas_sala_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.disponibilidad_salas
    ADD CONSTRAINT disponibilidad_salas_sala_id_fkey FOREIGN KEY (sala_id) REFERENCES public.salas(sala_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3780 (class 2606 OID 25473)
-- Name: encuestas encuestas_actividad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.encuestas
    ADD CONSTRAINT encuestas_actividad_id_fkey FOREIGN KEY (actividad_id) REFERENCES public.actividades(actividad_id) DEFERRABLE;


--
-- TOC entry 3781 (class 2606 OID 25468)
-- Name: encuestas encuestas_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.encuestas
    ADD CONSTRAINT encuestas_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3757 (class 2606 OID 25353)
-- Name: evaluaciones evaluaciones_asignacion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT evaluaciones_asignacion_id_fkey FOREIGN KEY (asignacion_id) REFERENCES public.asignaciones_evaluacion(asignacion_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3758 (class 2606 OID 25358)
-- Name: evaluaciones evaluaciones_criterio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.evaluaciones
    ADD CONSTRAINT evaluaciones_criterio_id_fkey FOREIGN KEY (criterio_id) REFERENCES public.criterios_rubrica(criterio_id) DEFERRABLE;


--
-- TOC entry 3733 (class 2606 OID 25233)
-- Name: eventos eventos_evento_base_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.eventos
    ADD CONSTRAINT eventos_evento_base_id_fkey FOREIGN KEY (evento_base_id) REFERENCES public.eventos(evento_id) DEFERRABLE;


--
-- TOC entry 3725 (class 2606 OID 49163)
-- Name: personas fk_persona_afiliacion; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.personas
    ADD CONSTRAINT fk_persona_afiliacion FOREIGN KEY (afiliacion_id) REFERENCES public.afiliaciones(id);


--
-- TOC entry 3741 (class 2606 OID 41080)
-- Name: gastos_ejecutados gastos_ejecutados_registrado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.gastos_ejecutados
    ADD CONSTRAINT gastos_ejecutados_registrado_por_fkey FOREIGN KEY (registrado_por) REFERENCES public.usuarios(usuario_id) DEFERRABLE;


--
-- TOC entry 3742 (class 2606 OID 25273)
-- Name: gastos_ejecutados gastos_ejecutados_rubro_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.gastos_ejecutados
    ADD CONSTRAINT gastos_ejecutados_rubro_id_fkey FOREIGN KEY (rubro_id) REFERENCES public.rubros_presupuestales(rubro_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3788 (class 2606 OID 25508)
-- Name: indicadores indicadores_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.indicadores
    ADD CONSTRAINT indicadores_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3775 (class 2606 OID 25448)
-- Name: inscripciones_actividad inscripciones_actividad_actividad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_actividad
    ADD CONSTRAINT inscripciones_actividad_actividad_id_fkey FOREIGN KEY (actividad_id) REFERENCES public.actividades(actividad_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3776 (class 2606 OID 25443)
-- Name: inscripciones_actividad inscripciones_actividad_inscripcion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_actividad
    ADD CONSTRAINT inscripciones_actividad_inscripcion_id_fkey FOREIGN KEY (inscripcion_id) REFERENCES public.inscripciones_evento(inscripcion_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3773 (class 2606 OID 25438)
-- Name: inscripciones_evento inscripciones_evento_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_evento
    ADD CONSTRAINT inscripciones_evento_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3774 (class 2606 OID 40999)
-- Name: inscripciones_evento inscripciones_evento_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.inscripciones_evento
    ADD CONSTRAINT inscripciones_evento_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3737 (class 2606 OID 25253)
-- Name: lineas_tematicas lineas_tematicas_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.lineas_tematicas
    ADD CONSTRAINT lineas_tematicas_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3789 (class 2606 OID 25513)
-- Name: memorias_historicas memorias_historicas_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.memorias_historicas
    ADD CONSTRAINT memorias_historicas_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3760 (class 2606 OID 25373)
-- Name: observaciones_propuesta observaciones_propuesta_comite_evaluador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.observaciones_propuesta
    ADD CONSTRAINT observaciones_propuesta_comite_evaluador_id_fkey FOREIGN KEY (comite_evaluador_id) REFERENCES public.comites_evaluadores(comite_evaluador_id) DEFERRABLE;


--
-- TOC entry 3761 (class 2606 OID 25368)
-- Name: observaciones_propuesta observaciones_propuesta_version_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.observaciones_propuesta
    ADD CONSTRAINT observaciones_propuesta_version_id_fkey FOREIGN KEY (version_id) REFERENCES public.versiones_propuesta(version_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3744 (class 2606 OID 25293)
-- Name: participaciones_conferencista participaciones_conferencista_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.participaciones_conferencista
    ADD CONSTRAINT participaciones_conferencista_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3745 (class 2606 OID 40974)
-- Name: participaciones_conferencista participaciones_conferencista_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.participaciones_conferencista
    ADD CONSTRAINT participaciones_conferencista_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3782 (class 2606 OID 25478)
-- Name: preguntas_encuesta preguntas_encuesta_encuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.preguntas_encuesta
    ADD CONSTRAINT preguntas_encuesta_encuesta_id_fkey FOREIGN KEY (encuesta_id) REFERENCES public.encuestas(encuesta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3739 (class 2606 OID 41075)
-- Name: presupuestos_aprobados presupuestos_aprobados_aprobado_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.presupuestos_aprobados
    ADD CONSTRAINT presupuestos_aprobados_aprobado_por_fkey FOREIGN KEY (aprobado_por) REFERENCES public.usuarios(usuario_id) DEFERRABLE;


--
-- TOC entry 3740 (class 2606 OID 25263)
-- Name: presupuestos_aprobados presupuestos_aprobados_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.presupuestos_aprobados
    ADD CONSTRAINT presupuestos_aprobados_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3748 (class 2606 OID 40979)
-- Name: propuestas_autores propuestas_autores_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas_autores
    ADD CONSTRAINT propuestas_autores_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3749 (class 2606 OID 25308)
-- Name: propuestas_autores propuestas_autores_propuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas_autores
    ADD CONSTRAINT propuestas_autores_propuesta_id_fkey FOREIGN KEY (propuesta_id) REFERENCES public.propuestas(propuesta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3746 (class 2606 OID 25298)
-- Name: propuestas propuestas_convocatoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas
    ADD CONSTRAINT propuestas_convocatoria_id_fkey FOREIGN KEY (convocatoria_id) REFERENCES public.convocatorias(convocatoria_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3747 (class 2606 OID 25303)
-- Name: propuestas propuestas_linea_tematica_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.propuestas
    ADD CONSTRAINT propuestas_linea_tematica_id_fkey FOREIGN KEY (linea_tematica_id) REFERENCES public.lineas_tematicas(linea_tematica_id) DEFERRABLE;


--
-- TOC entry 3783 (class 2606 OID 41004)
-- Name: respuestas_encuesta respuestas_encuesta_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.respuestas_encuesta
    ADD CONSTRAINT respuestas_encuesta_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3784 (class 2606 OID 25483)
-- Name: respuestas_encuesta respuestas_encuesta_pregunta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.respuestas_encuesta
    ADD CONSTRAINT respuestas_encuesta_pregunta_id_fkey FOREIGN KEY (pregunta_id) REFERENCES public.preguntas_encuesta(pregunta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3759 (class 2606 OID 25363)
-- Name: resultados_evaluacion resultados_evaluacion_propuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.resultados_evaluacion
    ADD CONSTRAINT resultados_evaluacion_propuesta_id_fkey FOREIGN KEY (propuesta_id) REFERENCES public.propuestas(propuesta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3727 (class 2606 OID 25208)
-- Name: roles_permisos roles_permisos_permiso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles_permisos
    ADD CONSTRAINT roles_permisos_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES public.permisos(permiso_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3728 (class 2606 OID 41026)
-- Name: roles_permisos roles_permisos_rol_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.roles_permisos
    ADD CONSTRAINT roles_permisos_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES public.roles(rol_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3751 (class 2606 OID 25323)
-- Name: rubricas_evaluacion rubricas_evaluacion_convocatoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubricas_evaluacion
    ADD CONSTRAINT rubricas_evaluacion_convocatoria_id_fkey FOREIGN KEY (convocatoria_id) REFERENCES public.convocatorias(convocatoria_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3738 (class 2606 OID 25258)
-- Name: rubros_presupuestales rubros_presupuestales_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.rubros_presupuestales
    ADD CONSTRAINT rubros_presupuestales_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3762 (class 2606 OID 25378)
-- Name: salas salas_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.salas
    ADD CONSTRAINT salas_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3736 (class 2606 OID 25248)
-- Name: tipos_actividad tipos_actividad_evento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tipos_actividad
    ADD CONSTRAINT tipos_actividad_evento_id_fkey FOREIGN KEY (evento_id) REFERENCES public.eventos(evento_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3731 (class 2606 OID 41085)
-- Name: tokens_recuperacion tokens_recuperacion_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.tokens_recuperacion
    ADD CONSTRAINT tokens_recuperacion_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3726 (class 2606 OID 41099)
-- Name: usuarios usuarios_persona_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_persona_id_fkey FOREIGN KEY (persona_id) REFERENCES public.personas(persona_id) DEFERRABLE;


--
-- TOC entry 3729 (class 2606 OID 41125)
-- Name: usuarios_roles usuarios_roles_rol_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios_roles
    ADD CONSTRAINT usuarios_roles_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES public.roles(rol_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3730 (class 2606 OID 41113)
-- Name: usuarios_roles usuarios_roles_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.usuarios_roles
    ADD CONSTRAINT usuarios_roles_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(usuario_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 3750 (class 2606 OID 25318)
-- Name: versiones_propuesta versiones_propuesta_propuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: neondb_owner
--

ALTER TABLE ONLY public.versiones_propuesta
    ADD CONSTRAINT versiones_propuesta_propuesta_id_fkey FOREIGN KEY (propuesta_id) REFERENCES public.propuestas(propuesta_id) ON DELETE CASCADE DEFERRABLE;


--
-- TOC entry 2268 (class 826 OID 16399)
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: cloud_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO neon_superuser WITH GRANT OPTION;


--
-- TOC entry 2267 (class 826 OID 16398)
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: cloud_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE cloud_admin IN SCHEMA public GRANT ALL ON TABLES TO neon_superuser WITH GRANT OPTION;


-- Completed on 2026-09-27 14:15:27

--
-- PostgreSQL database dump complete
--

\unrestrict OFlTchfxHsnuhTBQk2dW01oY2VobwdDqe888g88ovuRCmGBQjbeqdUpMwgNi3Nm

