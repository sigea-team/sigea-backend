# Documento de Requerimientos – SIGEA

**Asignatura:** Análisis y Diseño de Sistemas
**Tablero Jira:** [ufps-team-sigea / SCRUM](https://ufps-team-sigea.atlassian.net/jira/software/projects/SCRUM/boards/1)
**Sprints documentados:**
- SCRUM Sprint 1 — *Establecer la base de seguridad y planeación de eventos* (15/09/2026 – 05/10/2026)
- SCRUM Sprint 2 — *Planear los recursos financieros del evento y abrir su participación externa* (05/10/2026 – 19/10/2026)
**Fuentes:** EP2 – Documento de requisitos (SRS), D7 – Descripción de requisitos, EP3 – Casos de uso de alto nivel, HU vs Casos de uso.

---

## Historial de versiones

| Versión | Fecha      | Autor        | Descripción del cambio                                         |
|---------|------------|--------------|----------------------------------------------------------------|
| 1.0     | 2026-10-08 | Equipo SIGEA | Versión inicial: casos de uso, RF y RNF abordados en el Sprint 1 |
| 2.0     | 2026-10-22 | Equipo SIGEA | Se agregan actores, casos de uso, RF y RNF del Sprint 2, con PR y pruebas que los verifican |

---

# Sprint 1

## 1. Actores

| Actor                       | Descripción                                                  |
|-----------------------------|--------------------------------------------------------------|
| Visitante (usuario nuevo)   | Se registra en la plataforma y verifica su correo.           |
| Usuario registrado          | Inicia sesión y recupera su contraseña.                      |
| Administrador               | Gestiona roles y permisos y consulta la auditoría.           |
| Administrador / Organizador | Gestiona eventos y ediciones, parámetros y comité organizador. |

---

## 2. Casos de uso de alto nivel cubiertos en el Sprint

| ID    | Caso de uso                                                        | Actor principal                |
|-------|--------------------------------------------------------------------|--------------------------------|
| CU-01 | Autenticar Usuario                                                 | Usuario (todos los roles)      |
| CU-27 | Registrar Usuario                                                  | Visitante (usuario nuevo)      |
| CU-28 | Recuperar Contraseña                                               | Usuario registrado             |
| CU-02 | Gestionar Roles y Permisos                                         | Administrador                  |
| CU-03 | Auditar Operaciones Críticas                                       | Administrador (registro automático del sistema) |
| CU-04 | Gestionar Eventos y Ediciones                                      | Administrador / Organizador    |
| CU-05 | Configurar Parámetros del Evento (Tipos de Actividades y Líneas Temáticas) | Administrador / Organizador |
| CU-06 | Asignar Comité Organizador y Responsables                          | Administrador / Organizador    |

---

## 3. Requerimientos funcionales

| ID          | Requerimiento funcional                              | Caso de uso | Historia de usuario | Historia Jira | Estado      |
|-------------|------------------------------------------------------|-------------|---------------------|---------------|-------------|
| RF01        | Autenticación de usuarios según su rol               | CU-01       | HU-01               | [SCRUM-6](https://ufps-team-sigea.atlassian.net/browse/SCRUM-6)   | Finalizado  |
| RF57        | Registro de usuarios con verificación de correo      | CU-27       | HU-31               | [SCRUM-11](https://ufps-team-sigea.atlassian.net/browse/SCRUM-11) | Finalizado  |
| RF58        | Recuperación de contraseña                           | CU-28       | HU-32               | [SCRUM-12](https://ufps-team-sigea.atlassian.net/browse/SCRUM-12) | Finalizado  |
| RF56        | Registro de auditoría de operaciones críticas        | CU-03       | HU-03               | [SCRUM-13](https://ufps-team-sigea.atlassian.net/browse/SCRUM-13) | Finalizado  |
| RF03 · RF04 | Creación y configuración de eventos · Gestión de ediciones | CU-04 | HU-04               | [SCRUM-10](https://ufps-team-sigea.atlassian.net/browse/SCRUM-10) | Finalizado  |
| RF05        | Registro de responsables y comité organizador        | CU-06       | HU-06               | [SCRUM-14](https://ufps-team-sigea.atlassian.net/browse/SCRUM-14) | Finalizado  |
| RF02        | Administración de roles y permisos                   | CU-02       | HU-02               | [SCRUM-7](https://ufps-team-sigea.atlassian.net/browse/SCRUM-7)   | En revisión* |
| RF06 · RF07 | Tipos de actividades · Líneas temáticas              | CU-05       | HU-05               | [SCRUM-9](https://ufps-team-sigea.atlassian.net/browse/SCRUM-9)   | En revisión* |


> \* SCRUM-7 y SCRUM-9 no se cerraron en el Sprint 1 y continúan en el Sprint 2.

---

## 4. Requerimientos no funcionales asociados al Sprint

| ID    | Requerimiento no funcional (SRS)             | Categoría | RF asociados             | Métrica                                         | Verificación                                                   |
|-------|----------------------------------------------|-----------|--------------------------|-------------------------------------------------|----------------------------------------------------------------|
| RNF01 | Rendimiento en operaciones comunes           | Rendimiento | RF01                   | ≤ 3 s bajo carga normal                          | Prueba de carga (pendiente de ejecutar)                         |
| RNF04 | Almacenamiento seguro de contraseñas         | Seguridad | RF01, RF57, RF58         | 100 % de contraseñas almacenadas con hash       | Revisión de la tabla de usuarios y del código (BCrypt)          |
| RNF05 | Comunicaciones cifradas                      | Seguridad | RF01, RF57, RF58         | 100 % de las peticiones por HTTPS en producción | Inspección de la URL y del certificado del despliegue          |
| RNF06 | Control de acceso por roles                  | Seguridad | RF01, RF02, RF05         | 0 accesos a funciones no permitidas por el rol  | Pruebas por rol: acción no autorizada → acceso denegado (403)   |
| RNF07 | Registro de operaciones críticas             | Auditoría | RF02, RF56               | 100 % de operaciones críticas registradas (usuario, acción, fecha y hora) | Consulta del log de auditoría tras ejecutar cada operación |

---

# Sprint 2

## 1. Actores

| Actor | Descripción |
|---|---|
| Administrador / Organizador | Elabora el presupuesto, registra gastos, crea y publica convocatorias, gestiona conferencistas, roles y parámetros. |
| Conferencista | Registra sus datos y trayectoria desde el enlace de invitación (HU-12). |
| Autor / Ponente | Registra sus datos al enviar una propuesta (HU-13). |

---

## 2. Casos de uso de alto nivel cubiertos en el Sprint

| ID | Caso de uso | Actor principal |
|---|---|---|
| CU-02 | Gestionar Roles y Permisos (continúa del Sprint 1) | Administrador |
| CU-05 | Configurar Parámetros del Evento (continúa del Sprint 1) | Administrador / Organizador |
| CU-07 | Elaborar Presupuesto Preliminar | Administrador / Organizador |
| CU-29 | Registrar Presupuesto Aprobado | Administrador / Organizador |
| CU-08 | Registrar Gastos y Ejecución Presupuestal | Administrador / Organizador |
| CU-09 | Crear y Configurar Convocatoria | Administrador / Organizador |
| CU-30 | Publicar Convocatoria (extiende CU-09) | Administrador / Organizador |
| CU-10 | Gestionar Conferencistas | Administrador / Organizador, Conferencista |
| CU-11 | Registrar Autores y Ponentes | Autor / Ponente |

---

## 3. Requerimientos funcionales

| ID | Requerimiento funcional | Caso de uso | Historia de usuario | Historia Jira | Estado | Pull requests | Prueba que lo verifica |
|---|---|---|---|---|---|---|---|
| RF02 | Administración de roles y permisos | CU-02 | HU-02 | [SCRUM-7](https://ufps-team-sigea.atlassian.net/browse/SCRUM-7) | En revisión* | [back #12](https://github.com/sigea-team/sigea-backend/pull/12) · [front #5](https://github.com/sigea-team/sigea-front/pull/5), [front #6](https://github.com/sigea-team/sigea-front/pull/6), [front #8](https://github.com/sigea-team/sigea-front/pull/8) | RolServiceTest, RolSincronizacionPermisosE2ETest |
| RF06 · RF07 | Tipos de actividades · Líneas temáticas | CU-05 | HU-05 | [SCRUM-9](https://ufps-team-sigea.atlassian.net/browse/SCRUM-9) | Finalizado | [back #21](https://github.com/sigea-team/sigea-backend/pull/21) · [front #17](https://github.com/sigea-team/sigea-front/pull/17) | TipoActividadServiceTest, LineaTematicaServiceTest, ParametroEventoReglasTest |
| RF08 · RF09 | Presupuesto preliminar · Rubros presupuestales | CU-07 | HU-07 | [SCRUM-18](https://ufps-team-sigea.atlassian.net/browse/SCRUM-18) | Finalizado | [back #31](https://github.com/sigea-team/sigea-backend/pull/31) · [front #21](https://github.com/sigea-team/sigea-front/pull/21) | PresupuestoPreliminarServiceTest |
| RF10 | Registro del presupuesto aprobado | CU-29 | HU-08 | [SCRUM-20](https://ufps-team-sigea.atlassian.net/browse/SCRUM-20) | Finalizado | [back #34](https://github.com/sigea-team/sigea-backend/pull/34) · [front #23](https://github.com/sigea-team/sigea-front/pull/23) | PresupuestoAprobadoServiceTest |
| RF11 · RF12 | Gastos ejecutados · Ejecución presupuestal | CU-08 | HU-09 | [SCRUM-21](https://ufps-team-sigea.atlassian.net/browse/SCRUM-21) | Finalizado | [back #35](https://github.com/sigea-team/sigea-backend/pull/35), [back #36](https://github.com/sigea-team/sigea-backend/pull/36) · [front #24](https://github.com/sigea-team/sigea-front/pull/24) | EjecucionPresupuestalServiceTest |
| RF13 · RF15 | Crear convocatoria · Fechas de apertura y cierre | CU-09 | HU-10 | [SCRUM-16](https://ufps-team-sigea.atlassian.net/browse/SCRUM-16) | Finalizado | [back #25](https://github.com/sigea-team/sigea-backend/pull/25), [back #27](https://github.com/sigea-team/sigea-backend/pull/27) · [front #15](https://github.com/sigea-team/sigea-front/pull/15), [front #20](https://github.com/sigea-team/sigea-front/pull/20), [front #28](https://github.com/sigea-team/sigea-front/pull/28) | ConvocatoriaServiceTest |
| RF14 | Publicar convocatorias | CU-30 | HU-11 | [SCRUM-25](https://ufps-team-sigea.atlassian.net/browse/SCRUM-25) | Finalizado | [back #37](https://github.com/sigea-team/sigea-backend/pull/37) · [front #25](https://github.com/sigea-team/sigea-front/pull/25) | ConvocatoriaPublicaControllerTest |
| RF16 · RF17 | Conferencistas · Trayectoria del conferencista | CU-10 | HU-12 | [SCRUM-27](https://ufps-team-sigea.atlassian.net/browse/SCRUM-27) | En revisión* | Rama feature/HU12-conferencistas (sin integrar) | Pendiente |
| RF18 | Registro de autores y ponentes | CU-11 | HU-13 | [SCRUM-28](https://ufps-team-sigea.atlassian.net/browse/SCRUM-28) | Finalizado | [back #38](https://github.com/sigea-team/sigea-backend/pull/38) · [front #26](https://github.com/sigea-team/sigea-front/pull/26) | PropuestaServiceTest |

> \* SCRUM-7 (HU-02) y SCRUM-27 (HU-12) no se cerraron en el Sprint 2 y continúan en el Sprint 3. SCRUM-9 (HU-05), que venía del Sprint 1, quedó Finalizado.

---

## 4. Requerimientos no funcionales asociados al Sprint

| ID | Requerimiento no funcional (SRS) | Categoría | RF asociados | Métrica | Verificación |
|---|---|---|---|---|---|
| RNF09 | Integridad del presupuesto | Integridad | RF08, RF09, RF11, RF12 | Total y % de ejecución recalculados en el 100 % de las altas, ediciones y eliminaciones | Criterios CA-1 y CA-2 de HU-07, CA-1 y CA-4 de HU-09; PresupuestoPreliminarServiceTest, EjecucionPresupuestalServiceTest |
| RNF06 | Control de acceso por roles | Seguridad | RF02 | 0 accesos a funciones no permitidas por el rol | CA-4 de HU-02: acción no autorizada → acceso denegado (403) |

---

# Trazabilidad

Cada RF se asocia a un caso de uso, a una historia de usuario, a su historia en Jira y, desde el Sprint 2, a los pull requests y a la prueba que lo verifica; cada RNF tiene una métrica y una forma de verificación. Los cambios a este documento se registran en el historial de versiones y en los commits del repositorio, mencionando la clave de Jira afectada (por ejemplo, `SCRUM-6`).
