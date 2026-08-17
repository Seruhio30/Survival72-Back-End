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
- `POST /api/subscriptions/unsubscribe` expone la frontera HTTP de cancelación;
- autorización exclusivamente mediante `Authorization: Bearer <management-token>`;
- el endpoint no acepta email, ID ni token en query/path y no requiere body;
- éxito devuelve `200 OK` con respuesta pública neutral `UNSUBSCRIBED`;
- acceso inválido, desconocido, revocado o no gestionable devuelve `404 Not Found`
  neutral con código `SUBSCRIPTION_ACCESS_NOT_FOUND`;
- integración base de email implementada mediante `JoinApplicationService`,
  `SubscriptionUnsubscribeApplicationService` y `SubscriptionEmailService`;
- `NEW_SUBSCRIPTION` y `REJOINED` envían welcome email usando el raw management
  token únicamente de forma temporal para construir links;
- `ACTIVE_DUPLICATE` no rota token y no envía welcome email;
- los links de gestión usan frontend URL + fragmento:
  `/manage#token=<token>` y `/unsubscribe#token=<token>`;
- email nunca aparece en las URLs y el token no se envía como query parameter;
- unsubscribe confirmation se envía después del lifecycle y no necesita
  management token;
- fallos SMTP se manejan después de completar el lifecycle persistido y no
  provocan rollback de Join ni unsubscribe;
- `app.frontend.base-url` usa `FRONTEND_BASE_URL`, con
  `http://localhost:5500` como fallback de desarrollo;
- configuración SMTP sensible se externaliza mediante `MAIL_USERNAME`,
  `MAIL_PASSWORD` y `MAIL_FROM`;
- `MailConfig` histórico con credenciales hardcodeadas fue eliminado;
- frontend Admin, subscriber admin UI, dashboard visual,
  retry avanzado/outbox y legacy cleanup siguen pendientes.

Validación dirigida del bloque de email:

- 66 pruebas;
- 0 fallos;
- 0 errores;
- `BUILD SUCCESS`;
- incluye tests unitarios de email/orquestación, controllers, JoinService,
  unsubscribe y 2 pruebas de integración con MySQL real que verifican que un
  fallo SMTP no revierte el lifecycle persistido.

### Tests desde WSL contra MySQL en Windows

`application.properties` continúa apuntando a `localhost/root`, por lo que los
tests de integración desde WSL requieren usar el MySQL de Windows mediante la
IP del host.

Procedimiento recurrente:

1. Obtener la IP de Windows desde WSL con `ip route`.
2. Verificar que MySQL responde en el puerto `3306`.
3. Exportar localmente `SPRING_DATASOURCE_USERNAME=survival72_dev`.
4. Cargar `SPRING_DATASOURCE_PASSWORD` de forma oculta y sin versionarla.
5. Ejecutar Maven pasando
   `-Dspring.datasource.url=jdbc:mysql://<windows-host>:3306/survival72_db`.

En el entorno validado durante este bloque, el host fue `192.168.16.1`.
La contraseña nunca debe incluirse en Git, documentación ni logs.

El contrato detallado se encuentra en `survival72/docs/join-contract.md`.

## Admin security foundation

La seguridad base del nuevo Admin está implementada en backend.

Incluye:

- Spring Security como frontera de autenticación y autorización;
- un único administrador para el MVP;
- username configurado mediante `ADMIN_USERNAME`;
- password verificado exclusivamente contra un hash BCrypt configurado mediante
  `ADMIN_PASSWORD_HASH`;
- ninguna credencial administrativa nueva ni password hash sensible se almacena
  en Git;
- autenticación basada en sesión HTTP, sin JWT;
- protección de `/api/admin/**` mediante autenticación backend;
- `POST /api/admin/auth/login` para establecer la sesión administrativa;
- `GET /api/admin/auth/session` para consultar el estado autenticado y obtener el
  token CSRF requerido por el futuro frontend Admin;
- `POST /api/admin/auth/logout` para invalidar la sesión;
- protección contra session fixation mediante cambio del session ID después de
  autenticación válida;
- CSRF habilitado para mutaciones administrativas mediante
  `HttpSessionCsrfTokenRepository`;
- Join, Management y Unsubscribe conservan sus contratos públicos existentes y
  no requieren sesión administrativa;
- CORS local continúa limitado a `http://localhost:5500` y
  `http://127.0.0.1:5500`, con credentials habilitadas;
- cookie de sesión con `HttpOnly=true`;
- `SameSite` configurable mediante `SESSION_COOKIE_SAME_SITE`, con `lax` como
  fallback local;
- atributo `Secure` configurable mediante `SESSION_COOKIE_SECURE`, con `false`
  como fallback local; producción HTTPS deberá configurarlo como `true`;
- endpoint técnico `/api/admin/security-check` limitado a este foundation para
  validar autorización y CSRF; no constituye un dashboard;
- Content se implementa de forma separada mediante la base administrativa descrita más abajo;
- Newsletter se implementa mediante la base administrativa descrita más abajo;
- no se implementaron subscriber admin UI ni dashboard visual.

## Admin subscriber read model

La lectura administrativa segura de subscribers ya está implementada mediante:

- `GET /api/admin/subscribers`;
- acceso exclusivo con sesión Admin válida bajo la protección existente de `/api/admin/**`;
- paginación real con `page` desde `0`, `size` por defecto `20` y máximo `100`;
- filtros opcionales por `status` y `preference`;
- combinación de filtros con semántica `status AND preference`;
- orden estable por `subscribedAt DESC, id DESC`;
- respuesta mediante DTO administrativo controlado, sin serializar la entidad `Subscriber`;
- campos expuestos: `id`, `email`, `firstName`, `countryCode`, `status`, `preferences`, `subscribedAt`, `updatedAt` y `unsubscribedAt`;
- `managementTokenHash`, tokens raw, campos legacy transitorios y otros datos internos permanecen ocultos;
- valores inválidos de `page`, `size`, `status` o `preference` devuelven `400 BAD_REQUEST` controlado.

- no se implementaron subscriber admin UI, dashboard visual ni analytics.

## Admin content foundation

La gestión administrativa mínima de contenido ya está implementada en backend.

Incluye:

- entidad `ContentItem`;
- tipos `VIDEO` y `ARTICLE`;
- estados `DRAFT`, `PUBLISHED` y `ARCHIVED`;
- cero o varias preferencias canónicas mediante `SubscriberPreference`;
- persistencia Flyway en `content_item` y `content_item_preferences`;
- YouTube mediante ID canónico de 11 caracteres, aceptando ID directo,
  `youtube.com/watch?v=...` y `youtu.be/...`;
- ningún archivo de video, llamada a YouTube API ni metadata externa;
- `ARTICLE` rechaza valores YouTube y conserva `youtubeVideoId = null`;
- `VIDEO` requiere un identificador YouTube válido;
- publicación establece `publishedAt` únicamente cuando todavía está vacío;
- archivado conserva el contenido y no realiza borrado físico;
- `GET /api/admin/content` paginado, con `page=0`, `size=20`, máximo `100`,
  filtros opcionales `type` y `status`, y orden `createdAt DESC, id DESC`;
- `GET /api/admin/content/{id}`;
- `POST /api/admin/content`;
- `PATCH /api/admin/content/{id}`;
- respuestas mediante DTOs administrativos controlados, sin exponer la entidad JPA;
- todos los endpoints reutilizan la protección existente de `/api/admin/**`;
- POST y PATCH mantienen CSRF obligatorio.

Este foundation es backend-only. Frontend Admin, publicación pública de contenido,
dashboard, analytics y uploads permanecen fuera de este bloque.

La configuración productiva definitiva de CORS/cookies depende del dominio HTTPS
final del frontend Admin y permanece pendiente.


## Admin newsletter foundation

La gestión administrativa mínima de newsletters ya está implementada en backend.

Incluye:

- entidad `Newsletter`;
- estados `DRAFT`, `READY_TO_SEND` y `SENT`;
- persistencia Flyway mediante `newsletter` y `newsletter_preferences`;
- `subject` y `body` obligatorios;
- mínimo una preferencia canónica por newsletter;
- creación siempre en estado `DRAFT`;
- edición permitida para `DRAFT`;
- editar una newsletter `READY_TO_SEND` la devuelve automáticamente a `DRAFT`;
- `SENT` permanece inmutable y no puede fabricarse mediante PATCH;
- `POST /api/admin/newsletters/{id}/ready` como transición explícita a `READY_TO_SEND`;
- listado paginado con `page=0`, `size=20`, máximo `100`,
  filtro opcional por `status` y orden `createdAt DESC, id DESC`;
- audience preview paginado mediante
  `GET /api/admin/newsletters/{id}/audience-preview`;
- audiencia resuelta directamente en base de datos con
  `status = ACTIVE` y semántica ANY sobre las preferencias seleccionadas;
- resultados `DISTINCT` para evitar duplicar subscribers que coincidan con
  varias preferencias;
- preview administrativo limitado a `id`, `email`, `firstName` y `preferences`,
  sin exponer management tokens, hashes, sesiones ni entidades JPA;
- todos los endpoints reutilizan la sesión Admin y CSRF existentes;
- no existe endpoint `/send` en este foundation;
- no se realiza envío SMTP ni se marca una newsletter como `SENT` sin una
  futura operación real de entrega.

Validación del bloque:

- 31 pruebas dirigidas, 0 fallos y 0 errores;
- suite completa: 192 pruebas, 0 fallos, 0 errores y 0 skipped;
- `BUILD SUCCESS`;
- `git diff --check` limpio;
- Flyway `V6__create_admin_newsletter_tables.sql` y persistencia validados
  contra MySQL 8 real desde WSL.

Frontend Newsletter, transporte de correo, batching, queue/outbox, retries,
scheduler, métricas y automatización permanecen fuera de alcance.


La credencial histórica de base de datos que todavía existe en
`application.properties` continúa siendo un riesgo conocido y debe rotarse y
externalizarse en un bloque de seguridad separado antes de producción.
