# Um.tesoreria.auditoria-service

[![UM.tesoreria.auditoria-service CI](https://github.com/UM-services/UM.tesoreria.auditoria-service/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/UM-services/UM.tesoreria.auditoria-service/actions/workflows/maven.yml)

## Descripción
Servicio de auditoría del sistema de Tesorería de la Universidad de Mendoza. Registra **quién
hizo qué, cuándo, sobre qué entidad y con qué resultado** en los servicios emisores:

| Emisor | Qué aporta | Cómo entrega |
|---|---|---|
| `tesoreria-core` | `REQUEST` (una por operación HTTP), `CHANGE` (diff por entidad JPA) y `SUMMARY/BULK` (procesos masivos) | Outbox transaccional en `tesium` + HTTP |
| `tesoreria-mercadopago` | `REQUEST` HTTP (incluye el webhook de Mercado Pago) y ejecución de los jobs nocturnos | Cola en memoria + HTTP |
| `tesoreria-guarani` | `REQUEST` de **consulta** (`ACCESS`) a datos personales y académicos | Cola en memoria + HTTP |

Versión actual: **1.0.1** (ver [CHANGELOG.md](CHANGELOG.md)).

## Tecnologías
- Java 25
- Spring Boot 4.1.2
- Spring Cloud 2025.1.4 (Consul discovery)
- Spring Boot Actuator
- Spring Kafka (declaración de la DLT de auditoría)
- Maven 3.10.0 (wrapper)
- MySQL Connector/J 26.10.0
- SpringDoc OpenAPI 3.1.1
- Spring Security
- Lombok
- Log4j2
- GitHub Actions (CI/CD con SonarCloud)
- JaCoCo (cobertura de código)

## Requisitos previos
- Java JDK 25 o superior
- Maven 3.8.8 o superior (el wrapper descarga 3.10.0)
- MySQL 8.0 o superior
- Docker (opcional)

## Configuración
La configuración vive en `src/main/resources/bootstrap.yml` (mismo esquema que el resto de
los servicios UM). Todo se puede inyectar por variables de entorno:

| Variable | Default | Descripción |
|---|---|---|
| `APP_PORT` | `8211` | Puerto HTTP |
| `APP_SERVER` | `10.147.20.20:3306` | Host MySQL |
| `APP_DATABASE` | `tesium_aud` | Base de datos de auditoría (separada de `tesium`) |
| `APP_USER` / `APP_PASSWORD` | `root` / `root` | Credenciales MySQL |
| `APP_API_KEY` | *(vacío)* | Clave para la ingesta (`X-API-Key`). Sin ella, la escritura responde 503 |
| `APP_SWAGGER_USER` / `APP_SWAGGER_PASSWORD` | `admin` / `password` | Basic auth de Swagger |
| `APP_CONSUL_HOST` / `APP_CONSUL_PORT` | `consul-service` / `8500` | Consul |
| `APP_KAFKA_BOOTSTRAP` | `kafka:9092` | Bootstrap de Kafka usado para declarar la DLT |
| `APP_AUDIT_DLT_TOPIC` | `tesoreria-audit-evento.DLT` | Nombre de la DLT declarada por el servicio |
| `APP_AUDIT_TOPIC_PARTITIONS` | `3` | Particiones de la DLT (deben coincidir con el topic origen) |
| `APP_KAFKA_TOPIC_REPLICAS` | `1` | Replication factor de la DLT (según el entorno) |
| `APP_LOGGING` | `info` | Nivel de log |

```yaml
app:
  port: ${APP_PORT:8211}
  server: ${APP_SERVER:10.147.20.20:3306}
  database: ${APP_DATABASE:tesium_aud}
  user: ${APP_USER:root}
  password: ${APP_PASSWORD:root}
  api_key: ${APP_API_KEY:}
  swagger_user: ${APP_SWAGGER_USER:admin}
  swagger_password: ${APP_SWAGGER_PASSWORD:password}
  consul:
    host: ${APP_CONSUL_HOST:consul-service}
    port: ${APP_CONSUL_PORT:8500}
  kafka:
    dlt-topic: ${APP_AUDIT_DLT_TOPIC:tesoreria-audit-evento.DLT}
    topic-partitions: ${APP_AUDIT_TOPIC_PARTITIONS:3}
    topics:
      replicas: ${APP_KAFKA_TOPIC_REPLICAS:1}

spring:
  application:
    name: tesoreria-auditoria-service
  kafka:
    bootstrap-servers: ${APP_KAFKA_BOOTSTRAP:kafka:9092}
  cloud:
    consul:
      discovery:
        health-check-path: /actuator/health/readiness
```

### Base de datos

El servicio **no crea ni migra el esquema** (`spring.jpa.hibernate.ddl-auto: none`). El DDL vive
versionado en `sql/` y lo aplica quien despliega:

```bash
mysql -h 10.147.20.20 -u root -p tesium_aud < sql/001_audit_event.sql
```

`tesium_aud` es una base **dedicada** (creada el 2026-10-27). El outbox de `tesoreria-core`,
en cambio, vive en `tesium` porque tiene que compartir transacción y conexión con los cambios
de negocio; `tesium_aud` guarda el evento ya consolidado.

## Instalación y configuración

1. Clonar el repositorio:
```bash
git clone https://github.com/UM-services/UM.tesoreria.auditoria-service.git
```

2. Navegar al directorio del proyecto:
```bash
cd UM.tesoreria.auditoria-service
```

3. Compilar el proyecto:
```bash
mvn clean install
```

## Ejecución

### Local
```bash
mvn spring-boot:run
```

### Docker
```bash
docker build -t um-tesoreria-auditoria .
docker run -p 8211:8211 -e APP_SERVER=10.147.20.20:3306 um-tesoreria-auditoria
```

## Uso
El servicio expone endpoints REST para la gestión de auditorías. La documentación detallada de la API está disponible (basic auth con `APP_SWAGGER_USER` / `APP_SWAGGER_PASSWORD`) en:

```
http://localhost:8211/swagger-ui.html
```

### Endpoints disponibles

Ingesta (requiere `X-API-Key`):

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/api/tesoreria/auditoria/evento/lote` | Lote de hasta 500 eventos; idempotente por `eventId` |
| `POST` | `/api/tesoreria/auditoria/evento/` | Un evento |

Consulta (red interna; JWT pendiente):

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/api/tesoreria/auditoria/evento/{eventId}` | Detalle |
| `GET` | `/api/tesoreria/auditoria/evento/correlacion/{correlationId}` | Todos los cambios de una operación |
| `GET` | `/api/tesoreria/auditoria/evento/entidad/{entity}/{entityId}` | Últimos 100 cambios de una entidad |
| `GET` | `/api/tesoreria/auditoria/evento/actor/{actorId}` | Últimos 100 eventos de un usuario |
| `GET` | `/api/tesoreria/auditoria/evento/periodo/{anho}/{mes}` | Eventos del período (hasta 200, orden descendente) |
| `GET` | `/api/tesoreria/auditoria/evento/servicio/{service}/periodo/{anho}/{mes}` | Ídem, acotado a un emisor |
| `POST` | `/api/tesoreria/auditoria/evento/search` | Búsqueda compuesta por body |

### Niveles y operaciones

| `level` | Cuándo | Contenido |
|---|---|---|
| `REQUEST` | Una fila por operación HTTP (o job) de un emisor, incluso si falla | actor, IP, endpoint, `outcome`, `affectedRows` |
| `CHANGE` | Un evento por cambio de entidad JPA (sólo `tesoreria-core`) | `entity`, `entityId`, `changedFields`, `beforeData`/`afterData` enmascarados |
| `SUMMARY` | Operación masiva resumida por el emisor | `entity`, `affectedRows` y `metadata.counts` (bajas/altas), período, filtro y `bulkId` |

`operation` admite `INSERT`, `UPDATE`, `DELETE`, `ACCESS` y `BULK`. Para `operation=BULK` el
servicio exige `affectedRows > 0`; para `CHANGE` y `SUMMARY`, `entity` no puede ser vacío. El
buscador compuesto (`POST /search`) filtra por `level`, `operation`, `outcome`, `service`,
`actorId`, `entity`, `entityId`, `correlationId` y rango `from`/`to`, con paginación
(`page`, `size` hasta 200).

### Catálogo de eventos por emisor

| Emisor | `level` / `operation` | `entity` | Cuándo |
|---|---|---|---|
| `tesoreria-core` | `REQUEST` | — | Cada operación HTTP que muta |
| `tesoreria-core` | `CHANGE` / `INSERT`·`UPDATE`·`DELETE` | entidad JPA | Cada `save`/`delete` vía Hibernate |
| `tesoreria-core` | `SUMMARY` / `BULK` | entidad | Proceso masivo resumido |
| `tesoreria-mercadopago` | `CHANGE` / `INSERT`·`UPDATE` | `MercadoPagoPreference` | Alta o cambio de preferencia (también con `outcome=ERROR` si falla el SDK) |
| `tesoreria-mercadopago` | `REQUEST` / `ACCESS` | `MercadoPagoNotification` | **Toda** notificación recibida (webhook, IPN legacy, replay manual), incluso rechazada |
| `tesoreria-mercadopago` | `REQUEST` / `ACCESS` | — | HTTP y ejecución de jobs nocturnos |
| `tesoreria-guarani` | `REQUEST` / `ACCESS` | — | Cada consulta |
| `tesoreria-guarani` | `CHANGE` / `INSERT`·`UPDATE`·`DELETE` | entidad JPA | Escrituras (cuando empiecen a existir) |

### Enmascaramiento
`tesoreria-core` enmascara por nombre de campo (`tesoreria.audit.mask-fields`):
`password`, `clave`, `cbu`, `cuenta`, `documento`, `token`, `tarjeta`. Los valores quedan como
`****` (credenciales) o `****` + últimos 4 (identificadores y cuentas). Los payloads que superan
`max-payload-chars` se descartan y se marcan con `metadata.truncated=true`.

## Integración en el stack
- Se registra en Consul como `tesoreria-auditoria-service` (tags `tesoreria,auditoria`) y expone
  `GET /actuator/health/readiness` para el health check.
- Declara la DLT `tesoreria-audit-evento.DLT` (3 particiones, retención de 14 días). El topic
  principal `tesoreria-audit-evento` lo declara `tesoreria-mercadopago`; el consumo en este
  servicio todavía no está implementado.
- Si la DLT ya existe, el servicio actualiza sus configs
  (`spring.kafka.admin.modify-topic-configs: true`). El arranque no falla si el broker no está
  disponible (`fail-fast` desactivado por defecto): la ingesta REST sigue funcionando.
- El gateway de tesorería publica la ruta `/api/tesoreria/auditoria/**` → `lb://tesoreria-auditoria-service`.
- En el compose local corre en el puerto `8211`.
- Los emisores auditan por defecto (`TESORERIA_AUDIT_MODE=SEND`) y comparten
  `TESORERIA_AUDIT_API_KEY`.

### Activación en los emisores

La auditoría está encendida desde el arranque: `SEND` es el default en los tres servicios y en
el compose. Variables relevantes:

| Variable | Default | Efecto |
|---|---|---|
| `TESORERIA_AUDIT_MODE` | `SEND` | `SEND` captura y envía · `LOG` captura y sólo loguea · `OFF` apaga |
| `TESORERIA_AUDIT_URL` | `http://localhost:8211` | En compose: `http://tesoreria-auditoria-service:8211` |
| `TESORERIA_AUDIT_API_KEY` | *(vacío)* | Debe coincidir con `APP_API_KEY` de este servicio |

Si la API key no está configurada, el emisor no inventa nada: `tesoreria-core` deja los eventos
`PENDING` en `tesium.audit_outbox` (no se pierden) y `mercadopago`/`guarani` llenan su cola y
empiezan a descartar. En el compose la clave sale de `TESORERIA_AUDIT_API_KEY`
(default local: `tesoreria-audit-dev`), así que funciona sin configuración adicional.

## Estructura del proyecto
```
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── um/tesoreria/aud/rest/
│   │   │       ├── configuration/
│   │   │       │   ├── AuditApiKeyFilter.java
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── KafkaTopicsConfiguration.java
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       ├── model/
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── resources/
│   │       ├── banner.txt
│   │       └── bootstrap.yml
│   └── test/
├── docs/
│   └── diagrams/
├── sql/
│   └── 001_audit_event.sql
├── .github/
│   └── workflows/
│       ├── deploy-develop.yml
│       ├── deploy-staging.yml
│       ├── generate-docs.yml
│       └── maven.yml
├── .dockerignore
├── .gitignore
├── Dockerfile
├── pom.xml
├── README.md
└── CHANGELOG.md
```

## Logging
El servicio usa Log4j2 (`spring-boot-starter-log4j2`, con `spring-boot-starter-logging` excluido)
y el patrón/destino por defecto de Spring Boot (consola). El nivel se controla con `APP_LOGGING`
(`logging.level.um.tesoreria.aud`, default `info`). El repositorio no incluye un `log4j2.xml`:
para escribir a archivo o rotar por tamaño hay que agregar esa configuración.

## Contribución
1. Fork el proyecto
2. Cree una rama para su feature (`git checkout -b feature/AmazingFeature`)
3. Commit sus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abra un Pull Request

## Contacto
Universidad de Mendoza - [https://um.edu.ar](https://um.edu.ar)

## Licencia
Este proyecto está bajo la Licencia MIT - vea el archivo [LICENSE.md](LICENSE.md) para más detalles.

## Autor
✍️ Desarrollado por Daniel Quinteros
