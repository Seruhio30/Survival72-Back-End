# Survival72 Join — Canonical MVP Contract

## 1. Objective

Join is the public subscription system for Survival72.

It allows a person to:

- subscribe to Survival72 communications;
- select topics of interest;
- manage allowed preferences through a secure management token;
- unsubscribe safely;
- rejoin later using the same email address.

Join is not an authentication or user account system.

A Subscriber is not a User Account.

The MVP does not include passwords, login, sessions, roles, profiles, or subscriber dashboards.

This document is the source of truth for the future Join implementation.

## 2. MVP Scope

The MVP supports three public operations:

1. Join.
2. Manage subscription preferences.
3. Unsubscribe.

It also defines server-side behavior for:

- duplicate active subscriptions;
- rejoining after unsubscribe;
- email normalization;
- secure management tokens;
- persistence;
- email delivery;
- validation;
- controlled errors.

The new contract does not preserve compatibility with the historical subscription API.

Historical endpoints, request formats, entity exposure, unsubscribe-by-email mechanisms, and contracts must not be reused as public API contracts.

## 3. Subscriber Data Model

### 3.1 Final MVP fields

A Subscriber contains:

| Field | Required | Purpose |
|---|---|---|
| `id` | Yes | Internal database identifier only |
| `email` | Yes | Delivery address and unique subscriber identity |
| `firstName` | No | Basic communication personalization |
| `countryCode` | Yes | Regional content targeting |
| `status` | Yes | Subscription lifecycle state |
| `subscribedAt` | Yes | Start of the current active subscription period |
| `updatedAt` | Yes | Last subscription modification |
| `unsubscribedAt` | No | Time of unsubscribe |
| `managementTokenHash` | Active only | Secure management credential representation |
| `preferences` | Yes | Topics selected by the subscriber |

Internal IDs must not be exposed unnecessarily through the public API.

### 3.2 Fields excluded from the MVP

The MVP does not collect:

- last name;
- city;
- street address;
- phone number;
- date of birth;
- precise location;
- religion;
- church membership or religious affiliation;
- passwords.

### 3.3 Country

`countryCode` is required.

It uses ISO 3166-1 alpha-2 codes.

Initial supported examples include:

- `CR`
- `GT`
- `SV`
- `HN`
- `NI`
- `PA`
- `BZ`

The initial product may primarily serve Costa Rica, but the contract must support future use elsewhere in Central America.

City is intentionally excluded until Survival72 has a concrete feature that requires city-level information.

Religious or church membership information must not be stored merely because Survival72 may be distributed through church communities.

## 4. Subscription Preferences

The initial canonical preferences are:

- `GENERAL_PREPAREDNESS`
- `EMERGENCY_KIT`
- `EDUCATIONAL_CONTENT`
- `EVENTS_AND_TRAINING`

### 4.1 Meaning

#### GENERAL_PREPAREDNESS

General emergency preparedness, family planning, recommendations, and preparedness guidance.

#### EMERGENCY_KIT

72-hour kits, water, food, supplies, and related preparation.

#### EDUCATIONAL_CONTENT

Guides, PDFs, videos, educational resources, and new learning material.

#### EVENTS_AND_TRAINING

Classes, workshops, talks, training sessions, and related activities.

### 4.2 Preference rules

A Join request must contain at least one supported preference.

Duplicate preference values are not allowed.

Unknown preference values are rejected.

Real-time emergency alerts are not represented as an MVP preference because Survival72 does not currently provide an official real-time alerting service.

## 5. Subscriber Status

The MVP has exactly two states:

- `ACTIVE`
- `UNSUBSCRIBED`

Additional states such as `PENDING`, `BLOCKED`, `BOUNCED`, or `DELETED` are outside the MVP.

A boolean `active` flag must not replace the status field.

## 6. Lifecycle

### 6.1 New Join

New email -> validate -> normalize email -> create subscriber -> ACTIVE -> generate management token -> store token hash -> persist preferences -> send welcome email.

### 6.2 Active duplicate

If the normalized email already belongs to an `ACTIVE` subscriber, the subscriber remains `ACTIVE`.

The operation must:

- not create another subscriber;
- not overwrite preferences;
- not modify first name;
- not modify country;
- not rotate the management token;
- not expose whether the email already existed.

### 6.3 Unsubscribe

`ACTIVE` -> `UNSUBSCRIBED`.

The operation must:

- set `status = UNSUBSCRIBED`;
- set `unsubscribedAt = now`;
- set `updatedAt = now`;
- revoke the management credential;
- clear `managementTokenHash`;
- preserve the subscriber record;
- preserve existing preferences.

### 6.4 Rejoin

If the normalized email belongs to an `UNSUBSCRIBED` subscriber, the existing record is reused and returns to `ACTIVE`.

Rejoin must:

- set `status = ACTIVE`;
- set `subscribedAt = now`;
- set `updatedAt = now`;
- set `unsubscribedAt = null`;
- update `firstName` from the new Join request;
- update `countryCode` from the new Join request;
- replace preferences with those from the new Join request;
- generate a new management token;
- store its new hash;
- send a new welcome email.

The previous management token must remain invalid.

## 7. Email Normalization

Before lookup or persistence, email must be normalized.

Canonical MVP normalization:

1. trim surrounding whitespace;
2. convert to lowercase.

Example:

`" Sergio@Example.COM "` -> `"sergio@example.com"`

Survival72 must not perform provider-specific transformations.

For example, it must not:

- remove Gmail dots;
- remove `+tag` portions;
- otherwise rewrite the local part of an address.

Database uniqueness applies to the normalized email value.

## 8. Public API

The canonical public API is:

- `POST /api/join`
- `GET /api/subscriptions/manage`
- `PATCH /api/subscriptions/manage`
- `POST /api/subscriptions/unsubscribe`

Management endpoints require:

`Authorization: Bearer <management-token>`

The management token must not be placed in the backend API path or query string.

The public API must not expose JPA entities directly.

## 9. POST /api/join

### Purpose

Create a new subscription or reactivate a previously unsubscribed subscriber.

### Authentication

None.

### Request DTO

Conceptual DTO: `JoinRequest`

```json
{
  "email": "sergio@example.com",
  "firstName": "Sergio",
  "countryCode": "CR",
  "preferences": [
    "GENERAL_PREPAREDNESS",
    "EMERGENCY_KIT"
  ]
}
```

### Validation

#### email

- required;
- valid email syntax;
- trimmed;
- normalized to lowercase before lookup and persistence;
- maximum length: 254 characters.

#### firstName

- optional;
- trimmed;
- blank after normalization becomes `null`;
- maximum length: 80 characters.

#### countryCode

- required;
- exactly two alphabetic characters;
- normalized to uppercase;
- must belong to the supported country list.

#### preferences

- required;
- must contain at least one value;
- values must be unique;
- every value must be supported.

### Success response

To reduce email enumeration, valid Join requests use the same external success shape for new subscribers, active duplicates, and rejoins.

Status: `200 OK`

Conceptual DTO: `JoinResponse`

```json
{
  "status": "REQUEST_ACCEPTED",
  "message": "Join request processed."
}
```

The response must not expose:

- internal subscriber ID;
- whether the email already existed;
- current subscription status;
- management token;
- management token hash;
- the complete Subscriber entity.

### Internal behavior

- new email: create an active subscriber and send the welcome email;
- existing `ACTIVE` email: keep the current subscriber unchanged;
- existing `UNSUBSCRIBED` email: perform the Rejoin lifecycle.

## 10. GET /api/subscriptions/manage

### Purpose

Load the editable subscription information for an active subscriber.

### Authentication

Required: `Authorization: Bearer <management-token>`

### Request body

None.

### Success

Status: `200 OK`

Conceptual DTO: `SubscriptionManagementResponse`

```json
{
  "firstName": "Sergio",
  "countryCode": "CR",
  "preferences": [
    "GENERAL_PREPAREDNESS",
    "EMERGENCY_KIT"
  ],
  "status": "ACTIVE"
}
```

The response does not include internal ID, management token, token hash, unnecessary timestamps, or email address.

Changing email through the management flow is outside the MVP.

Invalid, unknown, or revoked token: `404 Not Found` with code `SUBSCRIPTION_ACCESS_NOT_FOUND`.

## 11. PATCH /api/subscriptions/manage

### Purpose

Modify the fields the subscriber is allowed to manage.

### Authentication

Required: `Authorization: Bearer <management-token>`

### Request DTO

Conceptual DTO: `UpdateSubscriptionPreferencesRequest`

```json
{
  "firstName": "Sergio",
  "countryCode": "CR",
  "preferences": [
    "GENERAL_PREPAREDNESS",
    "EDUCATIONAL_CONTENT"
  ]
}
```

Editable fields:

- `firstName`;
- `countryCode`;
- `preferences`.

The subscriber may not directly modify email, status, timestamps, management credentials, or internal ID.

Success: `200 OK`.

```json
{
  "status": "UPDATED",
  "message": "Subscription preferences updated successfully."
}
```

Invalid request: `400 Bad Request`.

Invalid, unknown, or revoked token: `404 Not Found`.

## 12. POST /api/subscriptions/unsubscribe

### Purpose

Cancel an active subscription.

### Authentication

Required: `Authorization: Bearer <management-token>`

### Request body

None.

### Behavior

A successful unsubscribe:

1. validates the management token;
2. resolves the active subscriber;
3. sets status to `UNSUBSCRIBED`;
4. sets `unsubscribedAt`;
5. updates `updatedAt`;
6. revokes the management token;
7. clears the stored token hash;
8. preserves the subscriber and preferences.

### Success

Status: `200 OK`

```json
{
  "status": "UNSUBSCRIBED",
  "message": "Subscription cancelled successfully."
}
```

Invalid or already revoked token: `404 Not Found`.

The API does not retain revoked tokens merely to make repeated unsubscribe requests return success.

## 13. Management Token

The management token is a bearer credential.

Anyone possessing a valid token can manage the associated subscription.

### 13.1 Generation

The token must be:

- generated by the backend;
- cryptographically random;
- unpredictable;
- independent of email;
- independent of subscriber ID;
- sufficiently long.

Recommended MVP design: 32 random bytes or more, encoded using Base64 URL-safe format.

JWT is not required.

An opaque token is preferred because it is easier to revoke and rotate.

### 13.2 Persistence

The raw management token must not be stored in the database.

Persist `SHA-256(raw token)` instead.

A hexadecimal SHA-256 representation may use `VARCHAR(64)`.

`management_token_hash` must be unique when present.

### 13.3 Lifecycle

- new Join: generate a new token;
- active duplicate: do not rotate the token;
- unsubscribe: revoke the token and set its stored hash to `NULL`;
- rejoin: generate a completely new token.

### 13.4 Logging

The raw token must not appear in application logs.

Token-bearing authorization headers must not be intentionally logged.

## 14. Frontend Management Links

Email links must point to frontend pages rather than directly performing API mutations.

Recommended conceptual links:

- `https://<frontend>/manage#token=<token>`
- `https://<frontend>/unsubscribe#token=<token>`

The frontend reads the token from the URL fragment and sends it to the backend using the Bearer authorization header.

Using a URL fragment avoids transmitting the token to the web server as part of the initial page request.

Exact production URLs are environment configuration and must not be hardcoded into the domain contract.

## 15. GET Must Never Unsubscribe

Opening a link must never cancel a subscription.

Unsubscribe is a state-changing action and must use `POST /api/subscriptions/unsubscribe`.

The unsubscribe frontend page may first display a confirmation screen.

This protects users from mail scanners, link previews, crawlers, and security software that may automatically perform GET requests.

## 16. Persistence Model

### 16.1 subscribers

Conceptual table:

```text
subscribers
─────────────────────────────
id
email
first_name
country_code
status
subscribed_at
updated_at
unsubscribed_at
management_token_hash
```

Constraints:

- `id`: primary key, internal only;
- `email`: `NOT NULL`, unique, stored normalized;
- `first_name`: nullable;
- `country_code`: `NOT NULL`, two-character country code;
- `status`: `NOT NULL`, allowed values `ACTIVE` and `UNSUBSCRIBED`;
- `subscribed_at`: `NOT NULL`;
- `updated_at`: `NOT NULL`;
- `unsubscribed_at`: nullable and normally `NULL` while active;
- `management_token_hash`: unique when present, required while active, `NULL` after unsubscribe.

### 16.2 subscriber_preferences

Preferences are stored separately rather than as boolean columns or JSON.

Conceptual table:

```text
subscriber_preferences
─────────────────────────────
subscriber_id
preference
```

Constraints:

- `subscriber_id NOT NULL`;
- `preference NOT NULL`;
- foreign key to `subscribers.id`;
- unique `(subscriber_id, preference)`.

The foreign key may use `ON DELETE CASCADE` for referential integrity even though normal unsubscribe does not delete subscribers.

## 17. Timestamp Semantics

### New subscription

- `subscribedAt = now`;
- `updatedAt = now`;
- `unsubscribedAt = null`.

### Preference update

- `subscribedAt` unchanged;
- `updatedAt = now`;
- `unsubscribedAt` unchanged.

### Unsubscribe

- `subscribedAt` unchanged;
- `updatedAt = now`;
- `unsubscribedAt = now`.

### Rejoin

- `subscribedAt = now`;
- `updatedAt = now`;
- `unsubscribedAt = null`.

For the MVP, `subscribedAt` represents the beginning of the current active subscription period.

Historical fields such as `firstSubscribedAt` or a complete subscription-event history are outside scope.

## 18. Database Migrations

The new Join schema must use versioned Flyway migrations.

Hibernate `ddl-auto=update` must not be considered the source of truth for the new schema.

Target architecture:

- Flyway: schema evolution source of truth;
- JPA/Hibernate: persistence mapping and schema validation.

The implementation phase must define a safe migration path from the historical database before changing production data.

That migration work must not reshape the new contract merely to preserve historical API compatibility.


### 18.1 Persistence foundation implementation

The persistence foundation was implemented in `feat/join-persistence-foundation`.

The inspected development database contained the historical `subscriber` table with zero rows, so no duplicate emails, null email data, or other incompatible records blocked the migration.

Flyway strategy:

- explicit baseline at version 1 for the existing historical schema;
- V2 migrates to the canonical Join persistence model;
- V3 aligns `management_token_hash` to `VARCHAR(64)`;
- automatic `baseline-on-migrate` is not used;
- historical columns remain in place to avoid destructive migration.

Hibernate now uses `spring.jpa.hibernate.ddl-auto=validate`.

Persistence tests currently report 5 tests, 0 failures, 0 errors, and `BUILD SUCCESS`.



## 19. Email Strategy

### 19.1 Welcome email

Send after:

- a successful new Join;
- a successful Rejoin.

The email should contain:

- optional greeting using `firstName`;
- confirmation that the subscription is active;
- a short explanation of Survival72;
- a management link;
- an unsubscribe link.

### 19.2 Unsubscribe confirmation

The MVP should send a short confirmation after successful unsubscribe.

The message confirms that communications have been stopped.

It may provide a normal link back to the public Join experience for future rejoining.

It must not reactivate the subscription automatically.

### 19.3 No double opt-in in MVP

A confirmation-before-activation workflow is outside the initial MVP.

Successful Join activates the subscription directly.

The MVP therefore does not introduce a `PENDING` status.

### 19.4 Email failure

Database state must not be rolled back solely because email delivery fails.

Conceptual flow: validate -> persist database transaction -> attempt email delivery.

If email delivery fails:

- the subscriber state remains persisted;
- the failure is handled internally;
- sensitive data and raw management tokens are not logged unnecessarily.

Persistent queues, transactional outbox patterns, and advanced retry systems are outside MVP scope.

## 20. Error Contract

Conceptual DTO: `ApiErrorResponse`

### Validation example

```json
{
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid data.",
  "fieldErrors": {
    "email": "must be a valid email address",
    "preferences": "must contain at least one preference"
  }
}
```

`fieldErrors` is optional and applies when field-specific validation details are useful.

### Invalid management credential

```json
{
  "code": "SUBSCRIPTION_ACCESS_NOT_FOUND",
  "message": "The subscription management link is invalid or no longer available."
}
```

### Internal error

Unexpected server failures return a generic controlled response.

The public API must not expose:

- Java exception class names;
- SQL details;
- stack traces;
- internal IDs;
- entities;
- secrets;
- raw management tokens.

## 21. HTTP Status Codes

### POST /api/join

- `200 OK`: valid Join request accepted for processing, including new subscription, active duplicate, or rejoin;
- `400 Bad Request`: invalid request data;
- `500 Internal Server Error`: unexpected failure preventing processing.

### GET /api/subscriptions/manage

- `200 OK`: valid active management credential;
- `404 Not Found`: invalid, unknown, or revoked credential;
- `500 Internal Server Error`: unexpected server failure.

### PATCH /api/subscriptions/manage

- `200 OK`: preferences updated successfully;
- `400 Bad Request`: invalid editable data;
- `404 Not Found`: invalid, unknown, or revoked credential;
- `500 Internal Server Error`: unexpected server failure.

### POST /api/subscriptions/unsubscribe

- `200 OK`: unsubscribe completed successfully;
- `404 Not Found`: invalid, unknown, or already revoked credential;
- `500 Internal Server Error`: unexpected server failure.

## 22. Security Requirements

The Join implementation must follow these minimum principles:

- public DTOs are separate from JPA entities;
- JPA entities are never returned directly;
- Bean Validation is used for incoming DTOs;
- email is normalized before lookup and persistence;
- database email uniqueness is enforced;
- management tokens are cryptographically random;
- raw management tokens are not persisted;
- only token hashes are stored;
- token-bearing requests and authorization headers are not intentionally logged;
- unnecessary PII logging is prohibited;
- internal IDs are not exposed without a concrete requirement;
- secrets are loaded from environment-based configuration rather than hardcoded source;
- CORS is configured according to environment;
- administrative endpoints are outside the public Join API;
- API errors are controlled and sanitized;
- schema changes use Flyway;
- GET requests do not perform state-changing subscription operations.

The management token is a bearer credential and must be treated as sensitive.

## 23. Minimal Privacy Model

Survival72 stores only data needed for the subscription service.

### email

Purpose: deliver Survival72 communications and identify the unique subscription.

### firstName

Purpose: optional basic personalization.

### countryCode

Purpose: support regional relevance as Survival72 expands beyond Costa Rica.

### preferences

Purpose: determine the subscriber selected content topics.

### status and timestamps

Purpose: manage the subscription lifecycle.

### managementTokenHash

Purpose: allow secure subscription management without requiring a password or account.

The MVP does not collect data merely because it may be useful someday.

Unsubscribe stops future communications but does not immediately delete the subscriber record.

A complete retention, deletion, or compliance platform is outside the MVP and must be designed separately if later required.

## 24. Decisions Summary

1. Join is a subscription system, not an account system.
2. Email is required and unique after normalization.
3. First name is optional.
4. Last name is excluded.
5. Country is required.
6. City is excluded until a concrete feature requires it.
7. Religious or church membership information is not collected.
8. The MVP supports four canonical preference values.
9. At least one preference is required.
10. Subscriber states are only `ACTIVE` and `UNSUBSCRIBED`.
11. Duplicate active Join does not create or modify a subscriber.
12. Public Join responses do not reveal whether an email already exists.
13. Rejoin reuses the existing subscriber row.
14. Rejoin replaces editable profile fields and preferences with the new Join request.
15. Rejoin generates a new management token.
16. Unsubscribe preserves the subscriber record and preferences.
17. Unsubscribe revokes the management token.
18. The raw management token is never stored.
19. SHA-256 token hashes are persisted.
20. Management authorization uses a Bearer token.
21. Backend API paths and query strings do not contain the management token.
22. GET never performs unsubscribe.
23. Public API contracts use DTOs rather than entities.
24. Flyway is the migration strategy for new Join schema evolution.
25. Email delivery failure does not automatically roll back subscriber persistence.
26. Double opt-in is outside the MVP.

## 25. Out of Scope

The Join MVP explicitly excludes:

- user login;
- passwords;
- user accounts;
- subscriber dashboard;
- admin panel;
- roles and permissions;
- payments;
- weather API;
- push notifications;
- official real-time emergency alerts;
- advanced newsletter engine;
- complex email automations;
- double opt-in;
- changing email through the management link;
- complete subscriber deletion workflow;
- analytics segmentation platform;
- full subscription event history;
- persistent email retry queues;
- transactional outbox implementation;
- frontend implementation;
- backend implementation;
- secret rotation work;
- compatibility adapters for historical subscription contracts.

## 26. Conceptual Backend Architecture

The target application flow remains:

`JoinController -> JoinService -> SubscriberRepository -> MySQL`

Supporting components may later be introduced for clearly separated concerns such as:

- `TokenService`;
- `EmailService`.

The contract does not require unnecessary abstraction before implementation needs justify it.

## 27. Implementation Plan

Implementation must occur in separate branches after this contract is approved.

### Stage 1 — Database and migration foundation

- add Flyway dependency and configuration;
- define the initial Join migration;
- create or adjust the subscriber persistence model;
- create preference persistence;
- enforce normalized email uniqueness;
- verify migration behavior against the historical database.

No public Join endpoint should be implemented before the persistence contract is stable.

### Stage 2 — Domain and DTO foundation

- define status and preference enums;
- define public DTOs;
- add Bean Validation;
- implement email and country normalization;
- establish the controlled API error structure.

### Stage 3 — Join lifecycle

Status: application/domain lifecycle foundation implemented in
`feat/join-service-foundation`.

Implemented:

- `JoinService` as the transactional application service;
- `JoinCommand` as the internal input model;
- `JoinResult` and `JoinOutcome` as internal lifecycle results;
- new subscriber creation with `ACTIVE` status;
- normalized lowercase email;
- optional trimmed `firstName`, with blank values normalized to `null`;
- uppercase two-letter alphabetic `countryCode`;
- validation requiring at least one preference;
- duplicate active Join handled idempotently without modifying profile,
  preferences, timestamps, or management token;
- rejoin using the same subscriber row;
- rejoin profile and preference replacement;
- rejoin lifecycle timestamps and clearing of `unsubscribedAt`;
- cryptographically random 32-byte opaque management tokens;
- URL-safe Base64 encoding without padding;
- SHA-256 lowercase hexadecimal token hashing;
- persistence of only the token hash;
- raw management token returned only through the internal application result
  for new subscriptions and rejoins;
- transactional persistence through `SubscriberRepository`.

Validated scenarios include new Join, preference persistence, token hashing,
active duplicate behavior, rejoin behavior, normalization, and controlled
validation failures.

The initial HTTP boundary is now implemented through `POST /api/join`.

Implemented at the HTTP boundary:

- `JoinController` delegates to `JoinService`;
- `JoinRequest` is the public request DTO;
- Bean Validation protects required email, valid email format, maximum email
  length, optional `firstName` length, two-letter alphabetic `countryCode`,
  and at least one `SubscriberPreference`;
- request mapping is `JoinRequest -> JoinCommand -> JoinService.join(...)`;
- NEW_SUBSCRIPTION, ACTIVE_DUPLICATE, and REJOINED all return the same public
  `200 OK` response;
- the public response is:
  `{"status":"REQUEST_ACCEPTED","message":"Join request processed."}`;
- raw management tokens, token hashes, internal IDs, `Subscriber`, and
  `JoinOutcome` are not exposed through HTTP;
- invalid Bean Validation payloads and unreadable JSON / invalid enum values
  return a controlled `400 Bad Request`;
- the Join-specific HTTP error handler does not expose stack traces, SQL,
  internal class names, or lifecycle details.

The internal subscription-management service foundation is now implemented.

Implemented internally:

- raw management tokens are hashed with SHA-256 before lookup;
- `SubscriberRepository` resolves subscribers by `managementTokenHash`;
- management authorization never uses email or public IDs;
- only `ACTIVE` subscribers with a valid current token are manageable;
- invalid, unknown, blank, revoked, and non-manageable access resolves through
  the internal `SubscriptionAccessException`;
- `SubscriptionManagementView` exposes only `firstName`, `countryCode`, and
  preferences;
- `UpdateSubscriptionCommand` accepts only `firstName`, `countryCode`, and
  preferences;
- updates normalize `firstName`, uppercase and validate `countryCode`, replace
  preferences completely, and update `updatedAt`;
- profile updates do not change email, status, lifecycle timestamps directly,
  or the management token hash;
- management reads use a read-only transaction and updates use a transactional
  write boundary.

Still pending:

- `GET /api/subscriptions/manage` HTTP boundary;
- `PATCH /api/subscriptions/manage` HTTP boundary;
- unsubscribe HTTP endpoint;
- email integration;
- frontend integration.

### Stage 4 — Subscription management

Internal management authorization and profile update logic are implemented.

Still to implement:

- `GET /api/subscriptions/manage`;
- `PATCH /api/subscriptions/manage`;
- Bearer-token extraction and HTTP mapping;
- public management request/response DTOs;
- HTTP mapping for invalid or revoked token behavior.

### Stage 5 — Unsubscribe

Implement `POST /api/subscriptions/unsubscribe`.

Cover status transition, timestamps, token revocation, and repeated or revoked token behavior.

### Stage 6 — Email integration

Implement:

- welcome email;
- management link;
- unsubscribe link;
- unsubscribe confirmation;
- safe email failure behavior;
- environment-based frontend URL.

### Stage 7 — Tests and hardening

Test at minimum:

- validation;
- normalization;
- uniqueness;
- duplicate Join;
- rejoin;
- token hashing;
- token revocation;
- management authorization;
- preference updates;
- unsubscribe;
- invalid tokens;
- controlled errors;
- email failure behavior.

### Stage 8 — Frontend integration

Frontend implementation occurs only after the backend contract is implemented and validated.

It is not part of this documentation branch.

## 28. First Implementation Block

The first implementation branch after this design is approved should focus exclusively on persistence and migrations.

Proposed branch:

`feat/join-persistence-foundation`

Scope:

1. introduce Flyway safely;
2. inspect the current historical MySQL schema before writing destructive migrations;
3. define the canonical subscriber persistence mapping;
4. define preference persistence;
5. define `ACTIVE` and `UNSUBSCRIBED`;
6. enforce email uniqueness and required constraints;
7. support nullable `management_token_hash` for unsubscribed records;
8. add persistence-level tests;
9. update documentation.

Do not implement JoinController, email sending, management endpoints, or frontend integration in that first block.
