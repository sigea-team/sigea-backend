CREATE TABLE "personas" (
  "persona_id" SERIAL PRIMARY KEY,
  "tipo_documento" VARCHAR(20) NOT NULL,
  "numero_documento" VARCHAR(30) UNIQUE NOT NULL,
  "nombres" VARCHAR(100) NOT NULL,
  "apellidos" VARCHAR(100) NOT NULL,
  "correo" VARCHAR(150) UNIQUE NOT NULL,
  "telefono" VARCHAR(30),
  "afiliacion_institucional" VARCHAR(150)
);

CREATE TABLE "usuarios" (
  "usuario_id" SERIAL PRIMARY KEY,
  "persona_id" INT UNIQUE NOT NULL,
  "nombre_usuario" VARCHAR(50) UNIQUE NOT NULL,
  "contrasena_hash" VARCHAR(255) NOT NULL,
  "correo_verificado" BOOLEAN NOT NULL DEFAULT false,
  "estado" VARCHAR(20) NOT NULL CHECK (estado IN ('activo','bloqueado','inactivo')) DEFAULT 'activo',
  "fecha_registro" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "roles" (
  "rol_id" SERIAL PRIMARY KEY,
  "nombre" VARCHAR(50) UNIQUE NOT NULL,
  "descripcion" VARCHAR(255)
);

CREATE TABLE "permisos" (
  "permiso_id" SERIAL PRIMARY KEY,
  "codigo" VARCHAR(50) UNIQUE NOT NULL,
  "modulo" VARCHAR(50) NOT NULL,
  "descripcion" VARCHAR(255)
);

CREATE TABLE "roles_permisos" (
  "rol_id" INT NOT NULL,
  "permiso_id" INT NOT NULL,
  PRIMARY KEY ("rol_id", "permiso_id")
);

CREATE TABLE "usuarios_roles" (
  "usuario_id" INT NOT NULL,
  "rol_id" INT NOT NULL,
  "fecha_asignacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  PRIMARY KEY ("usuario_id", "rol_id")
);

CREATE TABLE "tokens_recuperacion" (
  "token_id" SERIAL PRIMARY KEY,
  "usuario_id" INT NOT NULL,
  "token" VARCHAR(255) UNIQUE NOT NULL,
  "tipo" VARCHAR(20) NOT NULL CHECK (tipo IN ('verificacion','recuperacion')),
  "fecha_generacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "fecha_expiracion" TIMESTAMP NOT NULL,
  "usado" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "auditoria" (
  "auditoria_id" SERIAL PRIMARY KEY,
  "usuario_id" INT,
  "accion" VARCHAR(100) NOT NULL,
  "entidad" VARCHAR(100) NOT NULL,
  "entidad_id" INT,
  "fecha_hora" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "detalle" TEXT
);

CREATE TABLE "eventos" (
  "evento_id" SERIAL PRIMARY KEY,
  "nombre" VARCHAR(200) NOT NULL,
  "objetivo" TEXT,
  "descripcion" TEXT,
  "tipo" VARCHAR(50),
  "modalidad" VARCHAR(20) CHECK (modalidad IN ('presencial','virtual','hibrida')),
  "fecha_inicio" DATE NOT NULL,
  "fecha_fin" DATE NOT NULL,
  "semestre" VARCHAR(10),
  "estado" VARCHAR(30) NOT NULL CHECK (estado IN ('en_configuracion','habilitado','en_ejecucion','cerrado')) DEFAULT 'en_configuracion',
  "evento_base_id" INT,
  CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE "comite_organizador" (
  "comite_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "persona_id" INT NOT NULL,
  "rol_comite" VARCHAR(50) NOT NULL,
  "fecha_asignacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "activo" BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE "tipos_actividad" (
  "tipo_actividad_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(80) NOT NULL,
  "descripcion" VARCHAR(255)
);

CREATE TABLE "lineas_tematicas" (
  "linea_tematica_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(120) NOT NULL,
  "descripcion" VARCHAR(255)
);

CREATE TABLE "rubros_presupuestales" (
  "rubro_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(100) NOT NULL,
  "cantidad" NUMERIC(10,2) NOT NULL CHECK (cantidad >= 0),
  "valor_unitario_proyectado" NUMERIC(14,2) NOT NULL CHECK (valor_unitario_proyectado >= 0),
  "activo" BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE "presupuestos_aprobados" (
  "aprobacion_id" SERIAL PRIMARY KEY,
  "evento_id" INT UNIQUE NOT NULL,
  "valor_total_aprobado" NUMERIC(14,2) NOT NULL CHECK (valor_total_aprobado >= 0),
  "evidencia_url" VARCHAR(255) NOT NULL,
  "fecha_aprobacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "aprobado_por" INT NOT NULL,
  "estado" VARCHAR(20) NOT NULL DEFAULT 'aprobado'
);

CREATE TABLE "gastos_ejecutados" (
  "gasto_id" SERIAL PRIMARY KEY,
  "rubro_id" INT NOT NULL,
  "descripcion" VARCHAR(255),
  "valor" NUMERIC(14,2) NOT NULL CHECK (valor >= 0),
  "fecha_gasto" DATE NOT NULL,
  "soporte_url" VARCHAR(255),
  "registrado_por" INT NOT NULL
);

CREATE TABLE "convocatorias" (
  "convocatoria_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "titulo" VARCHAR(150) NOT NULL,
  "descripcion" TEXT,
  "requisitos" TEXT,
  "fecha_apertura" TIMESTAMP NOT NULL,
  "fecha_cierre" TIMESTAMP NOT NULL,
  "estado" VARCHAR(20) NOT NULL CHECK (estado IN ('borrador','publicada','cerrada')) DEFAULT 'borrador',
  CHECK (fecha_cierre > fecha_apertura)
);

CREATE TABLE "participaciones_conferencista" (
  "participacion_id" SERIAL PRIMARY KEY,
  "persona_id" INT NOT NULL,
  "evento_id" INT NOT NULL,
  "tipo_participacion" VARCHAR(20) NOT NULL CHECK (tipo_participacion IN ('conferencista','ponente')),
  "tema" VARCHAR(200),
  "fecha_registro" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "propuestas" (
  "propuesta_id" SERIAL PRIMARY KEY,
  "convocatoria_id" INT NOT NULL,
  "titulo" VARCHAR(200) NOT NULL,
  "resumen" TEXT,
  "palabras_clave" VARCHAR(255),
  "linea_tematica_id" INT,
  "estado" VARCHAR(20) NOT NULL CHECK (estado IN ('recibida','en_evaluacion','aprobada','ajustes','rechazada')) DEFAULT 'recibida',
  "fecha_envio" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "propuestas_autores" (
  "propuesta_id" INT NOT NULL,
  "persona_id" INT NOT NULL,
  "orden" INT NOT NULL DEFAULT 1,
  PRIMARY KEY ("propuesta_id", "persona_id")
);

CREATE TABLE "versiones_propuesta" (
  "version_id" SERIAL PRIMARY KEY,
  "propuesta_id" INT NOT NULL,
  "numero_version" INT NOT NULL,
  "documento_url" VARCHAR(255) NOT NULL,
  "observaciones" TEXT,
  "fecha_creacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "rubricas_evaluacion" (
  "rubrica_id" SERIAL PRIMARY KEY,
  "convocatoria_id" INT NOT NULL,
  "nombre" VARCHAR(100) NOT NULL,
  "activo" BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE "criterios_rubrica" (
  "criterio_id" SERIAL PRIMARY KEY,
  "rubrica_id" INT NOT NULL,
  "nombre" VARCHAR(100) NOT NULL,
  "peso_porcentual" NUMERIC(5,2) NOT NULL CHECK (peso_porcentual > 0 AND peso_porcentual <= 100)
);

CREATE TABLE "comites_evaluadores" (
  "comite_evaluador_id" SERIAL PRIMARY KEY,
  "convocatoria_id" INT NOT NULL,
  "persona_id" INT NOT NULL,
  "area_experticia" VARCHAR(150)
);

CREATE TABLE "asignaciones_evaluacion" (
  "asignacion_id" SERIAL PRIMARY KEY,
  "propuesta_id" INT NOT NULL,
  "comite_evaluador_id" INT NOT NULL,
  "fecha_asignacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "estado" VARCHAR(20) NOT NULL CHECK (estado IN ('pendiente','en_proceso','completada')) DEFAULT 'pendiente'
);

CREATE TABLE "evaluaciones" (
  "evaluacion_id" SERIAL PRIMARY KEY,
  "asignacion_id" INT NOT NULL,
  "criterio_id" INT NOT NULL,
  "calificacion" NUMERIC(5,2) NOT NULL,
  "observaciones" TEXT,
  "fecha_evaluacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "resultados_evaluacion" (
  "resultado_id" SERIAL PRIMARY KEY,
  "propuesta_id" INT UNIQUE NOT NULL,
  "puntuacion_ponderada" NUMERIC(5,2) NOT NULL,
  "clasificacion" VARCHAR(30) NOT NULL CHECK (clasificacion IN ('aprobada','aprobada_con_ajustes','rechazada')),
  "fecha_calculo" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "observaciones_propuesta" (
  "observacion_id" SERIAL PRIMARY KEY,
  "version_id" INT NOT NULL,
  "comite_evaluador_id" INT NOT NULL,
  "descripcion" TEXT NOT NULL,
  "fecha" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "resuelto" BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE "salas" (
  "sala_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(100) NOT NULL,
  "ubicacion" VARCHAR(150),
  "tipo" VARCHAR(20) NOT NULL CHECK (tipo IN ('fisica','virtual')),
  "aforo_maximo" INT NOT NULL CHECK (aforo_maximo > 0)
);

CREATE TABLE "disponibilidad_salas" (
  "disponibilidad_id" SERIAL PRIMARY KEY,
  "sala_id" INT NOT NULL,
  "fecha" DATE NOT NULL,
  "hora_inicio" TIME NOT NULL,
  "hora_fin" TIME NOT NULL,
  CHECK (hora_fin > hora_inicio)
);

CREATE TABLE "disponibilidad_ponentes" (
  "disponibilidad_id" SERIAL PRIMARY KEY,
  "persona_id" INT NOT NULL,
  "evento_id" INT NOT NULL,
  "fecha" DATE NOT NULL,
  "hora_inicio" TIME NOT NULL,
  "hora_fin" TIME NOT NULL,
  CHECK (hora_fin > hora_inicio)
);

CREATE TABLE "actividades" (
  "actividad_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "propuesta_id" INT,
  "tipo_actividad_id" INT NOT NULL,
  "linea_tematica_id" INT,
  "nombre" VARCHAR(200) NOT NULL,
  "fecha" DATE NOT NULL,
  "hora_inicio" TIME NOT NULL,
  "hora_fin" TIME NOT NULL,
  "sala_id" INT NOT NULL,
  "ponente_id" INT,
  "modalidad" VARCHAR(20) CHECK (modalidad IN ('presencial','virtual','hibrida')),
  "permite_simultaneidad" BOOLEAN NOT NULL DEFAULT false,
  "estado" VARCHAR(20) NOT NULL DEFAULT 'programada',
  CHECK (hora_fin > hora_inicio)
);

CREATE TABLE "agendas_publicadas" (
  "agenda_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "fecha_dia" DATE NOT NULL,
  "fecha_publicacion" TIMESTAMP,
  "estado" VARCHAR(20) NOT NULL CHECK (estado IN ('no_publicada','publicada')) DEFAULT 'no_publicada'
);

CREATE TABLE "inscripciones_evento" (
  "inscripcion_id" SERIAL PRIMARY KEY,
  "persona_id" INT NOT NULL,
  "evento_id" INT NOT NULL,
  "categoria_participante" VARCHAR(30) NOT NULL CHECK (categoria_participante IN ('estudiante','egresado','docente','particular')),
  "fecha_inscripcion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "inscripciones_actividad" (
  "inscripcion_actividad_id" SERIAL PRIMARY KEY,
  "inscripcion_id" INT NOT NULL,
  "actividad_id" INT NOT NULL,
  "fecha_inscripcion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "codigos_qr" (
  "qr_id" SERIAL PRIMARY KEY,
  "inscripcion_id" INT UNIQUE NOT NULL,
  "codigo" VARCHAR(100) UNIQUE NOT NULL,
  "fecha_generacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "vigente" BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE "asistencias" (
  "asistencia_id" SERIAL PRIMARY KEY,
  "inscripcion_id" INT NOT NULL,
  "actividad_id" INT,
  "fecha_hora_registro" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "metodo_registro" VARCHAR(20) NOT NULL CHECK (metodo_registro IN ('qr','manual'))
);

CREATE TABLE "encuestas" (
  "encuesta_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "actividad_id" INT,
  "titulo" VARCHAR(150) NOT NULL,
  "estado" VARCHAR(20) NOT NULL DEFAULT 'activa'
);

CREATE TABLE "preguntas_encuesta" (
  "pregunta_id" SERIAL PRIMARY KEY,
  "encuesta_id" INT NOT NULL,
  "texto" TEXT NOT NULL,
  "tipo_respuesta" VARCHAR(20) NOT NULL CHECK (tipo_respuesta IN ('escala','texto','opcion')),
  "orden" INT NOT NULL DEFAULT 1
);

CREATE TABLE "respuestas_encuesta" (
  "respuesta_id" SERIAL PRIMARY KEY,
  "pregunta_id" INT NOT NULL,
  "persona_id" INT NOT NULL,
  "respuesta" TEXT,
  "fecha_respuesta" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "certificados" (
  "certificado_id" SERIAL PRIMARY KEY,
  "persona_id" INT NOT NULL,
  "evento_id" INT NOT NULL,
  "actividad_id" INT,
  "tipo_certificado" VARCHAR(20) NOT NULL CHECK (tipo_certificado IN ('asistente','ponente','evaluador','organizador')),
  "porcentaje_asistencia" NUMERIC(5,2),
  "fecha_generacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "archivo_url" VARCHAR(255) NOT NULL
);

CREATE TABLE "indicadores" (
  "indicador_id" SERIAL PRIMARY KEY,
  "evento_id" INT NOT NULL,
  "nombre" VARCHAR(100) NOT NULL,
  "valor" NUMERIC(14,2) NOT NULL,
  "unidad" VARCHAR(20),
  "fecha_calculo" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP)
);

CREATE TABLE "memorias_historicas" (
  "memoria_id" SERIAL PRIMARY KEY,
  "evento_id" INT UNIQUE NOT NULL,
  "fecha_consolidacion" TIMESTAMP NOT NULL DEFAULT (CURRENT_TIMESTAMP),
  "archivo_url" VARCHAR(255),
  "resumen" TEXT
);

CREATE INDEX "idx_eventos_semestre" ON "eventos" ("semestre");

CREATE UNIQUE INDEX ON "comite_organizador" ("evento_id", "persona_id", "rol_comite");

CREATE UNIQUE INDEX ON "tipos_actividad" ("evento_id", "nombre");

CREATE UNIQUE INDEX ON "lineas_tematicas" ("evento_id", "nombre");

CREATE UNIQUE INDEX ON "rubros_presupuestales" ("evento_id", "nombre");

CREATE UNIQUE INDEX ON "participaciones_conferencista" ("persona_id", "evento_id", "tipo_participacion", "tema");

CREATE INDEX "idx_participaciones_persona" ON "participaciones_conferencista" ("persona_id");

CREATE UNIQUE INDEX ON "versiones_propuesta" ("propuesta_id", "numero_version");

CREATE UNIQUE INDEX ON "comites_evaluadores" ("convocatoria_id", "persona_id");

CREATE UNIQUE INDEX ON "asignaciones_evaluacion" ("propuesta_id", "comite_evaluador_id");

CREATE UNIQUE INDEX ON "evaluaciones" ("asignacion_id", "criterio_id");

CREATE UNIQUE INDEX ON "salas" ("evento_id", "nombre");

CREATE INDEX "idx_actividades_evento" ON "actividades" ("evento_id", "fecha");

CREATE UNIQUE INDEX ON "agendas_publicadas" ("evento_id", "fecha_dia");

CREATE UNIQUE INDEX ON "inscripciones_evento" ("persona_id", "evento_id");

CREATE INDEX "idx_inscripciones_evento" ON "inscripciones_evento" ("evento_id");

CREATE UNIQUE INDEX ON "inscripciones_actividad" ("inscripcion_id", "actividad_id");

CREATE UNIQUE INDEX ON "asistencias" ("inscripcion_id", "actividad_id");

CREATE UNIQUE INDEX ON "respuestas_encuesta" ("pregunta_id", "persona_id");

ALTER TABLE "usuarios" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "roles_permisos" ADD FOREIGN KEY ("rol_id") REFERENCES "roles" ("rol_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "roles_permisos" ADD FOREIGN KEY ("permiso_id") REFERENCES "permisos" ("permiso_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "usuarios_roles" ADD FOREIGN KEY ("usuario_id") REFERENCES "usuarios" ("usuario_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "usuarios_roles" ADD FOREIGN KEY ("rol_id") REFERENCES "roles" ("rol_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tokens_recuperacion" ADD FOREIGN KEY ("usuario_id") REFERENCES "usuarios" ("usuario_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "auditoria" ADD FOREIGN KEY ("usuario_id") REFERENCES "usuarios" ("usuario_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "eventos" ADD FOREIGN KEY ("evento_base_id") REFERENCES "eventos" ("evento_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comite_organizador" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comite_organizador" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "tipos_actividad" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "lineas_tematicas" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "rubros_presupuestales" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "presupuestos_aprobados" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "presupuestos_aprobados" ADD FOREIGN KEY ("aprobado_por") REFERENCES "usuarios" ("usuario_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "gastos_ejecutados" ADD FOREIGN KEY ("rubro_id") REFERENCES "rubros_presupuestales" ("rubro_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "gastos_ejecutados" ADD FOREIGN KEY ("registrado_por") REFERENCES "usuarios" ("usuario_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "convocatorias" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "participaciones_conferencista" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "participaciones_conferencista" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "propuestas" ADD FOREIGN KEY ("convocatoria_id") REFERENCES "convocatorias" ("convocatoria_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "propuestas" ADD FOREIGN KEY ("linea_tematica_id") REFERENCES "lineas_tematicas" ("linea_tematica_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "propuestas_autores" ADD FOREIGN KEY ("propuesta_id") REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "propuestas_autores" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "versiones_propuesta" ADD FOREIGN KEY ("propuesta_id") REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "rubricas_evaluacion" ADD FOREIGN KEY ("convocatoria_id") REFERENCES "convocatorias" ("convocatoria_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "criterios_rubrica" ADD FOREIGN KEY ("rubrica_id") REFERENCES "rubricas_evaluacion" ("rubrica_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comites_evaluadores" ADD FOREIGN KEY ("convocatoria_id") REFERENCES "convocatorias" ("convocatoria_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "comites_evaluadores" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "asignaciones_evaluacion" ADD FOREIGN KEY ("propuesta_id") REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "asignaciones_evaluacion" ADD FOREIGN KEY ("comite_evaluador_id") REFERENCES "comites_evaluadores" ("comite_evaluador_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "evaluaciones" ADD FOREIGN KEY ("asignacion_id") REFERENCES "asignaciones_evaluacion" ("asignacion_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "evaluaciones" ADD FOREIGN KEY ("criterio_id") REFERENCES "criterios_rubrica" ("criterio_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "resultados_evaluacion" ADD FOREIGN KEY ("propuesta_id") REFERENCES "propuestas" ("propuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "observaciones_propuesta" ADD FOREIGN KEY ("version_id") REFERENCES "versiones_propuesta" ("version_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "observaciones_propuesta" ADD FOREIGN KEY ("comite_evaluador_id") REFERENCES "comites_evaluadores" ("comite_evaluador_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "salas" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "disponibilidad_salas" ADD FOREIGN KEY ("sala_id") REFERENCES "salas" ("sala_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "disponibilidad_ponentes" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "disponibilidad_ponentes" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("propuesta_id") REFERENCES "propuestas" ("propuesta_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("tipo_actividad_id") REFERENCES "tipos_actividad" ("tipo_actividad_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("linea_tematica_id") REFERENCES "lineas_tematicas" ("linea_tematica_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("sala_id") REFERENCES "salas" ("sala_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "actividades" ADD FOREIGN KEY ("ponente_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "agendas_publicadas" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "inscripciones_evento" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "inscripciones_evento" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "inscripciones_actividad" ADD FOREIGN KEY ("inscripcion_id") REFERENCES "inscripciones_evento" ("inscripcion_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "inscripciones_actividad" ADD FOREIGN KEY ("actividad_id") REFERENCES "actividades" ("actividad_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "codigos_qr" ADD FOREIGN KEY ("inscripcion_id") REFERENCES "inscripciones_evento" ("inscripcion_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "asistencias" ADD FOREIGN KEY ("inscripcion_id") REFERENCES "inscripciones_evento" ("inscripcion_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "asistencias" ADD FOREIGN KEY ("actividad_id") REFERENCES "actividades" ("actividad_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "encuestas" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "encuestas" ADD FOREIGN KEY ("actividad_id") REFERENCES "actividades" ("actividad_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "preguntas_encuesta" ADD FOREIGN KEY ("encuesta_id") REFERENCES "encuestas" ("encuesta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "respuestas_encuesta" ADD FOREIGN KEY ("pregunta_id") REFERENCES "preguntas_encuesta" ("pregunta_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "respuestas_encuesta" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "certificados" ADD FOREIGN KEY ("persona_id") REFERENCES "personas" ("persona_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "certificados" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "certificados" ADD FOREIGN KEY ("actividad_id") REFERENCES "actividades" ("actividad_id") DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "indicadores" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "memorias_historicas" ADD FOREIGN KEY ("evento_id") REFERENCES "eventos" ("evento_id") ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE;
