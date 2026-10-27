# Changelog

Todos los cambios notables en este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-27

### Agregado
- feat: modelo de auditoría estructurada `audit_event` (append-only). DDL versionado en `sql/001_audit_event.sql` (sin Flyway) sobre la base dedicada `tesium_aud`.
- feat: ingesta por lote (`POST /api/tesoreria/auditoria/evento/lote`, hasta 500 eventos) idempotente por `eventId`.
- feat: ingesta unitaria (`POST /api/tesoreria/auditoria/evento/`) con respuesta `202 Accepted`.
- feat: API de consulta por evento, correlación, entidad, actor, período, período por servicio y búsqueda compuesta (`POST /api/tesoreria/auditoria/evento/search`).
- feat: niveles `REQUEST`, `CHANGE` y `SUMMARY`; validación de `operation=BULK` (`affectedRows > 0` obligatorio, `entity` obligatorio para `CHANGE` y `SUMMARY`).
- feat: autenticación de la ingesta con `X-API-Key` (`APP_API_KEY`); la escritura responde `503` si la clave no está configurada y `401` si falta o no coincide.
- feat: validación de payloads, hashing canónico SHA-256 (`payload_hash`) y manejo de errores con `ProblemDetail`.
- feat: integración con Spring Cloud Consul (registro como `tesoreria-auditoria-service`, readiness en `/actuator/health/readiness`).
- feat: Spring Boot Actuator para health checks.
- feat: ruta pública `/api/tesoreria/auditoria/**` publicada por el gateway de tesorería.
- feat: workflows de CI (`maven.yml`, SonarCloud), despliegue para `develop` y `staging`, y generación de documentación.
- build: `bootstrap.yml` con host/base/credenciales inyectables por variables `APP_*`.
- build: puerto por defecto 8211 (8210 lo ocupa `haberes-auditoria-service`).
- build: `.dockerignore` para excluir `target/`, `.git/`, `graphify-out/` y archivos de IDE del contexto Docker.
- build: `spring-boot-starter-kafka` y declaración de la DLT `tesoreria-audit-evento.DLT` (3 particiones, retención de 14 días y `min.insync.replicas` acotado al replication factor ambiental `APP_KAFKA_TOPIC_REPLICAS`). El topic principal `tesoreria-audit-evento` lo declara `tesoreria-mercadopago`.
- test: datasource H2, Consul deshabilitado y `spring-boot-starter-webmvc-test` en el contexto de test; 11 tests de ingesta idempotente, validaciones, consultas y DLT.

### Notas
- La base `tesium_aud` es independiente de `tesium`. El outbox transaccional de `tesoreria-core` vive en `tesium` porque debe compartir la conexión con el cambio de negocio.
- `tesoreria-mercadopago` y `tesoreria-guarani` no tienen outbox: entregan desde una cola acotada en memoria. Ver `PLANIFICACION_AUDITORIA_TESORERIA.md` en el workspace.
- Kafka: el servicio todavía no consume `tesoreria-audit-evento`; por ahora sólo declara la DLT que usará el consumidor cuando se incorpore.
