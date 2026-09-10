# TECH_STACK.md

# Technology Stack

## 1. Goal

본 프로젝트는 실제 사용 가능한 멀티테넌트 SaaS를 목표로 한다.
기술은 포트폴리오용 나열이 아니라 실제 요구사항을 해결하기 위해 선택한다.

---

## 2. Frontend

```text
Next.js
React
TypeScript
App Router
Tailwind CSS
shadcn/ui
```

Frontend responsibilities:
- landing
- authentication UI
- onboarding
- operator dashboard
- customer management
- booking calendar
- lesson module UI
- beauty module UI
- customer booking portal

핵심 business rule은 Spring backend가 결정한다.

---

## 3. Backend

```text
Java 21
Spring Boot 3.5.x
Spring Web
Spring Security
Spring Security OAuth2 Client
Spring Session
Spring Data JPA
QueryDSL
Bean Validation
Flyway
Gradle
Spring Boot Actuator
Micrometer
```

Architecture는 Modular Monolith다.

---

## 4. Database

```text
PostgreSQL
```

PostgreSQL이 persistent business data의 source of truth다.

사용 기능:
- transactions
- constraints
- indexes
- partial unique indexes
- locking
- JSONB는 제한적 policy/config metadata에만 사용

Hibernate production setting:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Schema 변경은 Flyway migration으로 관리한다.

---

## 5. Redis

Redis는 PostgreSQL을 대체하지 않는다.

MVP responsibilities:

```text
Spring Session
Customer Portal Session
Rate Limiting
Temporary Verification State
Configuration Cache
```

### Reservation Concurrency

예약 정합성은 다음으로 보장한다.

```text
PostgreSQL transaction
+
availability recheck
+
database constraints
+
appropriate database locking
```

Redis distributed lock은 MVP 필수 구성요소가 아니다.
실제 concurrency/load test에서 DB transaction/locking만으로 처리하기 어려운 contention이 확인된 경우에만 도입을 검토한다.
Redis lock을 추가해도 DB validation을 제거하지 않는다.

---

## 6. Authentication

MVP operator login:

```text
Email + Password
Google OAuth
Kakao OAuth
```

Stack:

```text
Spring Security
Spring Security OAuth2 Client
Spring Session
Redis
HttpOnly Cookie
```

JWT access token을 browser localStorage에 저장하지 않는다.

Customer portal:

```text
name + phone
→ Redis portal session
→ HttpOnly Cookie
```

SMS OTP는 P1.

---

## 7. ORM / Query

Simple CRUD/aggregate lifecycle:

```text
Spring Data JPA
```

Complex filtering/dashboard queries:

```text
QueryDSL
```

---

## 8. Database Migration

```text
Flyway
```

production DB 직접 수정 금지.

---

## 9. Object Storage

```text
Cloudflare R2
```

사용 예:
- treatment photos (P1)
- studio/profile images
- exports

DB에는 object key와 metadata만 저장한다.

---

## 10. Payments

MVP:

```text
manual payment records
manual deposit confirmation
full refund record
```

Actual PG는 P2.

---

## 11. Notifications

MVP:
- Notification entity
- intent/status
- mock/manual state

P1:
- Kakao 알림톡
- SMS
- Email provider

Kakao OAuth login과 Kakao 알림톡을 혼동하지 않는다.

---

## 12. Testing

Backend:

```text
JUnit 5
AssertJ
Mockito
Spring Boot Test
Testcontainers
```

Frontend:

```text
Vitest
React Testing Library
```

E2E:

```text
Playwright
```

H2로 PostgreSQL-specific behavior를 대체하지 않는다.

---

## 13. Infrastructure

Local:

```text
Docker
Docker Compose
PostgreSQL
Redis
```

Production hosting provider는 deployment 단계 전에 ADR로 확정한다.

---

## 14. Observability

```text
Spring Boot Actuator
Micrometer
Prometheus (later)
Grafana (later)
```

---

## 15. Pass Balance

MVP authoritative source:

```text
PassUsageLedger
```

```text
remaining balance = SUM(PassUsageLedger.amount)
```

별도의 mutable remaining_count/used_count를 source of truth로 사용하지 않는다.

---

## 16. Technologies Not Used Initially

명확한 필요성이 생기기 전까지 사용하지 않는다.

```text
Kafka
Kubernetes
Elasticsearch
Microservices
```
