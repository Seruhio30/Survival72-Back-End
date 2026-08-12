# Backend Subscription Audit

## Scope

This audit covers only the historical Survival72 backend areas related to:

1. subscribers;
2. subscription contracts;
3. persistence;
4. security;
5. email/newsletter behavior.

No functional code, endpoints, database schema, dependencies, or security mechanisms were changed as part of this audit.

---

## 1. Current architecture

The backend is a Spring Boot application located in `survival72/`.

Main package:

`com.seruhioCode30.survival72`

Relevant structure:

    survival72/
    ├── pom.xml
    ├── mvnw
    ├── src/
    │   ├── main/
    │   │   ├── java/com/seruhioCode30/survival72/
    │   │   │   ├── Survival72Application.java
    │   │   │   ├── config/
    │   │   │   │   ├── MailConfig.java
    │   │   │   │   ├── SchedulerConfig.java
    │   │   │   │   └── WebConfig.java
    │   │   │   ├── controller/
    │   │   │   │   ├── ExportToCSV.java
    │   │   │   │   └── SubscriberController.java
    │   │   │   ├── model/
    │   │   │   │   └── Subscriber.java
    │   │   │   ├── repository/
    │   │   │   │   └── SubscriberRepository.java
    │   │   │   └── service/
    │   │   │       ├── EmailService.java
    │   │   │       └── NewsletterService.java
    │   │   └── resources/
    │   │       └── application.properties
    │   └── test/
    │       └── java/com/seruhioCode30/survival72/
    │           └── Survival72ApplicationTests.java

The subscription flow does not have a dedicated service layer. `SubscriberController` accesses `SubscriberRepository` directly.

### Classification

- Spring Boot application structure: **KEEP**
- Direct controller-to-repository subscription logic: **REFACTOR**
- Dedicated DTO layer: **MISSING / REPLACE**
- Subscription-specific service layer: **MISSING / REPLACE**

---

## 2. Technology and build

### Detected stack

- Spring Boot 3.3.5
- Spring Web
- Spring Data JPA
- MySQL Connector/J
- Spring Mail
- Maven
- JUnit / Spring Boot Test
- Dropbox SDK dependency
- java-dotenv dependency

### Java configuration

The Maven configuration is inconsistent.

The project property declares:

    <java.version>17</java.version>

while `maven-compiler-plugin` specifies:

    <source>21</source>
    <target>21</target>
    <compilerArgs>--enable-preview</compilerArgs>

The WSL environment used during the audit has:

- OpenJDK 21.0.11
- Maven 3.8.7

### Maven Wrapper

The Maven wrapper is not currently reproducible:

- `mvnw` is versioned without executable permission (`100644`);
- `.mvn/wrapper/maven-wrapper.properties` is missing.

Running the wrapper therefore fails.

### Dependency observations

`spring-boot-starter-mail` explicitly uses version 3.3.4 while the Spring Boot parent is 3.3.5.

The explicit Jakarta Mail dependency may be redundant with the Spring Mail starter.

Dropbox SDK and `java-dotenv` are declared but no usage was found in the application code inspected during this audit.

### Classification

- Spring Boot / Maven foundation: **KEEP**
- Java 17 / Java 21 configuration inconsistency: **REFACTOR**
- Broken Maven Wrapper: **REFACTOR**
- Explicit Spring Mail version mismatch: **REFACTOR**
- Dropbox dependency: **REMOVE candidate**
- java-dotenv dependency: **REMOVE candidate**
- Explicit Jakarta Mail dependency: **REFACTOR / verify before removal**

No dependency changes belong in this audit branch.

---

## 3. Subscriber persistence model

The historical entity is `model/Subscriber.java`.

Fields:

    id
    firstName
    lastName
    email
    subscriptionDate
    topicsOfInterest

There is no `city` field.

There are no separate fields for four interests.

There are no JPA declarations for:

- table name;
- column length;
- nullable constraints;
- email uniqueness;
- indexes.

No Flyway, Liquibase, `schema.sql`, or `data.sql` configuration exists.

Database schema evolution relies on:

    spring.jpa.hibernate.ddl-auto=update

SQL logging is enabled:

    spring.jpa.show-sql=true

### Repository

`SubscriberRepository` extends:

    JpaRepository<Subscriber, Long>

and declares:

    Optional<Subscriber> findByEmail(String email);

No database-level uniqueness guarantee for email was found in the entity definition.

This means the code assumes email identifies at most one subscriber without expressing that rule in the JPA model.

### Classification

- JPA repository abstraction: **KEEP**
- Existing Subscriber entity as future Join domain contract: **REPLACE**
- Email lookup concept: **KEEP / REFACTOR**
- `ddl-auto=update` as schema management strategy: **REPLACE**
- Lack of migrations: **REPLACE**
- Lack of email uniqueness definition: **DATA INTEGRITY RISK**

---

## 4. Real subscription contracts

The controller base path is:

    /api/subscribers

This differs from the historical frontend assumption:

    /subscribers

### 4.1 Subscribe

Real backend contract:

    POST /api/subscribers/subscribe

Request body type:

    Subscriber

Accepted model properties:

    id
    firstName
    lastName
    email
    subscriptionDate
    topicsOfInterest

The controller overwrites `subscriptionDate` with the current server date.

Processing:

1. receives `Subscriber` directly;
2. sets `subscriptionDate`;
3. calls `subscriberRepository.save(subscriber)`;
4. attempts to send a welcome email;
5. returns the saved JPA entity.

Success response:

- HTTP 200 OK implicitly;
- serialized `Subscriber` entity.

No explicit `201 Created` response is used.

No request DTO exists.

No validation was found for:

- required name;
- required email;
- email format;
- duplicate email;
- interest values;
- string lengths;
- malformed or unexpected business values.

Email sending exceptions handled by `EmailService` do not roll back the database save. A subscriber can therefore be persisted even if the welcome email fails.

Database access:

    SubscriberRepository.save()

Service called:

    EmailService.sendSubscriptionEmail()

### Classification

- Basic subscribe capability: **KEEP concept**
- Controller contract: **REPLACE**
- Direct JPA entity as public request/response: **REPLACE**
- Lack of validation: **SECURITY / DATA QUALITY RISK**
- Returning full persisted entity: **REFACTOR**
- No duplicate-email handling: **REPLACE**
- Mail failure behavior: **REFACTOR**

---

### 4.2 Update preferences by email

Real backend contract:

    PUT /api/subscribers/update

Request body type:

    Subscriber

The endpoint only uses:

    email
    topicsOfInterest

Processing:

1. calls `findByEmail(email)`;
2. if present, replaces `topicsOfInterest`;
3. saves the existing subscriber;
4. returns a plain-text response.

Success:

    200 OK
    Intereses actualizados con éxito.

Not found:

    404 Not Found
    No se encontró el suscriptor.

There is no authentication, ownership verification, token, or other proof that the caller controls the supplied email address.

Database access:

    SubscriberRepository.findByEmail()
    SubscriberRepository.save()

### Classification

- Preferences update concept: **KEEP**
- Email-only authorization mechanism: **SECURITY RISK / REPLACE**
- Direct entity request: **REPLACE**
- Plain-text API response: **REFACTOR**

---

### 4.3 Update preferences by ID

A second update endpoint exists:

    PUT /api/subscribers/{id}

This endpoint finds the subscriber by database ID.

If new topics are supplied, it combines them with existing topics and removes duplicate strings using a `LinkedHashSet`.

Success:

    200 OK
    Suscriptor actualizado con éxito.

Not found:

    404 Not Found
    No se encontró el suscriptor.

No authentication or authorization was found.

Anyone capable of reaching the endpoint can attempt to modify a subscriber by numeric ID.

### Classification

- Endpoint: **SECURITY RISK**
- Numeric subscriber ID exposed as mutation authority: **REPLACE**
- Topic merging logic: **REFACTOR or REMOVE depending on Join UX**

---

### 4.4 Cancel subscription

Real backend contract:

    DELETE /api/subscribers/cancel?email={email}

The email is supplied as a query parameter.

Processing:

1. calls `findByEmail(email)`;
2. if present, physically deletes the entity;
3. returns a plain-text response.

Success:

    200 OK
    Suscripción cancelada con éxito.

Not found:

    404 Not Found
    No se encontró el suscriptor.

Database access:

    SubscriberRepository.findByEmail()
    SubscriberRepository.delete()

There is:

- no token;
- no authentication;
- no ownership proof;
- no confirmation workflow;
- no soft-delete/unsubscribed state.

Anyone able to call the endpoint can attempt to delete a subscriber using only their email address.

### Classification

**SECURITY RISK / REPLACE**

---

## 5. Frontend versus backend contract comparison

### Subscribe

Historical frontend assumption:

    POST /subscribers/subscribe

Real backend:

    POST /api/subscribers/subscribe

The historical frontend reportedly sent:

- nombre;
- apellido;
- email;
- ciudad;
- four interests.

The backend model expects:

- `firstName`;
- `lastName`;
- `email`;
- `topicsOfInterest`.

There is no city property.

There are no four individual interest properties.

No custom Jackson configuration was found.

Therefore the frontend and backend are not contractually equivalent.

Classification:

**INCOMPATIBLE unless the historical JavaScript transformed the payload before sending it.**

### Update

Historical frontend assumption:

    POST /subscribers/actualizar

Real backend:

    PUT /api/subscribers/update

The historical frontend and backend differ in:

- base path;
- HTTP method;
- route name.

Backend request semantics are:

    email
    topicsOfInterest

Classification:

**INCOMPATIBLE**

### Cancel / unsubscribe

Historical frontend assumptions:

    POST /subscribers/cancelar

with an email payload, and elsewhere:

    DELETE /subscribers/cancelar

without an identifier.

Real backend:

    DELETE /api/subscribers/cancel?email={email}

No historical `POST /cancelar` endpoint was found.

No identifier-less DELETE endpoint was found.

Classification:

**INCOMPATIBLE**

---

## 6. Additional subscriber-related endpoints

### List all subscribers

    GET /api/subscribers

Returns a list of complete `Subscriber` entities.

This includes:

- database ID;
- first name;
- last name;
- email;
- subscription date;
- interests.

No authentication or authorization mechanism was found.

Classification:

**SECURITY RISK / REMOVE from public API**

If administrative listing is needed in the future, it must be placed behind explicit administrative authorization.

### CSV export

    GET /api/export/toCSV

Behavior:

1. loads every subscriber with `findAll()`;
2. writes a `subscribers.csv` file to the server filesystem;
3. writes ID, name, surname, email, subscription date and interests;
4. returns a text message instead of the generated file.

No authentication or authorization was found.

The CSV writer performs no robust CSV escaping.

The endpoint catches generic exceptions, prints stack traces, and returns the exception message to the caller.

Classification:

**SECURITY RISK / REMOVE or completely redesign as protected admin functionality**

### Newsletter test endpoint

Inside `SubscriberController` there is an additional controller mapped to:

    /test-newsletter

with:

    GET /test-newsletter/send

It invokes:

    NewsletterService.sendMonthlyNewsletter()

No authentication or authorization mechanism was found.

This means an externally reachable caller may be able to trigger email delivery to all subscribers.

Classification:

**SECURITY RISK / REMOVE from public API**

---

## 7. Newsletter behavior

Scheduling is enabled globally through `@EnableScheduling`.

The newsletter cron expression is:

    0 0 8 1 * ?

This represents the first day of each month at 08:00 according to the scheduler/server timezone.

`NewsletterService`:

1. loads every subscriber with `findAll()`;
2. sends one email per subscriber;
3. uses subscriber name and topics;
4. logs subscriber email addresses;
5. includes a subscription-management link containing the email address in the URL.

The historical link points to:

    http://localhost:5500/update.html?email=...

The visible button text indicates cancellation even though it points to the update page.

### Classification

- Scheduled newsletter concept: **KEEP optional**
- Current newsletter implementation: **REPLACE**
- Email in URL: **SECURITY RISK**
- localhost frontend URL: **REPLACE**
- Public test trigger: **REMOVE**
- PII logging: **SECURITY RISK**

---

## 8. CORS

Global CORS configuration applies to:

    /**

Allowed origins:

    http://127.0.0.1:5500
    http://localhost:5500

Allowed methods:

    GET
    POST
    PUT
    DELETE
    OPTIONS

Allowed headers:

    *

Credentials:

    true

`SubscriberController` also declares:

    @CrossOrigin(origins = "http://localhost:5500")

This duplicates part of the global policy.

The policy is development-specific and broadly applies to every endpoint.

Classification:

**REFACTOR**

Join should use environment-specific allowed origins and avoid redundant controller-level CORS configuration.

---

## 9. Authentication and authorization

No evidence was found for:

- Spring Security;
- authentication;
- authorization;
- JWT;
- bearer tokens;
- session-based access control;
- subscriber ownership verification;
- unsubscribe tokens;
- administrative roles.

`spring-boot-starter-security` is not declared in `pom.xml`.

No security implementation was found under `src/main/java`.

Consequently, the following sensitive operations appear publicly callable whenever network access to the backend is possible:

- list all subscribers;
- update preferences by email;
- update subscriber interests by ID;
- cancel a subscriber using their email;
- export all subscriber data;
- trigger newsletter delivery.

Classification:

**SECURITY RISK — CRITICAL**

---

## 10. Validation

No use was found for:

- `@Valid`;
- `@Validated`;
- `@NotNull`;
- `@NotBlank`;
- `@Email`;
- `@Size`;
- `@Pattern`;
- validation-specific exception handling.

Classification:

**REPLACE**

Join needs explicit request DTO validation and business validation.

---

## 11. Exception handling and HTTP responses

No global:

- `@ControllerAdvice`;
- `@RestControllerAdvice`;
- `@ExceptionHandler`;
- explicit application exception model

was found.

Some endpoints manually return 404 while other errors rely on Spring's default exception handling.

`ExportToCSV` catches generic `Exception`, prints a stack trace, and includes the raw exception message in its response.

Classification:

**REPLACE**

Join should expose a consistent API error contract without leaking internal exception details.

---

## 12. Personal data exposure

Personal information is exposed or processed insecurely in several places.

### API responses

`GET /api/subscribers` returns full subscriber entities.

### CSV

`GET /api/export/toCSV` writes subscriber personal information to a server-side file.

### URLs

Newsletter links contain subscriber email addresses in query parameters.

### Logs

Subscriber email addresses are written using `System.out` and `System.err` during welcome mail and newsletter operations.

Classification:

**SECURITY RISK — CRITICAL**

---

## 13. Secret management

Sensitive application configuration is committed to Git.

The audit confirmed hardcoded values for:

- database URL;
- database username;
- database password;
- mail username;
- mail password.

Additionally, `MailConfig.java` contains hardcoded SMTP credentials directly in source code.

The real secret values are intentionally not reproduced in this document.

Historical Git inspection showed that sensitive values were already present from the initial commit.

### Required follow-up

Outside this audit branch:

1. rotate exposed credentials;
2. externalize application secrets;
3. ensure secret files/configuration are ignored;
4. determine whether Git history cleanup is appropriate;
5. configure separate development and production environments.

Classification:

**SECURITY RISK — CRITICAL / REPLACE**

---

## 14. Mail configuration

Mail configuration exists both in:

    MailConfig.java

and:

    application.properties

This creates duplicated configuration sources.

`MailConfig` manually creates `JavaMailSender`.

SMTP debug logging is enabled.

Classification:

**REFACTOR / REPLACE**

Join should use externalized configuration and a single clearly defined mail configuration source.

---

## 15. Database configuration

Database technology:

**MySQL**

Persistence:

**Spring Data JPA / Hibernate**

Schema management:

    spring.jpa.hibernate.ddl-auto=update

The database connection information is hardcoded in versioned configuration.

No migration framework was found.

### Classification

- MySQL: **KEEP possible**
- Spring Data JPA: **KEEP**
- Hibernate auto-update schema management: **REPLACE**
- Versioned database credentials: **SECURITY RISK**
- No migrations: **REPLACE**

---

## 16. Historical Git observations

Subscriber controller, model and repository were introduced in the initial repository commit and show no later contract changes in Git history.

Therefore there is no evidence in this repository that endpoints such as:

    /subscribers/actualizar
    /subscribers/cancelar

were previous backend contracts.

The later configuration commit only added:

    server.port=8080
    server.address=0.0.0.0

It did not change subscriber contracts.

This supports the conclusion that the historical frontend and backend became or were originally contractually misaligned.

---

## 17. Tests

The only detected test is:

    Survival72ApplicationTests.contextLoads()

No tests were found for:

- subscribe;
- update preferences;
- unsubscribe;
- repository behavior;
- validation;
- HTTP contracts;
- authorization;
- newsletter;
- CSV export.

The audit did not start the Spring application or execute the context test because the application currently contains historical hardcoded external database/mail configuration.

Classification:

**REPLACE / EXPAND**

---

## 18. Reuse classification

### KEEP

- Spring Boot foundation
- Spring Web
- Spring Data JPA
- Repository abstraction
- conceptual subscriber persistence
- conceptual preference management
- optional scheduled newsletter capability
- MySQL, if still desired for the new architecture

### REFACTOR

- package organization
- controller/service separation
- CORS
- Java/Maven build configuration
- mail configuration
- API response conventions
- preference representation
- Maven Wrapper
- logging

### REPLACE

- public `Subscriber` entity contract
- subscribe request/response contract
- email-only preference updates
- email-query unsubscribe
- secret management
- schema management through `ddl-auto=update`
- validation strategy
- exception handling
- newsletter management links
- public administrative functionality

### REMOVE

From the public API:

- `GET /api/subscribers`
- `GET /api/export/toCSV`
- `GET /test-newsletter/send`
- `PUT /api/subscribers/{id}` in its current form

Dependency removal candidates after separate verification:

- Dropbox SDK
- java-dotenv
- potentially explicit Jakarta Mail dependency

### SECURITY RISK

- committed database credentials;
- committed SMTP credentials;
- SMTP credentials hardcoded in Java source;
- no authentication;
- no authorization;
- subscriber enumeration;
- mutation by email;
- mutation by numeric ID;
- deletion by email;
- public CSV export;
- public newsletter trigger;
- subscriber email addresses in URLs;
- personal data in console logs;
- exception details exposed by CSV endpoint.

### UNKNOWN

- current contents/state of the historical production database;
- whether duplicate subscriber emails already exist;
- whether the historical frontend ever transformed its four interests into `topicsOfInterest`;
- whether this backend was publicly deployed while these endpoints were reachable;
- whether exposed historical credentials are still active.

---

## 19. Recommendation for the new Join architecture

The historical subscription code should be treated as a reference for business intent, not as the API contract for Join.

Recommended foundation:

    JoinController
          ↓
    JoinService
          ↓
    SubscriberRepository
          ↓
    MySQL

Public API requests should use dedicated DTOs rather than JPA entities.

### Subscribe

A dedicated subscribe request should contain only supported public fields, for example:

    firstName
    lastName
    email
    preferences

If city is still required by product requirements, it should be explicitly modeled rather than silently ignored.

Server responsibilities should include:

- request validation;
- normalized email;
- duplicate handling;
- controlled response DTO;
- explicit HTTP status;
- transactional persistence;
- email confirmation behavior defined separately from persistence.

### Preferences

Preference management must not authorize changes using only an email address or public database ID.

Use an opaque backend-issued token or another verified ownership mechanism.

### Unsubscribe

Do not expose email addresses as the unsubscribe credential.

Recommended pattern:

    unsubscribe URL
          ↓
    opaque random token
          ↓
    backend validates token
          ↓
    subscriber status updated

The token should be:

- generated by the backend;
- unpredictable;
- sufficiently random;
- revocable/rotatable if needed;
- stored securely, preferably as a hash when appropriate;
- independent of the subscriber database ID and email.

For Join, a subscription status such as active/unsubscribed is preferable to blindly deleting the subscriber record if retention requirements permit it.

### Administrative operations

Subscriber listing, CSV export and manual newsletter sending must not exist as anonymous public endpoints.

If retained, they should live behind an authenticated and authorized administrative boundary.

### Persistence

Introduce explicit schema migrations rather than relying on Hibernate `ddl-auto=update`.

Email uniqueness should be enforced consistently at both:

- application level;
- database level.

### Secrets

All credentials must come from environment/deployment secret configuration and never from committed source files.

---

## 20. Final conclusion

The historical backend contains reusable Spring/JPA concepts, but its existing subscription HTTP contract should not be carried directly into the new Join system.

The most valuable reusable pieces are:

- Spring Boot;
- Spring Data JPA;
- the repository pattern;
- the basic subscriber/newsletter business concepts.

The most important pieces to replace are:

- public JPA entity contracts;
- email-based authorization;
- unsubscribe-by-email;
- open administrative endpoints;
- secret management;
- validation and error handling;
- schema management;
- subscriber-management links.

The new Join implementation should therefore be designed from a new canonical API contract rather than attempting to make the new frontend conform to the historical backend.
