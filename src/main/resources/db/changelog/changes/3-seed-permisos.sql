--liquibase formatted sql

--changeset SIGEA:003-seed-catalogo-permisos
--validCheckSum: ANY
INSERT INTO "permisos" ("codigo", "modulo", "descripcion") VALUES
  -- Módulo Roles y Permisos
  ('ROLES_VER', 'ROLES', 'Visualizar roles y sus permisos asociados'),
  ('ROLES_CREAR', 'ROLES', 'Crear nuevos roles y asignar permisos'),
  ('ROLES_EDITAR', 'ROLES', 'Modificar roles existentes y sus permisos'),
  ('ROLES_ELIMINAR', 'ROLES', 'Eliminar roles sin usuarios activos asignados'),

  -- Módulo Usuarios
  ('USUARIOS_VER', 'USUARIOS', 'Consultar lista y detalle de usuarios'),
  ('USUARIOS_GESTIONAR_ROLES', 'USUARIOS', 'Asignar o remover roles a usuarios'),
  ('USUARIOS_ESTADO_EDITAR', 'USUARIOS', 'Modificar el estado de una cuenta de usuario'),

  -- Módulo Eventos
  ('EVENTOS_VER', 'EVENTOS', 'Consultar eventos académicos'),
  ('EVENTOS_CREAR', 'EVENTOS', 'Crear y configurar nuevos eventos'),
  ('EVENTOS_EDITAR', 'EVENTOS', 'Modificar configuración de eventos'),
  ('EVENTOS_ELIMINAR', 'EVENTOS', 'Eliminar eventos en configuración'),

  -- Módulo Convocatorias y Propuestas
  ('CONVOCATORIAS_GESTIONAR', 'CONVOCATORIAS', 'Crear, publicar y gestionar convocatorias'),
  ('PROPUESTAS_VER', 'PROPUESTAS', 'Visualizar propuestas académicas'),
  ('PROPUESTAS_EVALUAR', 'EVALUACION', 'Evaluar propuestas mediante rúbricas'),

  -- Módulo Agenda y Actividades
  ('AGENDA_GESTIONAR', 'AGENDA', 'Administrar cronograma, salas y actividades'),
  ('AGENDA_PUBLICAR', 'AGENDA', 'Publicar agenda del evento al público'),

  -- Módulo Inscripciones y Asistencia
  ('INSCRIPCIONES_GESTIONAR', 'INSCRIPCIONES', 'Administrar inscripciones a eventos y actividades'),
  ('ASISTENCIA_REGISTRAR', 'ASISTENCIA', 'Registrar asistencia de participantes y acreditación'),

  -- Módulo Certificados y Métricas
  ('CERTIFICADOS_EMITIR', 'CERTIFICADOS', 'Generar y emitir certificados académicos'),
  ('REPORTES_VER', 'REPORTES', 'Visualizar indicadores y reportes de gestión')
ON CONFLICT ("codigo") DO NOTHING;
