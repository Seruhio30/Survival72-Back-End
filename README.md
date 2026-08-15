# Survival72 Back-End

Backend Spring Boot de Survival72.

## Stack

- Java 17
- Spring Boot 3.3.5
- Spring Data JPA / Hibernate
- MySQL 8
- Flyway 10.10.0
- Maven

## Join persistence

Flyway es la fuente de verdad para la evolución del esquema del nuevo Join.
Hibernate usa `spring.jpa.hibernate.ddl-auto=validate` y no modifica el esquema automáticamente.

Las migraciones están en `survival72/src/main/resources/db/migration/`.

Estado actual:

- esquema histórico incorporado mediante baseline explícito en versión 1;
- modelo canónico aplicado mediante V2;
- `management_token_hash` alineado a `VARCHAR(64)` mediante V3;
- `subscriber_preferences` persiste las preferencias en tabla separada;
- las columnas históricas se conservan para evitar una migración destructiva;
- 5 pruebas de persistencia, 0 fallos, 0 errores.

## Join service foundation

La capa de aplicación del lifecycle de Join ya está implementada.

Incluye:

- creación de nuevas suscripciones `ACTIVE`;
- normalización y validación interna de datos;
- Join duplicado activo idempotente;
- rejoin reutilizando la misma fila;
- actualización de preferencias durante rejoin;
- generación de tokens opacos aleatorios de 32 bytes;
- persistencia exclusiva del hash SHA-256 del token;
- resultados internos diferenciados para `NEW_SUBSCRIPTION`,
  `ACTIVE_DUPLICATE` y `REJOINED`;
- transacciones mediante `JoinService`;
- frontera HTTP inicial implementada mediante `POST /api/join`;
- DTO público de request con Bean Validation;
- respuesta pública neutral `REQUEST_ACCEPTED` para NEW, ACTIVE_DUPLICATE y REJOIN;
- manejo controlado de `400 Bad Request` para payload inválido;
- sin email, management, unsubscribe ni frontend en este bloque.

Validación actual completa:

- 33 pruebas;
- 0 fallos;
- 0 errores;
- `BUILD SUCCESS`.

El contrato detallado se encuentra en `survival72/docs/join-contract.md`.
