# Reglas de Proyecto - SIGEA Backend

Este archivo establece las directrices permanentes para cualquier agente de IA que trabaje en el repositorio `demosigea-backend`.

---

## 📌 REGLA DE ORO: Referencia Obligatoria de Base de Datos

Cada vez que se solicite implementar, modificar, consultar o refactorizar cualquier funcionalidad que interactúe directa o indirectamente con la base de datos (entidades JPA, repositorios Spring Data, servicios de negocio, DTOs, validaciones `@Valid`, controladores REST, migraciones Liquibase, consultas JPQL/SQL nativo o tests de integración):

> **EL AGENTE DEBE CONSULTAR OBLIGATORIAMENTE EL ESQUEMA CANÓNICO EN:**
> 1. **Diccionario de datos completo**: [`docs/database/database-schema.md`](docs/database/database-schema.md)
> 2. **DDL ejecutable PostgreSQL**: [`docs/database/schema.sql`](docs/database/schema.sql) (también disponible en [`src/main/resources/db/schema.sql`](src/main/resources/db/schema.sql))
> 3. **Catálogo estructurado JSON**: [`docs/database/schema.json`](docs/database/schema.json)

**Bajo ninguna circunstancia se deben inventar nombres de tablas, nombres de columnas, tipos de datos, valores de enumeraciones o relaciones foráneas.** Todas las implementaciones deben corresponder exactamente con las especificaciones del esquema.

---

## 🗺️ Mapa de Módulos y Tablas del Sistema (42 Tablas)

### 1. Seguridad, Usuarios y Control de Acceso
- `personas` ➡️ Datos personales y de contacto.
- `usuarios` ➡️ Cuentas del sistema vinculadas a persona. Estados: `'activo'`, `'bloqueado'`, `'inactivo'`.
- `roles` ➡️ Roles del sistema (RBAC).
- `permisos` ➡️ Permisos atómicos por módulo.
- `roles_permisos` ➡️ Relación N:M entre roles y permisos.
- `usuarios_roles` ➡️ Relación N:M entre usuarios y roles.
- `tokens_recuperacion` ➡️ Tokens temporales. Tipos: `'verificacion'`, `'recuperacion'`.
- `auditoria` ➡️ Trazabilidad de operaciones del sistema.

### 2. Gestión de Eventos y Organización
- `eventos` ➡️ Eventos académicos. Modalidad: `'presencial'`, `'virtual'`, `'hibrida'`. Estados: `'en_configuracion'`, `'habilitado'`, `'en_ejecucion'`, `'cerrado'`.
- `comite_organizador` ➡️ Miembros organizadores y sus roles en el evento.
- `tipos_actividad` ➡️ Tipos de sesiones (conferencia, taller, etc.).
- `lineas_tematicas` ➡️ Ejes temáticos del evento.

### 3. Presupuesto y Finanzas
- `rubros_presupuestales` ➡️ Planificación y costeo proyectado.
- `presupuestos_aprobados` ➡️ Aprobación formal y evidencia de presupuesto.
- `gastos_ejecutados` ➡️ Ejecución de gastos con soportes contra rubros.

### 4. Convocatorias, Ponentes y Propuestas
- `convocatorias` ➡️ Llamados a presentación de trabajos. Estados: `'borrador'`, `'publicada'`, `'cerrada'`.
- `participaciones_conferencista` ➡️ Invitación a expositores (`'conferencista'`, `'ponente'`).
- `propuestas` ➡️ Trabajos académicos postulados. Estados: `'recibida'`, `'en_evaluacion'`, `'aprobada'`, `'ajustes'`, `'rechazada'`.
- `propuestas_autores` ➡️ Autores y coautores con orden de autoría.
- `versiones_propuesta` ➡️ Historial de versiones y archivos de la propuesta.

### 5. Sistema de Evaluación por Pares
- `rubricas_evaluacion` ➡️ Rúbricas por convocatoria.
- `criterios_rubrica` ➡️ Criterios y pesos porcentuales.
- `comites_evaluadores` ➡️ Pares evaluadores asignados a la convocatoria.
- `asignaciones_evaluacion` ➡️ Asignación de propuesta a evaluador. Estados: `'pendiente'`, `'en_proceso'`, `'completada'`.
- `evaluaciones` ➡️ Calificación detallada por criterio.
- `resultados_evaluacion` ➡️ Consolidado y clasificación final (`'aprobada'`, `'aprobada_con_ajustes'`, `'rechazada'`).
- `observaciones_propuesta` ➡️ Retroalimentación y correcciones solicitadas a versiones.

### 6. Espacios Físicos, Virtuales y Disponibilidad
- `salas` ➡️ Espacios físicos o virtuales (`'fisica'`, `'virtual'`) con aforo máximo.
- `disponibilidad_salas` ➡️ Franjas horarias disponibles de salas.
- `disponibilidad_ponentes` ➡️ Franjas horarias disponibles de conferencistas.

### 7. Programación y Agenda del Evento
- `actividades` ➡️ Cronograma de actividades (fecha, hora inicio/fin, sala, ponente). Modalidad: `'presencial'`, `'virtual'`, `'hibrida'`.
- `agendas_publicadas` ➡️ Publicación de la agenda diaria (`'no_publicada'`, `'publicada'`).

### 8. Inscripciones, Acreditación y Asistencia
- `inscripciones_evento` ➡️ Inscripción a evento por categoría (`'estudiante'`, `'egresado'`, `'docente'`, `'particular'`).
- `inscripciones_actividad` ➡️ Inscripción a actividades con cupo limitado.
- `codigos_qr` ➡️ Código QR único para acreditación.
- `asistencias` ➡️ Registro de asistencia (`'qr'`, `'manual'`).

### 9. Encuestas, Certificados, Métricas y Memorias
- `encuestas` ➡️ Instrumentos de satisfacción para eventos o actividades.
- `preguntas_encuesta` ➡️ Preguntas (`'escala'`, `'texto'`, `'opcion'`).
- `respuestas_encuesta` ➡️ Respuestas individuales por participante.
- `certificados` ➡️ Certificados emitidos (`'asistente'`, `'ponente'`, `'evaluador'`, `'organizador'`).
- `indicadores` ➡️ Indicadores de gestión y métricas.
- `memorias_historicas` ➡️ Memorias finales y actas de consolidación del evento.

---

## ☕ Convenciones de Código Backend (Java 21 / Spring Boot)

1. **Entidades JPA**:
   - Paquete: `com.sigea.demosigea_backend.model`
   - Clases en singular y PascalCase: ej. `Persona`, `Usuario`, `Evento`, `Propuesta`, `Actividad`.
   - Anotación `@Table(name = "nombre_tabla_snake_case")`.
   - Nombres de campos en Java en camelCase: ej. `tipoDocumento`, `numeroDocumento`, `fechaRegistro`.
   - Anotación `@Column(name = "columna_snake_case")`.
   - Claves foráneas modeladas con `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "fk_id")`.

2. **Tipos de Datos**:
   - `SERIAL / INT` ➡️ `Long` para claves primarias autonuméricas, `Integer` para enteros normales.
   - `VARCHAR(n) / TEXT` ➡️ `String`.
   - `NUMERIC(p, s)` ➡️ `java.math.BigDecimal`.
   - `BOOLEAN` ➡️ `Boolean`.
   - `DATE` ➡️ `java.time.LocalDate`.
   - `TIME` ➡️ `java.time.LocalTime`.
   - `TIMESTAMP` ➡️ `java.time.LocalDateTime`.

3. **Enums Java**:
   - Crear enums en Java para todas las columnas que tengan restricciones `CHECK (columna IN (...))` utilizando `@Enumerated(EnumType.STRING)` en la entidad.
   - Ejemplos: `EstadoUsuario`, `ModalidadEvento`, `EstadoEvento`, `EstadoConvocatoria`, `EstadoPropuesta`, etc.

4. **Repositorios Spring Data**:
   - Paquete: `com.sigea.demosigea_backend.repository`
   - Extender de `JpaRepository<Entidad, ID>`.

5. **DTOs y Mapeo**:
   - Paquete: `com.sigea.demosigea_backend.dto`
   - Usar `records` de Java para DTOs inmutables de petición y respuesta, con validaciones Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, etc.).
