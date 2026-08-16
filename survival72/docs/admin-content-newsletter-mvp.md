# Admin, Content, YouTube and Manual Newsletter MVP

## 1. Objective

Define the minimum viable administrative system needed to give the Survival72 Join flow a real purpose before the planned presentation/event.

This document defines architecture and contracts only.

This branch must not implement production functionality.

The MVP must allow one authorized administrator to:

1. authenticate into a protected admin area;
2. view subscribers;
3. register and manage content;
4. register YouTube videos using a URL or video ID without uploading video files;
5. create manual newsletters;
6. choose newsletter audience using canonical subscriber preferences;
7. prepare a newsletter for sending;
8. manually trigger sending once an email delivery provider is available.

Automation is intentionally postponed.

---

## 2. Current System Context

The canonical subscription system already provides:

- Join;
- subscription management;
- unsubscribe;
- frontend Join;
- frontend Manage;
- frontend Unsubscribe;
- MySQL persistence;
- Flyway migrations;
- canonical subscriber status;
- canonical subscriber preferences;
- opaque management tokens;
- email integration boundaries.

Canonical subscriber preferences are:

- `GENERAL_PREPAREDNESS`
- `EMERGENCY_KIT`
- `EDUCATIONAL_CONTENT`
- `EVENTS_AND_TRAINING`

Canonical subscriber statuses are:

- `ACTIVE`
- `UNSUBSCRIBED`

The existing `subscriber_preferences` table remains the source of truth for subscriber interests.

---

## 3. Legacy Components That Must Not Be Reused

The historical backend contains legacy subscriber and newsletter functionality.

Examples include:

- public-style subscriber listing;
- subscriber mutation using email or numeric IDs;
- physical deletion during cancellation;
- `NewsletterService`;
- scheduled monthly newsletter execution;
- `findAll()` newsletter audience selection;
- legacy `topicsOfInterest`;
- test newsletter endpoints;
- email addresses in URLs;
- subscriber email logging;
- CSV export logic.

The historical newsletter service currently uses a scheduled monthly operation.

Scheduling is globally enabled through Spring scheduling configuration.

These components are incompatible with the canonical Join architecture and must not become the foundation of the new Admin or Newsletter MVP.

The legacy frontend also contains:

- `admin.html`;
- `scripts/admin.js`;
- `styles/admin.css`.

`admin.html` is currently intentionally disabled.

The old JavaScript uses obsolete subscriber endpoints and historical fields and must not be revived as the new admin implementation.

The new admin must be implemented independently.

---

## 4. Scope

### Included

The MVP defines:

- single-administrator authentication;
- protected admin API;
- minimal admin UI;
- subscriber read-only listing;
- content creation and management;
- YouTube video registration;
- newsletter draft creation;
- newsletter audience definition;
- newsletter audience preview/count;
- ready-to-send state;
- manual send operation contract;
- database model;
- security boundaries.

### Excluded

The following remain outside the MVP:

- automated newsletters;
- scheduled newsletters;
- scheduler-based content delivery;
- real-time alerts;
- weather automation;
- push notifications;
- advanced analytics;
- multiple admin roles;
- permissions matrix;
- payments;
- visual drag-and-drop editors;
- uploaded video files;
- YouTube API integration;
- automatic YouTube metadata import;
- queue infrastructure;
- outbox infrastructure;
- retry workers;
- general legacy refactoring;
- subscriber editing from the admin;
- subscriber deletion from the admin.

---

## 5. Minimal Admin Screens

The admin requires only five functional areas.

### 5.1 Login

Purpose:

Authenticate the single authorized Survival72 administrator.

The login page must not make authorization decisions by itself.

The backend remains the authority.

### 5.2 Dashboard

The dashboard should remain intentionally simple.

Useful summary information may include:

- active subscriber count;
- published content count;
- draft content count;
- draft newsletter count;
- newsletters ready to send.

No graphs or sophisticated analytics are required.

### 5.3 Subscribers

The subscriber screen is read-only for the MVP.

The backend read model is implemented through:

`GET /api/admin/subscribers`

The endpoint:

- requires an authenticated Admin session through the existing Spring Security foundation;
- uses database-backed Spring Data pagination;
- accepts `page` starting at `0`;
- uses a default `size` of `20`;
- rejects sizes above `100`;
- supports optional `status` and `preference` filters;
- combines both filters with `status AND preference` semantics;
- orders results by `subscribedAt DESC, id DESC`;
- avoids duplicate subscribers when filtering through `subscriber_preferences`;
- returns controlled `400 BAD_REQUEST` responses for invalid query parameters.

The administrative DTO exposes only:

- id;
- email;
- first name;
- country code;
- status;
- preferences;
- subscribed timestamp;
- updated timestamp;
- unsubscribed timestamp.

The endpoint does not serialize the `Subscriber` JPA entity.

The MVP deliberately does not expose:

- `managementTokenHash`;
- raw management tokens;
- session identifiers;
- password material;
- legacy transient subscriber fields;
- unnecessary persistence internals.

Pagination must be used instead of loading every subscriber at once.

### 5.4 Content / Videos

The admin can:

- list content;
- create content;
- edit content;
- publish or unpublish content.

Videos are handled in this same section because a YouTube video is a content item.

### 5.5 Newsletters

The admin can:

- list newsletters;
- create a draft;
- edit subject and content;
- select audience preferences;
- preview audience size;
- mark the newsletter ready to send;
- manually trigger sending when delivery infrastructure is available.

---

## 6. Admin Authentication Approach

### Decision

Use Spring Security with backend-controlled authentication.

The MVP supports exactly one administrator.

Do not build a multi-user administration system yet.

### Credential source

The administrator username and password credential must come from environment configuration or another external secret source.

The password must never be stored in:

- frontend JavaScript;
- HTML;
- Git;
- application source code;
- plaintext database fields.

A one-way password hash such as BCrypt should be used by the backend.

### Session model

The preferred MVP model is a server-controlled authenticated session using Spring Security.

The browser receives only the session mechanism required by Spring Security.

Authorization must be enforced on every `/api/admin/**` endpoint.

The frontend must never decide whether an administrator is authorized.

### Why session authentication

For one administrator, session authentication avoids unnecessary complexity such as:

- JWT issuance;
- refresh tokens;
- role hierarchies;
- persistent admin API tokens.

### CSRF

Because the admin performs authenticated state-changing operations, CSRF protection must remain enabled.

The frontend must use the CSRF mechanism defined by Spring Security rather than disabling CSRF globally.

### Cookies

Production session cookies must use secure attributes appropriate for the deployment, including HTTPS.

The final frontend/backend deployment topology must be considered when configuring SameSite and CORS.

Prefer a same-origin or reverse-proxied admin deployment when practical because it reduces cross-origin session complexity.

### Logout

The admin must have an explicit logout operation that invalidates the backend session.

### Admin database table

An `admin_user` table is not required for the first MVP.

Reason:

- there is only one administrator;
- no self-service account creation is needed;
- no role system is needed;
- no account management UI is needed.

If multiple administrators are needed later, authentication can migrate to persistent admin accounts without changing the Content or Newsletter domain models.

---

## 7. Content Model

### Decision

Use a dedicated `ContentItem` concept.

Newsletter must not be represented as ordinary content.

### Initial content types

The MVP requires:

- `VIDEO`
- `ARTICLE`

`ARTICLE` also covers simple updates during the MVP.

Creating a separate `UPDATE` type is unnecessary until there is a meaningful behavioral difference.

### Content fields

Conceptually, a content item contains:

- internal ID;
- title;
- short description;
- type;
- status;
- content date;
- created timestamp;
- updated timestamp;
- published timestamp when applicable;
- zero or more target preferences;
- YouTube video ID when type is `VIDEO`.

### Content status

Use:

- `DRAFT`
- `PUBLISHED`
- `ARCHIVED`

`DRAFT` means not publicly available.

`PUBLISHED` means available to the public/product experience.

`ARCHIVED` preserves historical content without presenting it as active content.

Deleting content is not required for the MVP.

### Content audience

A content item may optionally be associated with one or more canonical subscriber preferences.

This metadata allows content to be categorized and reused by future newsletter or recommendation features.

Content preference metadata does not itself trigger automatic delivery.

---

## 8. YouTube Model

### Decision

YouTube videos are `ContentItem` records with:

`type = VIDEO`.

No video binary is stored by Survival72.

### Accepted admin input

The admin should be allowed to enter either:

- a YouTube video URL;
- a YouTube video ID.

### Persistence

The backend should normalize valid YouTube input and store the canonical YouTube video ID.

Example conceptual field:

`youtube_video_id`

The application can derive standard watch or embed URLs from that ID.

Storing the canonical ID avoids persisting multiple URL formats for the same video.

### Minimum video metadata

A YouTube content item contains:

- title;
- short description;
- `VIDEO` type;
- content status;
- content date;
- one or more optional preferences;
- canonical YouTube video ID.

### Validation

The backend must validate that:

- a `VIDEO` has a valid YouTube video ID;
- a non-video content item does not require a YouTube ID.

### Explicit exclusions

The MVP does not:

- upload video files;
- download YouTube videos;
- call the YouTube API;
- automatically retrieve thumbnails;
- automatically retrieve duration;
- automatically retrieve channel information;
- automatically retrieve title or description.

These can be considered later.

---

## 9. Why Newsletter Is a Separate Entity

### Decision

Newsletter must be a separate domain entity rather than a `ContentItem`.

Although both contain text, newsletter has lifecycle and operational behavior that ordinary content does not.

Newsletter requires concepts such as:

- email subject;
- audience selection;
- audience resolution;
- ready-to-send state;
- manual send action;
- sent timestamp;
- delivery lifecycle.

Keeping Newsletter separate avoids forcing email-delivery concepts into public content.

Content can later be referenced from a newsletter, but that relationship is not required for the first MVP.

---

## 10. Newsletter Model

### Minimum fields

Conceptually, a newsletter contains:

- internal ID;
- subject;
- body/content;
- status;
- created timestamp;
- updated timestamp;
- ready timestamp;
- sent timestamp;
- selected audience preferences.

### Newsletter status

Use:

- `DRAFT`
- `READY_TO_SEND`
- `SENT`

Optional future delivery states such as `SENDING`, `PARTIALLY_SENT`, or `FAILED` should not be introduced until actual delivery infrastructure requires them.

### DRAFT

Editable newsletter.

The administrator may change:

- subject;
- body;
- audience preferences.

### READY_TO_SEND

The administrator has reviewed the newsletter and intentionally marked it ready.

This prevents an unfinished draft from being sent accidentally.

A newsletter should not be editable while marked `READY_TO_SEND` without explicitly returning it to `DRAFT`.

### SENT

The manual send operation completed according to the future delivery contract.

A `SENT` newsletter is immutable for MVP purposes.

If similar content needs to be sent again, create another newsletter.

---

## 11. Newsletter Audience Selection

### Decision

Use `ANY selected preference` semantics.

Example:

If a newsletter selects:

- `EMERGENCY_KIT`
- `EDUCATIONAL_CONTENT`

then an ACTIVE subscriber matching either preference is included.

### Why ANY

`ANY` is preferable for the MVP because it is:

- easy for an administrator to understand;
- useful for broad preparedness communications;
- simpler to query;
- less likely to accidentally create an extremely small audience.

`ALL selected preferences` can be evaluated later if a real use case appears.

### Subscriber eligibility

A newsletter recipient must satisfy both conditions:

1. subscriber status is `ACTIVE`;
2. subscriber has at least one selected preference.

An `UNSUBSCRIBED` subscriber must never be returned.

### Duplicate prevention

A subscriber matching multiple selected preferences must appear only once in the resolved audience.

Database queries must therefore use distinct subscriber results.

### Empty audience selection

For MVP, a newsletter must contain at least one selected preference.

There is no implicit "send to everyone" mode.

If Survival72 later needs a global broadcast capability, it should be introduced explicitly rather than treating an empty preference list as global audience.

### Resolution time

The authoritative recipient list should be resolved at manual send time.

This ensures subscribers who unsubscribe before the send operation are excluded.

The admin may request an audience preview before sending, but the preview is informational and may change.

---

## 12. Subscriber Query Requirements

The existing canonical repository currently supports lookup by:

- email;
- management token hash.

The Admin/Newsletter implementation will require new query behavior.

Conceptually:

- list subscribers with pagination;
- filter by subscriber status;
- optionally filter by preference;
- resolve distinct ACTIVE subscribers matching ANY selected preferences;
- count distinct ACTIVE subscribers matching ANY selected preferences.

Newsletter code must not use unrestricted `findAll()` as its delivery audience.

---

## 13. Conceptual Database Tables

The exact migration names and SQL belong to the implementation phase.

### Existing tables reused

#### `subscriber`

Existing canonical subscriber record.

No new subscriber table is required.

#### `subscriber_preferences`

Existing canonical subscriber preference relationship.

This remains the source of truth for newsletter audience resolution.

### Proposed new table: `content_item`

Conceptual columns:

- `id`
- `title`
- `short_description`
- `type`
- `status`
- `content_date`
- `youtube_video_id`
- `created_at`
- `updated_at`
- `published_at`

`youtube_video_id` is nullable generally but required by application validation when `type = VIDEO`.

### Proposed new table: `content_item_preferences`

Conceptual columns:

- `content_item_id`
- `preference`

Use a composite unique or primary key so the same preference cannot be assigned twice.

Values must use the existing canonical preference enum values.

### Proposed new table: `newsletter`

Conceptual columns:

- `id`
- `subject`
- `body`
- `status`
- `created_at`
- `updated_at`
- `ready_at`
- `sent_at`

### Proposed new table: `newsletter_preferences`

Conceptual columns:

- `newsletter_id`
- `preference`

Use a composite unique or primary key.

Values must use the same canonical preferences as subscribers.

### Admin table

No `admin_user` table is proposed for the first MVP.

The single administrator credential is configured externally.

Future multi-admin functionality may add persistent admin identity tables later.

---

## 14. Conceptual Admin API

All endpoints below are conceptual and must require authenticated admin access unless explicitly identified as an authentication endpoint.

The exact DTOs and error contracts belong to implementation.

### Authentication

Conceptually:

- `POST /api/admin/auth/login`
- `POST /api/admin/auth/logout`
- `GET /api/admin/auth/session`

Purpose:

- establish authenticated admin session;
- destroy session;
- allow frontend to determine whether the backend session is authenticated.

### Dashboard

- `GET /api/admin/dashboard`

May return simple operational counts only.

### Subscribers

- `GET /api/admin/subscribers`

Possible query parameters:

- `page`
- `size`
- `status`
- `preference`

The endpoint returns controlled admin DTOs.

It must not return:

- `managementTokenHash`;
- raw management tokens;
- JPA entities directly.

No subscriber mutation endpoint is required for this admin MVP.

### Content

- `GET /api/admin/content`
- `GET /api/admin/content/{id}`
- `POST /api/admin/content`
- `PATCH /api/admin/content/{id}`

State transitions can initially be represented by validated PATCH updates.

If explicit commands prove clearer during implementation, publish/archive endpoints may be introduced later.

### Newsletters

- `GET /api/admin/newsletters`
- `GET /api/admin/newsletters/{id}`
- `POST /api/admin/newsletters`
- `PATCH /api/admin/newsletters/{id}`
- `GET /api/admin/newsletters/{id}/audience-preview`
- `POST /api/admin/newsletters/{id}/ready`
- `POST /api/admin/newsletters/{id}/send`

### Audience preview

The preview should at minimum return:

- distinct eligible recipient count.

Returning an entire email list is not required for the initial MVP.

### Send endpoint

`POST /api/admin/newsletters/{id}/send`

is a manual administrative command.

It must never be a `GET`.

It must require:

- authenticated admin;
- CSRF protection where applicable;
- newsletter status `READY_TO_SEND`.

The final delivery implementation must resolve the ACTIVE audience again immediately before sending.

---

## 15. Security Rules

The following rules are mandatory.

### Admin authorization

Every admin API endpoint must be protected by backend authentication and authorization.

Do not rely on:

- hiding an HTML file;
- JavaScript-only login checks;
- route obscurity;
- hardcoded frontend passwords.

### Credentials

Never commit admin credentials or password hashes that are intended to be secret.

Secrets belong in environment configuration or a proper secret store.

### Existing configuration risk

The current project configuration contains a historical hardcoded database credential.

That credential must be treated as exposed and moved/rotated in a separate security cleanup block before production.

This documentation branch does not modify that configuration.

### Subscriber privacy

Admin APIs may expose subscriber information required for administration, such as email, but those APIs must be authenticated.

Public subscription APIs must remain separated from admin subscriber APIs.

### Tokens

Never expose:

- management token hashes;
- raw management tokens;
- admin session identifiers in API response bodies.

Never log authentication credentials or management tokens.

### Newsletter recipient logging

Do not log every recipient email during newsletter delivery.

Use aggregate operational logging where possible.

### CSRF

Do not disable CSRF globally to make the admin easier to implement.

Use the Spring Security CSRF mechanism for authenticated state-changing operations.

### CORS

Current development CORS is explicitly limited to local frontend origins and allows credentials.

Production CORS must use explicit trusted origins from environment configuration.

Never use wildcard origins with authenticated admin requests.

### HTTPS

Production admin authentication and newsletter administration require HTTPS.

---

## 16. What Is Manual in the MVP

Manual means the administrator intentionally performs each operation.

The admin manually:

1. logs in;
2. creates or edits content;
3. registers a YouTube video;
4. creates a newsletter;
5. writes the newsletter subject and body;
6. selects one or more audience preferences;
7. reviews audience size;
8. marks the newsletter `READY_TO_SEND`;
9. explicitly starts the send operation when delivery support is available.

No schedule determines when a newsletter is sent.

No newsletter should be sent merely because a date was reached.

---

## 17. Email Provider Boundary

Newsletter creation and audience selection must not depend on a specific email provider.

The Newsletter domain owns:

- subject;
- body;
- audience;
- status;
- send eligibility.

A future delivery component owns:

- provider interaction;
- message transmission;
- provider-specific errors.

The manual send endpoint should eventually call this delivery boundary.

Until the production delivery provider is ready, Survival72 must still be able to:

- create newsletters;
- edit drafts;
- select audience;
- preview eligible recipient counts;
- mark newsletters ready.

Email-provider availability must not block these administrative features.

---

## 18. What Will Be Automated Later

The following may be considered after the manual workflow is proven useful:

- scheduled newsletters;
- scheduled content releases;
- automatic audience campaigns;
- queue-based delivery;
- outbox pattern;
- retries;
- delivery batching;
- bounce handling;
- provider webhooks;
- open/click metrics;
- automatic content recommendations;
- automated newsletter generation;
- YouTube API synchronization.

Automation must build on the manual Newsletter model rather than replacing it.

---

## 19. User Flows

### 19.1 Admin login

1. Administrator opens the admin login.
2. Credentials are submitted to the backend.
3. Spring Security validates the configured administrator credential.
4. Backend establishes an authenticated session.
5. Admin UI becomes accessible.
6. Every admin API request is independently protected by the backend.
7. Logout invalidates the session.

### 19.2 View subscribers

1. Administrator opens Subscribers.
2. Frontend requests a paginated admin subscriber endpoint.
3. Backend verifies admin authentication.
4. Backend returns controlled subscriber DTOs.
5. Administrator may filter by status or preference.

No subscriber mutation occurs in this flow.

### 19.3 Create article/update

1. Administrator opens Content.
2. Creates a content item.
3. Enters title and short description.
4. Selects `ARTICLE`.
5. Selects optional preferences.
6. Saves as `DRAFT`.
7. Administrator later changes it to `PUBLISHED`.

### 19.4 Register YouTube video

1. Administrator opens Content.
2. Creates a content item.
3. Selects `VIDEO`.
4. Enters title and description.
5. Enters YouTube URL or video ID.
6. Backend validates and normalizes the value to a canonical video ID.
7. Administrator selects preferences.
8. Saves as `DRAFT` or publishes it.

No video file is uploaded.

### 19.5 Prepare newsletter

1. Administrator opens Newsletters.
2. Creates a new draft.
3. Enters subject.
4. Enters newsletter body.
5. Selects one or more canonical preferences.
6. Saves the draft.
7. Admin requests audience preview.
8. Backend counts distinct ACTIVE subscribers matching ANY selected preference.
9. Administrator reviews the newsletter.
10. Administrator marks it `READY_TO_SEND`.

### 19.6 Manual send

1. Administrator opens a `READY_TO_SEND` newsletter.
2. Administrator explicitly chooses Send.
3. Backend verifies authentication and newsletter state.
4. Backend resolves ACTIVE matching subscribers again.
5. Backend delegates transmission to the email delivery boundary.
6. Newsletter becomes `SENT` only when the future delivery contract reports successful completion according to the implementation policy.

There is no scheduler involved.

---

## 20. Dashboard MVP

The dashboard should answer only basic operational questions.

Possible values:

- active subscribers;
- draft content;
- published content;
- draft newsletters;
- ready newsletters.

No charts are necessary.

The historical simulated subscriber graph must not be recreated.

---

## 21. Implementation Order

Implementation should be incremental and each block should have its own branch, tests, documentation update, commit, merge, and push.

### Block 1 — Admin security foundation

Recommended branch:

`feat/admin-security-foundation`

Scope:

- add Spring Security;
- external single-admin credentials;
- secure `/api/admin/**`;
- login/session/logout boundary;
- CSRF strategy;
- admin security tests;
- environment-based trusted admin frontend origin where necessary;
- documentation.

Do not add Content or Newsletter yet.

### Block 2 — Admin subscriber read model

Recommended branch:

`feat/admin-subscriber-read-model`

Scope:

- admin subscriber DTO;
- paginated subscriber listing;
- status filter;
- preference filter;
- authenticated endpoint;
- repository queries;
- tests.

No subscriber editing or deleting.

### Block 3 — Content persistence foundation

Recommended branch:

`feat/content-persistence-foundation`

Scope:

- Flyway migration;
- content entity/model;
- content preference relationship;
- enums;
- repository;
- persistence tests.

No HTTP UI yet.

### Block 4 — Content admin HTTP

Recommended branch:

`feat/content-admin-http`

Scope:

- admin content DTOs;
- create/read/update;
- publish/archive lifecycle;
- YouTube input normalization and validation;
- protected endpoints;
- tests.

### Block 5 — Admin frontend shell

Recommended branch:

`feat/admin-frontend-foundation`

Scope:

- login;
- simple dashboard;
- navigation;
- subscribers screen;
- content screen;
- secure authenticated requests;
- accessible responsive UI.

No newsletter UI yet.

### Block 6 — Newsletter persistence foundation

Recommended branch:

`feat/newsletter-persistence-foundation`

Scope:

- Flyway migration;
- newsletter model;
- newsletter preferences;
- statuses;
- repository;
- persistence tests.

### Block 7 — Newsletter audience service

Recommended branch:

`feat/newsletter-audience-foundation`

Scope:

- ACTIVE-only audience query;
- ANY-preference semantics;
- distinct subscribers;
- audience count;
- tests.

No sending yet.

### Block 8 — Newsletter admin HTTP

Recommended branch:

`feat/newsletter-admin-http`

Scope:

- create/edit draft;
- audience selection;
- audience preview;
- mark ready;
- protected endpoints;
- tests.

The send contract may exist only if its provider boundary is explicit.

### Block 9 — Newsletter admin frontend

Recommended branch:

`feat/newsletter-admin-frontend`

Scope:

- newsletter list;
- draft form;
- preference selector;
- audience preview;
- ready action;
- manual send control when backend delivery is available.

### Block 10 — Manual newsletter delivery

Recommended future branch:

`feat/newsletter-manual-delivery`

Scope:

- delivery provider adapter;
- explicit manual send operation;
- send-time audience resolution;
- newsletter completion state;
- failure behavior;
- tests.

No scheduler.

---

## 22. First Exact Implementation Block

The first implementation block after this design branch should be:

`feat/admin-security-foundation`

It should implement only the secure backend admin boundary.

Required outcomes:

1. Spring Security dependency is introduced.
2. One administrator is configured through external environment values.
3. Password verification uses a secure one-way hash.
4. `/api/admin/**` is inaccessible without authenticated admin state.
5. login/session/logout behavior is defined.
6. CSRF remains enabled for authenticated state-changing requests.
7. public Join, Manage and Unsubscribe contracts continue working.
8. security behavior is covered by tests.
9. no Content, Newsletter, subscriber admin UI or analytics are implemented in this block.

This ordering prevents building any new admin endpoint before there is a real backend security boundary.

---

## 23. Final MVP Decisions

### Admin

One administrator only.

Authentication and authorization are enforced by Spring Security in the backend.

No frontend-only security.

No multi-role system.

No admin database table initially.

### Content

Dedicated `ContentItem` domain.

Initial types:

- `VIDEO`
- `ARTICLE`

Initial states:

- `DRAFT`
- `PUBLISHED`
- `ARCHIVED`

### Video

A YouTube video is a `ContentItem` with `type = VIDEO`.

Store canonical YouTube video ID only.

Do not store video binaries.

### Newsletter

Newsletter is a separate entity.

Statuses:

- `DRAFT`
- `READY_TO_SEND`
- `SENT`

### Audience

Use canonical subscriber preferences.

Eligible subscriber:

`ACTIVE AND matches ANY selected preference`.

Always deduplicate subscribers.

At least one audience preference is required.

### Automation

None in the MVP.

The current historical scheduled newsletter is not part of the new architecture.

### Delivery

Newsletter preparation is independent of the eventual email delivery provider.

The final send action is manual and provider-backed.

Automation, queues, retries and scheduling remain future work.

---

## 24. Admin Security Foundation — Implementation Status

Implemented on branch:

`feat/admin-security-foundation`

The backend security boundary defined in Block 1 is now implemented.

Implemented:

- Spring Security dependency and Spring Security test support;
- one externally configured administrator;
- `ADMIN_USERNAME` as the external username source;
- `ADMIN_PASSWORD_HASH` as the external BCrypt password-hash source;
- no new plaintext administrator password or committed secret hash;
- backend authorization for `/api/admin/**`;
- HTTP session authentication without JWT;
- `POST /api/admin/auth/login`;
- `GET /api/admin/auth/session`;
- `POST /api/admin/auth/logout`;
- explicit persistence of the authenticated Spring Security context;
- session fixation protection by changing the session ID after successful login;
- explicit session invalidation on logout;
- CSRF enabled for authenticated administrative mutations;
- CSRF token storage through `HttpSessionCsrfTokenRepository`;
- the session endpoint exposes the current CSRF token and header name required by
  the future Admin frontend;
- controlled neutral responses for invalid administrator credentials;
- controlled unauthenticated response for protected Admin routes;
- local CORS remains limited to the existing explicit localhost frontend origins
  with credentials enabled;
- session cookie `HttpOnly=true`;
- configurable `SameSite` through `SESSION_COOKIE_SAME_SITE`, defaulting to
  `lax` for local development;
- configurable session-cookie `Secure` through `SESSION_COOKIE_SECURE`,
  defaulting to `false` locally and requiring `true` for HTTPS production;
- the existing public Join, Management and Unsubscribe contracts remain outside
  Admin session authentication;
- a minimal `/api/admin/security-check` probe exists only to validate protected
  read/mutation behavior and CSRF at this foundation stage;
- dedicated Admin security tests cover authentication, session, logout,
  protected routes, CSRF and preservation of public subscription boundaries.

Explicitly not implemented in this block:

- Content;
- Videos;
- Newsletter;
- subscriber Admin UI;
- dashboard UI or operational dashboard API;
- analytics;
- automation;
- general legacy cleanup;
- production CORS domain configuration;
- Admin frontend.

The historical database credential already documented as exposed remains outside
this block and must still be rotated and externalized before production.
