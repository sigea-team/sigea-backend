--liquibase formatted sql

-- =====================================================================
-- HU-02 - Administrar roles y permisos: rol base de evaluador
-- =====================================================================

--changeset SIGEA:hu02-001-rol-evaluador
--comment: Rol listo para que el administrador lo asigne a los pares evaluadores. Solo quien tenga un rol con el
--comment: permiso PROPUESTAS_EVALUAR puede ingresar con el perfil «Evaluador» y registrar evaluaciones (HU-17).
-- El catálogo de permisos (3-seed-permisos.sql) está comentado en el master: se asegura el permiso por si no existe.
INSERT INTO "permisos" ("codigo", "modulo", "descripcion")
VALUES ('PROPUESTAS_EVALUAR', 'EVALUACION', 'Evaluar propuestas mediante rúbricas')
ON CONFLICT ("codigo") DO NOTHING;

INSERT INTO "roles" ("nombre", "descripcion")
SELECT 'EVALUADOR', 'Par evaluador: califica las propuestas que le asignan según la rúbrica de la convocatoria'
WHERE NOT EXISTS (SELECT 1 FROM "roles" WHERE UPPER("nombre") = 'EVALUADOR');

INSERT INTO "roles_permisos" ("rol_id", "permiso_id")
SELECT r."rol_id", p."permiso_id"
FROM "roles" r
JOIN "permisos" p ON p."codigo" = 'PROPUESTAS_EVALUAR'
WHERE UPPER(r."nombre") = 'EVALUADOR'
ON CONFLICT DO NOTHING;
