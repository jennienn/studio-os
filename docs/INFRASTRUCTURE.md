# INFRASTRUCTURE.md

## 1. Local

Docker Compose:
```text
postgres
redis
```

frontend/backend는 local process 또는 compose로 선택 가능.

## 2. Production components

```text
Frontend hosting
Backend runtime
Managed PostgreSQL
Managed Redis
Cloudflare R2
DNS/TLS
External OAuth/notification providers
```

## 3. Initial deployment principle

MVP에서는 운영 복잡도를 최소화한다.

후보:
- Frontend: Vercel or equivalent
- Backend: Railway / Render / AWS based on cost/control
- PostgreSQL: managed service
- Redis: managed service
- Object storage: Cloudflare R2

최종 provider는 비용과 운영 요구사항 확인 후 ADR로 기록한다.

## 4. Environment separation

- local
- staging
- production

OAuth callback URL, DB, R2 bucket, secrets를 환경별로 분리한다.

## 5. Secrets

Git commit 금지:
- DB password
- OAuth client secret
- JWT/session secret
- R2 secret
- notification provider secret

`.env.example`에는 key 이름만 제공한다.

## 6. Backup

Production DB:
- automated backup
- restore procedure 확인
- migration 전 backup 정책
