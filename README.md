# Studio SaaS

예약 기반 소규모 서비스 사업장을 위한 맞춤형 운영 SaaS.

이 저장소는 **레슨/스튜디오형 사업장**과 **뷰티/예약형 사업장**을 하나의 공통 운영 코어 위에서 지원하는 멀티테넌트 SaaS를 목표로 한다.

## Product modes

- `LESSON`: 댄스, 필라테스, 요가, PT, 폴댄스, 보컬/음악, 기타 레슨
- `BEAUTY`: 네일, 속눈썹, 붙임머리, 왁싱, 1인 미용, 기타 뷰티

두 모드는 **하위 업종, 메뉴, 대시보드, 기능 노출이 분리**되며 공통 데이터 개념만 공유한다.

## Repository structure

```text
studio-saas/
├── AGENTS.md
├── README.md
├── docs/
├── frontend/
├── backend/
├── docker-compose.yml
└── .env.example
```

## Source of truth

개발 전 반드시 다음 문서를 읽는다.

1. `docs/PRODUCT.md`
2. `docs/DOMAIN_RULES.md`
3. `docs/FEATURES.md`
4. `docs/ONBOARDING.md`
5. `docs/IA.md`
6. `docs/DATA_MODEL.md`
7. `docs/ARCHITECTURE.md`
8. `docs/AUTH.md`
9. `docs/BOOKING.md`
10. `docs/TESTING.md`

상충하는 요구사항이 있을 경우 임의 구현하지 말고 문서의 결정을 우선한다.


## Operator identity and repository foundation

The landing design is preserved under `frontend/`. Phase 2 implements real operator
authentication, Studio membership and default Staff creation. Phase 3 adds category-safe
onboarding and persistent configuration through Flyway V2. Phases 4–6 add Customer, manual
Payment and common manual 1:1 Booking operations through Flyway V3–V5. Specialized lesson
and beauty operations remain outside this scope. Flyway V1 remains the identity and tenant foundation.

### Requirements

- JDK 21 (set `JAVA_HOME` to a JDK 21 installation)
- Node.js 22 and npm
- Docker Engine / Docker Desktop with Compose v2, running
- Network access for initial Gradle/npm dependencies and container images

The Gradle wrapper and frontend package lock belong to this repository. No other
project's dependencies are needed. QueryDSL will be added when domain queries exist;
no speculative query infrastructure is created. Existing CSS components are retained;
Tailwind utilities are enabled without Preflight to preserve the current design.
No unused shadcn components are generated.

### Local infrastructure and backend

From the repository root:

```sh
cp .env.example .env
```

Fill `.env` with local values. Set `POSTGRES_DB` and `POSTGRES_USER` to your chosen
local database/account names. Set separate, nonempty local `POSTGRES_PASSWORD` and
`REDIS_PASSWORD` values. Set `DATABASE_URL` to
`jdbc:postgresql://localhost:5432/<your-database-name>` (match `POSTGRES_PORT` if changed).
Set `SESSION_COOKIE_SECURE=false` only for local HTTP. Production requires HTTPS and
`SESSION_COOKIE_SECURE=true`. Never commit `.env`; quote values with shell-special
characters because the following command sources this trusted local file.

```sh
docker compose up -d --wait
set -a
. ./.env
set +a
cd backend
./gradlew bootRun
```

Compose reads the root `.env`; Spring reads exported environment variables. Spring
does not automatically load the root `.env` file. Both services bind to loopback only
and use named volumes. `docker compose stop` preserves data. Changing database
initialization credentials does not modify an already initialized PostgreSQL volume.

```sh
curl --fail http://localhost:8080/actuator/health
```

Health returns `UP` when PostgreSQL and Redis are reachable, without component details.
Operator APIs use Redis-backed Spring Session and CSRF. There is no generated demo
user or password. Configure OAuth credentials below to enable each provider.

### Frontend

In a separate terminal:

```sh
cd frontend
npm ci
npm run dev
```

Visit `http://localhost:3000`. `/signup`, `/login`, verification/reset screens and `/app`
use real backend sessions. After Studio creation, `/onboarding` collects and saves its
configuration. Completion opens `/app`; `/app/settings` edits the saved configuration
according to OWNER/MANAGER/STAFF permissions. Legacy business prototype routes remain behind the demo gate.

Optional, local visual preview only:

```sh
NEXT_PUBLIC_ENABLE_DEMO=true npm run dev
```

This build-time flag enables preserved prototype screens under `src/app/(demo)`.
Their components, fixtures, types and browser state are isolated under `src/demo`.
They are not production business logic. Do not enter real customer data. Legacy
mixed-category browser state is not imported; the isolated demo uses a new storage key.
Do not enable the flag for a production deployment.

### Repository-owned validation

Backend tests require Docker and start disposable PostgreSQL/Redis Testcontainers;
they do not use the Compose database or require `.env`. Tests fail rather than skip
when Docker is unavailable.

```sh
cd backend
./gradlew test
```

They cover Spring context startup, PostgreSQL connectivity, Flyway validation and
the current schema through the Phase 7–8 V10 integrity migration, Redis/session round-trip, public health privacy, authentication,
token expiry/replay, session revocation, OAuth identity isolation, CSRF, tenant authorization
and atomic Studio creation/onboarding, configuration role restrictions, category dependencies
and concurrent configuration writes. Operational tests cover customer archival, payment state
transitions and refunds, tenant isolation, booking/block conflicts, idempotent retries and rollback,
including concurrent booking, block, archive and refund commands.

```sh
cd frontend
npm run typecheck
npm run test
npm run build
npx playwright install chromium --no-shell
npm run test:e2e
```

Run browser tests against a default build (`NEXT_PUBLIC_ENABLE_DEMO` unset or false),
with JDK 21 selected and Docker running. Playwright starts a test-only Spring server on
8080 using disposable PostgreSQL/Redis Testcontainers, and the built frontend on 31741.
Both ports must be free; existing servers are never reused. Build with the default
`BACKEND_BASE_URL=http://127.0.0.1:8080`. The tests cover desktop/mobile signup, local
verification delivery, login, Studio creation, password reset, session revocation, logout,
both category onboarding flows, configuration reload/update, route guards, landing previews
and the demo gate, plus customer, payment/refund, manual booking and time-block flows in both
categories. The LESSON suite also covers pass products, private/group enrollment, cycle renewal,
private completion and cancellation, group booking and bulk attendance. It never uses the Compose
database or real OAuth.
Playwright builds and serves an ignored `.next-e2e` directory so an IDE development server cannot
rewrite the production chunks during the suite.
Test mail is written to ignored `backend/.local-mail/e2e/`. No external email is sent.

From the root after filling `.env`:

```sh
docker compose config --quiet
```

Historical screenshots and manual prototype verification in `artifacts/` describe the
old prototype, not the production acceptance suite. Authoritative product requirements
remain under `docs/`. Phase 7–8 LESSON products, enrollment, cycles, classes, private lessons,
group bookings and attendance are implemented; Phase 9 BEAUTY specialization remains deferred.

### Customer, payment and booking operations

After onboarding, OWNER/MANAGER can use `/app/customers`, `/app/payments` and `/app/bookings`.
Only OWNER can record a full refund. STAFF sees only assigned bookings and their customer names
and phone numbers, without mutation controls, customer notes or payment access.
Customer archival preserves history and is rejected while pending/confirmed bookings exist.
Payment records are manual KRW records, not payment-provider transactions. Current bookings
are explicitly marked manual 1:1 entries without a pass or beauty service; times use the Studio
timezone. See `docs/API_CONVENTIONS.md` for API routes and required idempotency headers.

### LESSON products and operations

For an ACTIVE LESSON Studio, OWNER/MANAGER can manage `/app/lesson/pass-products`,
`/app/lesson/enrollments` and `/app/lesson/classes`. Private lesson booking is integrated into
`/app/bookings`; group booking and bulk attendance are available under `/app/lesson/attendance`
when the matching capabilities are enabled. STAFF mutation access is limited to assigned private
lesson outcomes and attendance for classes they instruct.


## Phase 2 authentication setup

Use one browser origin consistently: `APP_BASE_URL=http://localhost:3000` locally.
Next.js forwards `/api/v1/*`, `/oauth2/*` and `/login/oauth2/*` to `BACKEND_BASE_URL`.
The browser uses same-origin requests with credentials; no permissive CORS or browser
JWT/localStorage authentication is configured. `BACKEND_BASE_URL` is a frontend build
setting. Production must use the intended HTTPS `APP_BASE_URL`, secure cookies, and
appropriate ingress routing. Do not expose a local-profile server publicly.

### Local email verification and password reset

Set `SPRING_PROFILES_ACTIVE=local` only for local development, then restart the backend.
Sign up at `/signup`. The local delivery adapter writes a JSON message containing the
verification URL to `backend/.local-mail/` (when launched from `backend/`). Open the URL
in the browser and explicitly confirm verification, then log in. Password reset works
through `/forgot-password` in the same way. These files emulate delivery, contain secrets,
have owner-only directory/file permissions (POSIX), and are ignored by Git. Remove them
when no longer needed; never share them or serve that directory. Tokens are not logged
or returned by public auth APIs. The local adapter does not send external messages.

Without the local/test profile, the unconfigured delivery adapter fails closed for signup;
reset/resend requests retain the generic response. Actual external email delivery remains
outside this phase. A real transport adapter must be configured before public email signup
is enabled; do not enable the local adapter in production.

Implementation-local security settings:

- EMAIL identity: trimmed, lower-case email; OAuth email is nullable and not globally unique.
- Password: at least 12 Unicode code points, at most 72 UTF-8 bytes; BCrypt cost 12.
- Verification token TTL: 24 hours; reset token TTL: 30 minutes (`app.auth` configuration).
- Tokens: 32 random bytes, SHA-256 hashes in PostgreSQL, transactional single consumption.
- Confirmation consumes all outstanding tokens of the same purpose for that account.
- Pending EMAIL accounts cannot log in or create a Studio (ADR-049).
- Sessions: 12-hour inactivity, ID rotation on authentication, CSRF refresh after login,
  invalidation on logout and password reset. PostgreSQL `security_version` checks reject
  stale sessions even if Redis deletion races with an in-flight request.
- Auth commands and OAuth initiation: atomic Redis limit, 30 requests/minute per socket
  source address, fail closed on Redis failure. The Next.js proxy shares an aggregate
  source limit. Client forwarding headers are not trusted; tune this limit and configure
  trusted ingress rate limiting before a public deployment.

Links carry tokens in URL fragments to keep them out of server request URLs. The frontend
removes the fragment on load and never stores the token in localStorage. Verification/reset
requires an explicit POST with a fresh server-issued CSRF token.

### Google and Kakao

Set each provider's client ID and secret in the ignored root `.env` and export them before
starting the backend. Empty credentials disable that provider; the UI shows its disabled
button rather than claiming login works. Kakao uses the REST API key as client ID and
requires client-secret activation for this configuration.

Register these exact callback URLs (replace the origin for each environment):

```text
http://localhost:3000/login/oauth2/code/google
http://localhost:3000/login/oauth2/code/kakao
```

Google uses Spring's OpenID Connect configuration. Kakao uses authorization-code login,
`client_secret_post`, the `/v2/user/me` profile and `profile_nickname` consent. Missing
Kakao email is supported. A new identity missing a usable name is rejected with a profile
consent message; no invented name or automatic email-based linking is used. Existing
identities resolve by `(provider, provider_user_id)`. Provider tokens are discarded after
login; only the internal principal is retained in the server session.

Provider references: [Spring OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
and [Kakao REST API](https://developers.kakao.com/docs/en/kakaologin/rest-api).
Automated tests validate identity mapping, concurrency, state rejection and session behavior
without provider network calls. Actual provider login requires configured credentials and
provider-console consent/callback settings.

### Studio and API boundaries

`/app` loads `/api/v1/auth/me` and permits minimal Studio creation/selection. A Studio starts
in `PRE_ONBOARDING` with no category/type (ADR-050). Creation atomically inserts Studio,
OWNER membership and default Staff. Slugs normalize to lower case and accept 3–63 ASCII
letters/digits with single internal hyphens. Database collisions return 409; clients choose
another slug. Timezones must be recognized IANA zone IDs. Category is selected in the subsequent
Phase 3 onboarding flow, not in Studio creation. No operational metrics are fabricated.

Active Studio selection is stored in the server session, revalidated against active
memberships, and automatically chosen when exactly one membership exists. Services obtain
`AuthorizedStudioContext` from `StudioAuthorization`; never construct authorization from
client input. The minimal role guard supports OWNER operations without a permission framework.
Staff's composite foreign key enforces membership in the same Studio.

Auth API: GET `csrf`, `providers`, `me`; POST `signup`, `login`, `logout`,
`email-verification/request`, `email-verification/confirm`, `password-reset/request`,
`password-reset/confirm`, all under `/api/v1/auth`. Studio API: GET/POST `/api/v1/studios`,
GET `/api/v1/studios/{id}`, POST `/api/v1/studios/{id}/activate`. API errors contain
`code`, `message`, `fieldErrors`, `traceId`; JPA entities are never response bodies.
