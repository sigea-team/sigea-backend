# Documento de Requerimientos – SIGEA (Sprint 1)

**Asignatura:** Análisis y Diseño de Sistemas
**Tablero Jira:** [ufps-team-sigea / SCRUM](https://ufps-team-sigea.atlassian.net/jira/software/projects/SCRUM/boards/1)
**Sprint:** SCRUM Sprint 1 — *Establecer la base de seguridad y planeación de eventos* (15/09/2026 – 05/10/2026)
**Fuentes:** EP2 – Documento de requisitos (SRS), D7 – Descripción de requisitos, EP3 – Casos de uso de alto nivel, HU vs Casos de uso.

---

## Historial de versiones

| Versión | Fecha      | Autor        | Descripción del cambio                                         |
|---------|------------|--------------|----------------------------------------------------------------|
| 1.0     | 2026-10-08 | Equipo SIGEA | Versión inicial: casos de uso, RF y RNF abordados en el Sprint 1 |

---

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

## 5. Trazabilidad

Cada RF se asocia a un caso de uso, a una historia de usuario y a su historia en Jira; cada RNF tiene una métrica y una forma de verificación. Los cambios a este documento se registran en el historial de versiones y en los commits del repositorio, mencionando la clave de Jira afectada (por ejemplo, `SCRUM-6`).
