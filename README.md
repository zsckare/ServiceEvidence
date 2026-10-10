# ServiceEvidence

**Plataforma SaaS multiempresa para administrar servicios de campo, órdenes de trabajo y evidencias de ejecución.**

ServiceEvidence centraliza clientes, ubicaciones, técnicos, órdenes de trabajo, fotografías, firmas, reportes y administración de suscripciones. Está diseñado para empresas que necesitan documentar el trabajo realizado en campo y conservar trazabilidad de sus operaciones.

> **Estado:** producto en desarrollo. El núcleo operativo y administrativo está implementado; todavía no se considera una versión de producción auditada. La facturación recurrente y la pasarela de pagos no están implementadas.

## Índice

- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos](#requisitos)
- [Configuración](#configuración)
- [Ejecución local](#ejecución-local)
- [Docker](#docker)
- [Usuarios y acceso](#usuarios-y-acceso)
- [API](#api)
- [Pruebas](#pruebas)
- [Seguridad y datos](#seguridad-y-datos)
- [Suscripciones](#suscripciones)
- [Despliegue](#despliegue)
- [Estado y próximos pasos](#estado-y-próximos-pasos)

## Funcionalidades

### Operación de campo

- Gestión de clientes y ubicaciones de servicio.
- Creación, consulta, edición y seguimiento de órdenes de trabajo.
- Asignación y desasignación de técnicos.
- Vista de órdenes asignadas al técnico.
- Cambios de estado e historial de eventos por orden.
- Registro de evidencias, archivos e imágenes asociadas a las órdenes.
- Reportes PDF de servicio mediante JasperReports.
- Consultas de resumen y exportación de información operativa desde la interfaz.

### Administración empresarial

- Gestión de usuarios y acceso por contexto de empresa (*tenant*).
- Configuración de la empresa y logotipo de reportes.
- Notificaciones y marcación de lectura.
- Consulta de auditoría de seguridad.
- Panel operativo con métricas y resúmenes.

### Administración de la plataforma

- Inicio de sesión global y selección de empresa.
- Administración de tenants por un usuario `SUPER_ADMIN`.
- Catálogo de planes y asignación de suscripciones.
- Consulta de uso y límites por empresa.
- Separación entre la consola global y el espacio de trabajo de cada empresa.

## Arquitectura

```text
Navegador (React + TypeScript)
          |
          | HTTP / JSON + JWT
          v
API REST (Kotlin + Spring Boot)
          |
          +-- Autenticación, autorización y contexto de tenant
          +-- Clientes, ubicaciones, órdenes y técnicos
          +-- Evidencias, notificaciones y reportes
          +-- Suscripciones y cuotas
          |
          +---- PostgreSQL 17 (datos relacionales)
          |
          +---- Almacenamiento privado de evidencias (volumen/directorio)
```

El backend usa Spring Data JPA para persistencia, Flyway para versionado de esquema y Spring Security para protección de rutas. La aplicación web consume la API y ofrece interfaces diferenciadas para operación, administración de empresa y administración global.

**Multi-tenancy:** las operaciones dependientes de una empresa utilizan un contexto de tenant. El aislamiento de datos es una propiedad crítica del sistema; las pruebas actuales cubren algunos conteos de suscripción, pero todavía faltan pruebas HTTP completas de acceso cruzado entre empresas.

## Tecnologías

| Capa | Stack |
| --- | --- |
| Frontend | React 19, TypeScript, Vite |
| UI | Tailwind CSS 4, componentes basados en Untitled UI React FREE |
| Estado y datos | TanStack Query, React Router |
| Backend | Kotlin 2.3.21, Spring Boot 4.1.1, Java 21 |
| Persistencia | PostgreSQL 17, Spring Data JPA, Hibernate |
| Migraciones | Flyway |
| Seguridad | Spring Security, JWT, OAuth2 Resource Server |
| PDF | JasperReports 6.16.0 |
| Build | Gradle 9.7.1, npm |
| Infraestructura local | Docker y Docker Compose |
| Pruebas | JUnit 5, Spring Boot Test, Playwright |

## Estructura del repositorio

```text
service-evidence/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/serviceevidence/backend/
│   │   │   │   ├── identity/
│   │   │   │   ├── tenants/
│   │   │   │   ├── subscriptions/
│   │   │   │   ├── customers/
│   │   │   │   ├── locations/
│   │   │   │   ├── workorders/
│   │   │   │   ├── evidence/
│   │   │   │   ├── notifications/
│   │   │   │   ├── reports/
│   │   │   │   ├── settings/
│   │   │   │   └── shared/
│   │   │   └── resources/
│   │   │       ├── db/migration/
│   │   │       └── reports/
│   │   └── test/
│   ├── build.gradle
│   ├── compose.yaml
│   ├── Dockerfile
│   └── gradlew
├── web/
│   ├── src/
│   │   ├── features/
│   │   ├── components/
│   │   ├── styles/
│   │   └── api.ts
│   ├── e2e/
│   └── package.json
└── README.md
```

La estructura representa los directorios funcionales del proyecto. Algunos archivos de pruebas o configuración pueden variar según la revisión instalada.

## Requisitos

- **JDK 21**.
- **Node.js** compatible con la versión de Vite declarada en `web/package.json` y **npm**.
- **PostgreSQL 17** para ejecución local sin Compose.
- **Docker** con soporte de Compose para ejecución en contenedores.
- Puertos disponibles: `8080` (API), `5432` (PostgreSQL local, si aplica) y el puerto mostrado por Vite (habitualmente `5173`).

## Configuración

El backend lee variables de entorno y, mediante su configuración Spring, también puede importar opcionalmente un archivo `.env` situado en la **raíz del repositorio** (`../.env` cuando se ejecuta desde `backend/`).

Ejemplo **para desarrollo local**, sin credenciales reales:

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/serviceevidence
DB_USERNAME=serviceevidence
DB_PASSWORD=CAMBIAR_PASSWORD_LOCAL
JWT_SECRET=REEMPLAZAR_CON_SECRETO_LARGO_ALEATORIO
BOOTSTRAP_ADMIN_EMAIL=admin@example.local
BOOTSTRAP_ADMIN_PASSWORD=REEMPLAZAR_CON_PASSWORD_DE_16_O_MAS_CARACTERES
SERVICEEVIDENCE_STORAGE_PATH=./private-evidence
```

| Variable | Propósito |
| --- | --- |
| `DB_URL` | URL JDBC de PostgreSQL |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |
| `JWT_SECRET` | Secreto de firma JWT; obligatorio |
| `BOOTSTRAP_ADMIN_EMAIL` | Correo del administrador global inicial (opcional) |
| `BOOTSTRAP_ADMIN_PASSWORD` | Contraseña inicial, mínimo 16 caracteres (opcional) |
| `SERVICEEVIDENCE_STORAGE_PATH` | Ruta privada de archivos y evidencias |

**Importante:** no subir `.env`, contraseñas, tokens, bases de datos, volúmenes o evidencias privadas al repositorio. Utilizar secretos diferentes por entorno.

### Base de datos local

Crear un usuario y base PostgreSQL con los valores elegidos en `.env`, o utilizar el servicio `db` de Docker Compose. Las migraciones Flyway se ejecutan durante el arranque del backend y Hibernate valida el esquema (`ddl-auto: validate`).

### Configuración del frontend

La URL base de la API depende de la configuración real del cliente HTTP y de Vite en la versión de `web/` instalada. Revisar `web/src/api.ts`, `web/vite.config.*` y cualquier `.env.example` existente antes de cambiarla. No colocar secretos JWT ni contraseñas de base de datos en variables públicas `VITE_*`.

## Ejecución local

### 1. Backend

```bash
cd backend
./gradlew bootRun
```

La API utiliza normalmente `http://localhost:8080`.

Para compilar:

```bash
cd backend
./gradlew build
```

### 2. Frontend

En otra terminal:

```bash
cd web
npm install
npm run dev
```

Abrir la URL que Vite muestre en consola. Para generar la versión de distribución:

```bash
npm run build
```

### 3. Primer acceso

1. Configurar `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD` **antes del primer arranque** si se desea crear el administrador global.
2. Iniciar el backend con PostgreSQL disponible.
3. Iniciar el frontend y abrir `/login`.
4. Acceder como administrador, crear o habilitar una empresa y seleccionar su contexto.
5. Configurar usuarios, clientes y órdenes de trabajo.

El bootstrap solo crea el administrador global si todavía no existe uno. No hay credenciales predeterminadas públicas.

## Docker

El archivo `backend/compose.yaml` define:

- `db`: PostgreSQL 17 Alpine, con volumen persistente `pgdata`.
- `api`: aplicación Spring Boot, expuesta en `localhost:8080`.
- `evidence`: volumen persistente para evidencias privadas.

Desde `backend/`, exportar las variables requeridas o crear un archivo de entorno apropiado para Compose:

```bash
export DB_PASSWORD='CAMBIAR_PASSWORD'
export JWT_SECRET='SECRETO_LARGO_ALEATORIO'
export BOOTSTRAP_ADMIN_EMAIL='admin@example.local'
export BOOTSTRAP_ADMIN_PASSWORD='PASSWORD_SEGURO_DE_16_O_MAS'

docker compose up --build -d
docker compose ps
docker compose logs -f api
```

Para detener:

```bash
docker compose down
```

`docker compose down` no elimina los volúmenes por defecto. **No utilizar `docker compose down -v` si se necesitan conservar la base y las evidencias.**

> El Compose inspeccionado contiene **API + PostgreSQL**, no el frontend. Para producción se necesita publicar también `web/` mediante hosting estático o un servidor web apropiado y configurar el acceso seguro a la API.

## Usuarios y acceso

La plataforma distingue dos contextos principales:

- **Global (`SUPER_ADMIN`)**: administración de empresas y suscripciones a nivel plataforma.
- **Tenant**: acceso a datos y operaciones de la empresa seleccionada, sujeto a permisos y rol.

El proceso de autenticación incluye login, refresh, logout, consulta de sesión, listado de empresas disponibles y selección de contexto de tenant. Los tokens de acceso tienen una duración configurada de **15 minutos** y los de renovación de **14 días** según `application.yml`.

Rutas web principales:

| Ruta | Descripción |
| --- | --- |
| `/` | Landing pública |
| `/login` | Inicio de sesión |
| `/select-tenant` | Selección de empresa/contexto |
| `/platform` | Consola de administración global |
| `/app/*` | Aplicación de la empresa |

## API

Prefijo principal: **`/api/v1`**. Las rutas protegidas requieren autenticación y, según el recurso, contexto de tenant y permisos.

| Área | Endpoints principales |
| --- | --- |
| Información | `GET /info` |
| Autenticación | `POST /auth/login`, `/auth/refresh`, `/auth/logout`; `GET /auth/me`, `/auth/available-tenants`; `POST /auth/tenant-context` |
| Empresas (plataforma) | `GET/POST /platform/tenants`, `PATCH /platform/tenants/{id}/status` |
| Usuarios | `GET/POST /users`, `PATCH /users/{userId}` |
| Clientes | `GET/POST /customers`, `GET/PUT/DELETE /customers/{id}` |
| Ubicaciones | `GET/POST /customers/{customerId}/locations`, operaciones por `{id}` |
| Órdenes | `GET/POST /work-orders`, `GET/PUT /work-orders/{id}`, `GET /work-orders/mine` |
| Seguimiento | `POST /work-orders/{id}/status`, asignaciones y eventos por orden |
| Evidencias | `/work-orders/{workOrderId}/evidence` y `/work-orders/{workOrderId}/files` |
| Reportes | `GET /reports/work-orders/summary`, `GET /work-orders/{workOrderId}/report.pdf` |
| Notificaciones | `GET /notifications`, `POST /notifications/{id}/read` |
| Configuración | `GET/PUT /settings`, endpoints de `/settings/report-logo` |
| Suscripciones | `GET /subscriptions/plans`, `GET /subscriptions/mine`, `GET /subscriptions/usage/mine` |
| Suscripciones globales | `/subscriptions/platform/tenants/{tenantId}` y `/subscriptions/platform/tenants/{tenantId}/usage` |
| Auditoría | `GET /security/audit` |

Esta tabla es una guía de rutas, no un contrato OpenAPI completo. Consultar los controladores para DTO, validaciones, paginación, códigos HTTP y reglas de autorización específicos.

La salud de la aplicación está disponible mediante Spring Boot Actuator, normalmente en `GET /actuator/health`.

## Pruebas

### Backend: pruebas unitarias

```bash
cd backend
./gradlew test
```

### Backend: integración PostgreSQL

Se incorporó la clase `SubscriptionQuotaPostgresIT.kt`, que verifica:

1. El conteo de usuarios activos para el tenant seleccionado.
2. El conteo mensual de órdenes para el tenant seleccionado.
3. El rechazo de un tenant inexistente.

**Resultado reportado el 10 de octubre de 2026: 3/3 pruebas aprobadas** en PostgreSQL, mediante la tarea `postgresIntegrationTest`.

```bash
cd backend

export DB_URL='jdbc:postgresql://localhost:5433/serviceevidence_test'
export DB_USERNAME='serviceevidence'
export DB_PASSWORD='PASSWORD_DE_PRUEBAS'
export JWT_SECRET='SECRETO_EXCLUSIVO_PARA_PRUEBAS'

./gradlew postgresIntegrationTest --rerun-tasks
```

La tarea personalizada debe existir en `backend/build.gradle` y reutilizar `sourceSets.test.output.classesDirs`, `sourceSets.test.runtimeClasspath`, depender de `testClasses` y filtrar `**/*PostgresIT.class`. **Si aparece `NO-SOURCE` o `NO-TESTS`, las pruebas no se ejecutaron realmente.**

Usar siempre una base independiente cuyo nombre termine en `_test`. La ejecución no debe apuntar a datos de desarrollo o producción.

### Frontend

```bash
cd web
npm run build
npx playwright test --config playwright.config.ts
```

**Resultado reportado:** 9/9 pruebas E2E de rutas públicas y protección de acceso aprobadas. Para ejecutarlas localmente, verificar la configuración de servidor de Playwright en `web/playwright.config.ts`.

### Reportes de pruebas

- Gradle: `backend/build/reports/tests/`.
- Resultados JUnit XML: `backend/build/test-results/`.
- Playwright: según `web/playwright.config.ts`.

## Seguridad y datos

- JWT para autenticación de la API.
- Autorización basada en roles y contexto de tenant.
- Archivos de evidencia almacenados fuera de los recursos públicos de la web.
- Registro de eventos operativos y auditoría de seguridad.
- Validación de datos de entrada en endpoints.
- Migraciones de esquema versionadas con Flyway.

**Antes de producción** se deben revisar, como mínimo, el aislamiento HTTP multi-tenant, permisos por rol, límites de carga de archivos, manejo de secretos, CORS, TLS, copias de seguridad, restauración, retención de evidencias, observabilidad y pruebas de carga/concurrencia.

Los límites de carga configurados en el backend son **20 MB por archivo** y **21 MB por solicitud**.

## Suscripciones

Existe un catálogo de planes `STARTER`, `GROWTH` y `BUSINESS` y mecanismos de consulta y aplicación de cuotas por empresa. La lógica contempla límites de usuarios activos y órdenes mensuales, así como consulta de consumo.

Los valores concretos de límites y precios deben consultarse en la migración `V7__subscription_catalog.sql` y en los datos de la plataforma; no deben asumirse a partir de este README.

**No implementado:** integración con pasarela de pagos, cobro recurrente, facturación automática ni gestión completa del ciclo de cobros.

## Despliegue

Para un entorno productivo se recomienda:

1. Configurar PostgreSQL persistente con respaldo y restauración probada.
2. Inyectar secretos mediante un gestor seguro de configuración.
3. Ejecutar migraciones Flyway antes de recibir tráfico.
4. Publicar la API detrás de HTTPS y un proxy inverso.
5. Publicar el build estático de `web/` y configurar la URL de API.
6. Proteger el almacenamiento privado de evidencias y sus copias de seguridad.
7. Activar monitoreo, logs centralizados y alertas.
8. Ejecutar pruebas unitarias, integración y E2E en CI antes de cada despliegue.

La presencia de Docker Compose **no implica** que la aplicación esté preparada automáticamente para producción.

## Estado y próximos pasos

### Implementado y validado parcialmente

- Backend multi-tenant, autenticación y administración de empresas.
- Gestión de clientes, ubicaciones, órdenes, asignaciones y evidencias.
- PDF de servicios, reportes, notificaciones y configuración.
- Catálogo de suscripciones y validación de cuotas.
- Frontend público, empresarial y de administración global.
- Build frontend, pruebas unitarias backend y pruebas PostgreSQL de cuotas.

### Pendiente de completar o verificar

- Pruebas de autorización HTTP entre tenants y roles.
- Pruebas de concurrencia sobre límites de suscripción.
- Verificación completa del pipeline CI en GitHub Actions.
- Endurecimiento de seguridad y operación para producción.
- Pasarela de pagos y facturación recurrente.
- Documentación formal de API (OpenAPI), si se decide incorporarla.

## Desarrollo y contribuciones

Antes de enviar cambios:

```bash
# Backend
cd backend
./gradlew test

# Frontend (en otra terminal)
cd web
npm run build
```

Mantener cambios por módulo, incluir migraciones cuando se modifique el esquema, añadir pruebas para lógica sensible y evitar subir datos o credenciales. Usar mensajes de commit descriptivos, por ejemplo:

```text
feat(workorders): add assignment workflow
test(subscriptions): cover PostgreSQL quota isolation
docs: expand project README
```

---

**ServiceEvidence** — Gestión de servicios de campo con evidencia, trazabilidad y control multiempresa.
