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
- capa interna de gestión de suscripciones implementada mediante management token;
- resolución exclusiva mediante SHA-256 de `managementTokenHash`;
- lectura interna mediante `SubscriptionManagementView`, sin exponer entidad, ID,
  timestamps ni credenciales;
- actualización interna limitada a `firstName`, `countryCode` y `preferences`;
- acceso de gestión permitido únicamente para suscripciones `ACTIVE`;
- `GET /api/subscriptions/manage` y `PATCH /api/subscriptions/manage` ya están
  expuestos mediante `Authorization: Bearer <management-token>`;
- GET/PATCH devuelven únicamente `firstName`, `countryCode` y `preferences`;
- acceso inválido, desconocido, revocado o no gestionable devuelve `404 Not Found`
  neutral con código `SUBSCRIPTION_ACCESS_NOT_FOUND`;
- payload PATCH inválido devuelve `400 Bad Request`;
- lifecycle interno de unsubscribe implementado mediante management token;
- unsubscribe resuelve exclusivamente `raw token -> SHA-256 -> managementTokenHash`;
- solo una suscripción `ACTIVE` con token vigente puede cancelarse;
- unsubscribe cambia `ACTIVE -> UNSUBSCRIBED`, establece `unsubscribedAt` y
  `updatedAt`, y revoca `managementTokenHash` dentro de la misma transacción;
- la fila del subscriber, email, perfil, `subscribedAt` y preferencias se conservan;
- tokens inválidos, desconocidos, blank, revocados o no gestionables fallan de
  forma interna neutral mediante `SubscriptionAccessException`;
- el endpoint HTTP de unsubscribe todavía NO está expuesto;
- email, frontend y admin siguen pendientes.

Validación actual completa:

- 79 pruebas;
- 0 fallos;
- 0 errores;
- `BUILD SUCCESS`.

El contrato detallado se encuentra en `survival72/docs/join-contract.md`.
