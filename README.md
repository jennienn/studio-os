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


## Phase 1 foundation

The landing design is preserved under `frontend/`. Backend business domains and
operator/customer authentication are not implemented yet. No business tables or
placeholder domain migrations are created. Flyway initializes its own history table
and validates an empty migration set until the first real schema migration.

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
Other backend endpoints are denied. CSRF remains enabled; there is no generated demo
user or password. Spring Session uses Redis; it is infrastructure only at this phase.
No OAuth registration/client secrets are needed until Phase 2.

### Frontend

In a separate terminal:

```sh
cd frontend
npm ci
npm run dev
```

Visit `http://localhost:3000`. The default build exposes the landing and a preparation
notice at prototype routes; it does not offer functioning authentication or bookings.

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
absence of business tables, Redis/session round-trip, public health privacy, and
non-health request/CSRF denial.

```sh
cd frontend
npm run typecheck
npm run test
npm run build
npx playwright install chromium --no-shell
npm run test:e2e
```

Run browser tests against a default build (`NEXT_PUBLIC_ENABLE_DEMO` unset or false).
Playwright starts the repository's built frontend on port 31741, tests desktop/mobile
landing previews and verifies the demo gate. Unit tests also check category separation.

From the root after filling `.env`:

```sh
docker compose config --quiet
```

Historical screenshots and manual prototype verification in `artifacts/` describe the
old prototype, not the production acceptance suite. Authoritative product requirements
remain under `docs/`; Phase 1 does not implement subsequent roadmap phases.
