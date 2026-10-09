# ServiceEvidence

**ServiceEvidence** es una plataforma SaaS B2B para administrar servicios en campo y generar evidencia verificable de su ejecución: órdenes de trabajo, checklists, fotografías, firmas, aprobaciones y comprobantes de servicio.

> **Estado:** Sprint 1 — infraestructura inicial. El flujo comercial descrito abajo es la visión del producto; **todavía no está implementado**.

## Problema que resuelve

Empresas de limpieza, mantenimiento, fumigación y servicios industriales necesitan demostrar qué trabajo realizaron, cuándo, dónde y con qué evidencias. ServiceEvidence busca centralizar esa información y facilitar su revisión por supervisores y clientes.

## Funcionalidades

| Capacidad | Estado |
| --- | --- |
| API REST con Spring Boot y Kotlin | Base inicial |
| PostgreSQL y migraciones Flyway | Implementado (migración `tenants`) |
| Spring Security con rutas protegidas por defecto | Configuración inicial |
| Health check | Implementado |
| Registro de empresas y aislamiento multi-tenant | Pendiente |
| Usuarios, roles y autenticación JWT | Pendiente |
| Clientes, ubicaciones y catálogo de servicios | Pendiente |
| Órdenes de trabajo y asignación | Pendiente |
| Checklists, fotografías y firmas | Pendiente |
| Aprobaciones y comprobantes PDF | Pendiente |
| Frontend administrativo y experiencia móvil | Pendiente |
| Suscripciones y facturación | Pendiente |

## Tecnologías actuales

- **Backend:** Kotlin 2.3.21, Spring Boot 4.1.1, Java 21.
- **Build:** Gradle con **Groovy DSL** (`backend/build.gradle`) y Gradle Wrapper.
- **Datos:** PostgreSQL 17, Spring Data JPA, Flyway.
- **Seguridad:** Spring Security (autenticación JWT aún pendiente).
- **Operación:** Spring Boot Actuator, Docker Compose para PostgreSQL.
- **Pruebas:** JUnit 5 y dependencias de Spring Boot Test.

**Planeado, no integrado:** React + TypeScript para web, almacenamiento compatible con S3 para evidencias, Redis cuando sea necesario y generación de PDF.

## Estructura del repositorio

```text
service-evidence/
├── README.md
├── .gitignore
├── backend/
│   ├── build.gradle
│   ├── gradlew
│   ├── gradlew.bat
│   ├── gradle/wrapper/
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/serviceevidence/backend/
│       │   │   ├── BackendApplication.kt
│       │   │   └── shared/
│       │   │       ├── api/ApiInfoController.kt
│       │   │       └── security/SecurityConfig.kt
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/V1__create_tenants.sql
│       └── test/
└── infrastructure/
    └── docker-compose.yml
```

La arquitectura prevista es un **monolito modular**: módulos de identidad, tenants, clientes, órdenes, evidencias, aprobaciones y reportes, con límites de dominio claros.

## Requisitos

- JDK 21.
- Docker con Docker Compose.
- Puerto **5432** disponible en localhost para PostgreSQL.
- Puerto **8080** disponible para la API.

No necesitas instalar Gradle globalmente: se incluye el Wrapper.

## Inicio rápido

Ejecuta los comandos desde la **raíz del repositorio**, salvo donde se indique lo contrario.

### 1. Iniciar PostgreSQL

```bash
docker compose -f infrastructure/docker-compose.yml up -d
```

Verifica su estado:

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

El contenedor crea la base de datos `serviceevidence` y expone PostgreSQL **solo en `127.0.0.1:5432`**.

### 2. Iniciar el backend

En macOS o Linux:

```bash
cd backend
chmod +x gradlew
./gradlew bootRun
```

En Windows (PowerShell):

```powershell
cd backend
.\gradlew.bat bootRun
```

Por defecto, la API utiliza `http://localhost:8080`. Flyway ejecuta las migraciones al arrancar.

### 3. Comprobar los endpoints

```bash
curl -i http://localhost:8080/actuator/health
```

Respuesta esperada con la aplicación saludable:

```json
{"status":"UP"}
```

```bash
curl -i http://localhost:8080/api/v1/info
```

`/api/v1/info` **requiere autenticación**. Como el Sprint 1 todavía no incluye un mecanismo de login, la solicitud anónima debe ser rechazada (el código HTTP exacto depende de la configuración de Spring Security).

## Configuración

`backend/src/main/resources/application.yml` permite sobrescribir estos valores mediante variables de entorno:

| Variable | Valor local por defecto | Descripción |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/serviceevidence` | URL JDBC del backend |
| `DB_USERNAME` | `serviceevidence` | Usuario PostgreSQL |
| `DB_PASSWORD` | `serviceevidence_dev` | Contraseña de desarrollo |

El servicio de PostgreSQL en Docker Compose también lee `DB_PASSWORD`. Si cambias la contraseña **después de inicializar el volumen**, debes actualizar las credenciales de la base de datos existente: modificar la variable no cambia automáticamente la contraseña almacenada.

> **Seguridad:** los valores anteriores son exclusivamente para desarrollo local. Nunca utilices la contraseña por defecto en producción ni subas secretos al repositorio. La configuración actual no está endurecida para producción.

## Migraciones de base de datos

Las migraciones versionadas se encuentran en:

```text
backend/src/main/resources/db/migration/
```

La migración `V1__create_tenants.sql` crea una tabla inicial `tenants` con `id`, `code`, `name`, `active` y `created_at`. **Aún no existe aislamiento de datos entre empresas**; será parte del módulo de identidad y multi-tenancy.

No modifiques migraciones que ya hayan sido aplicadas en entornos compartidos: crea una nueva versión.

## Pruebas y compilación

Desde `backend/`:

```bash
./gradlew test
./gradlew build
```

Las pruebas que levantan el contexto de Spring pueden requerir PostgreSQL configurado y disponible. Estos comandos se proporcionan para validación local; este README no afirma que hayan sido ejecutados exitosamente.

## Comandos útiles

Desde la raíz del repositorio:

```bash
# Ver logs de PostgreSQL
docker compose -f infrastructure/docker-compose.yml logs -f postgres

# Detener los contenedores sin eliminar los datos
docker compose -f infrastructure/docker-compose.yml down

# ADVERTENCIA: elimina el volumen de datos local
docker compose -f infrastructure/docker-compose.yml down -v
```

## Roadmap

1. **Sprint 1 — Base técnica:** Spring Boot, PostgreSQL, Flyway, seguridad por defecto, health check y Docker Compose.
2. **Sprint 2 — Identity & Multi-Tenancy:** usuarios, roles, autenticación JWT, permisos y aislamiento por tenant.
3. **Sprint 3 — Operaciones:** clientes, ubicaciones, servicios, técnicos y órdenes de trabajo.
4. **Sprint 4 — Evidencias:** checklists, fotografías, firmas y almacenamiento seguro.
5. **Sprint 5 — Aprobaciones:** revisión, correcciones, auditoría y comprobantes PDF.
6. **Sprint 6 — Producto comercial:** frontend administrativo, pruebas integrales, despliegue y pilotos.

## Convenciones de desarrollo

- Código Kotlin organizado por **dominios funcionales**, no únicamente por capas técnicas globales.
- APIs REST versionadas bajo `/api/v1`.
- Migraciones de esquema con Flyway.
- Validación y control de acceso en el backend.
- Comentarios en inglés y español cuando aporten claridad a la implementación.
- Commits con [Conventional Commits](https://www.conventionalcommits.org/).

## Licencia

No se ha definido una licencia pública. Hasta que se añada un archivo `LICENSE`, no se concede una licencia de reutilización del código por el simple hecho de publicar el repositorio.
