# ROADMAP.md

# Development Roadmap

각 Phase는 기본적으로 별도의 Codex task로 진행한다. 사용자 승인으로 Phase 4–6은 ADR-052의
하나의 작업 범위에서 Customer 테스트 → Payment 테스트 → Booking 테스트 순서로 진행한다.

```text
Plan
→ Review
→ Implement
→ Test
→ Review
→ Next Phase
```

한 번에 전체 서비스를 구현시키지 않는다.

---

## Phase 0 — Documentation Gate
Status: READY FOR FINAL REVIEW

Goal: implementation blocker를 문서로 확정한다.

Required docs:
- DECISIONS.md
- DOMAIN_RULES.md
- FEATURES.md
- AUTH.md
- MULTITENANCY.md
- DATA_MODEL.md
- BOOKING.md
- PAYMENTS.md
- ACCEPTANCE_CRITERIA.md
- ROADMAP.md

Acceptance: Codex re-review에서 implementation-blocking contradiction/ambiguity가 없어야 한다.

---

## Phase 1 — Repository Foundation

Goal: 실제 monorepo 기반 생성.

Create:

```text
frontend/
backend/
docker-compose.yml
.env.example
```

Backend:
- Java 21
- Spring Boot
- Gradle
- PostgreSQL
- Flyway
- Redis
- Actuator

Frontend:
- existing design migration
- Next.js
- TypeScript
- Tailwind
- test config

Tests:
- Spring boot smoke
- PostgreSQL Testcontainers
- frontend typecheck/build

Business feature 구현 금지.

---

## Phase 2 — Authentication + Studio + Tenant

MVP:

```text
Email + Password
Google OAuth
Kakao OAuth
```

Implement:
- User
- AuthAccount
- Studio
- StudioMembership
- Staff
- Spring Security
- Spring Session
- Redis
- email verification
- password reset
- membership authorization
- OWNER default Staff creation

Tests:
- email login
- OAuth mapping
- logout/session expiry
- duplicate identity
- cross-tenant access

---

## Phase 3 — Onboarding + Configuration

Implement:
- strict LESSON/BEAUTY subtype hierarchy
- StudioCapability
- BusinessHours
- BookingPolicy
- LessonPolicy
- BeautyPolicy
- category-specific navigation
- category change rejection

---

## Phase 4 — Customer Core

Implement:
- Customer CRUD
- search
- archive
- operator memo
- phone normalization
- pagination

UI terminology:
- LESSON = 회원
- BEAUTY = 고객

---

## Phase 5 — Payment Core

Implement:
- manual payment
- status/reference
- full refund
- PaymentRefund
- cancellation

No PG.

---

## Phase 6 — Common Booking Engine

현재 작업 범위는 ADR-052를 따른다. 아래 전문 detail 및 group capacity 통합은 해당
Phase 7–9 도메인이 생긴 후 연결하며, Phase 6에서 관련 가짜 레코드를 만들지 않는다.

Implement:
- Booking
- LessonBookingDetail
- BeautyBookingDetail
- BookingBlock
- BusinessHours
- availability
- private staff overlap
- group capacity architecture
- state transitions
- idempotency
- transaction conflict protection

Redis distributed lock은 테스트에서 필요성이 확인된 경우만 추가한다.

---

## Phase 7 — LESSON Products & Enrollment

Implement:
- PassProduct
- Enrollment
- EnrollmentCycle
- PassUsageLedger
- Class
- ClassSchedule
- ClassOccurrence
- cycle snapshots
- 90-day occurrence generation

---

## Phase 8 — LESSON Operations

Implement:
- private booking eligibility
- group booking
- attendance
- bulk attendance
- completion
- configured deduction trigger
- cancellation restore
- renewal
- SCHEDULED cycle activation
- payment association
- manual adjustment

---

## Phase 9 — BEAUTY Module

Implement:
- BeautyService
- single-service booking
- TreatmentRecord
- snapshots
- revisit
- Deposit
- NoShow operations

---

## Phase 10 — Customer Booking Portal

Route:

```text
/book/{studioSlug}
```

Common:
- name + phone identification
- Redis portal session
- rate limiting
- idempotency
- availability
- booking

LESSON:
- existing Customer only
- active enrollment/cycle validation

BEAUTY:
- existing or new Customer
- service booking

---

## Phase 11 — Notifications & Dashboard

Implement:
- Notification entity
- intent/status
- low balance/expiry/renewal/revisit/deposit alerts
- backend-derived dashboard metrics

Actual Kakao 알림톡/SMS/Email delivery는 P1.

---

## Phase 12 — MVP Reliability Gate

Verify:
- authorization
- indexes
- booking race tests
- ledger reconciliation
- migration testing
- backup/restore procedure
- session security
- rate limiting
- observability
- E2E critical flows

---

## P1-A — Customer OTP
- SMS OTP
- verified portal session
- future booking list
- booking cancellation

## P1-B — Staff Management
- invitation
- multiple staff
- staff working hours
- staff-specific calendar

## P1-C — Treatment Photos
- Cloudflare R2
- TreatmentPhoto
- private access

## P1-D — External Notifications
- Kakao 알림톡
- SMS
- Email

NOTE:

```text
Kakao OAuth login = MVP Phase 2
Kakao 알림톡 = P1
```

## P1-E — Beauty Multi-Service
- multiple services per booking
- add-ons

## P1-F — Partial Refund
- partial refund

## P2
- actual PG
- advanced analytics
- accounting integration
- category migration
- marketing automation


# Phase 0 Final Integrity Rules

Before Phase 8 lesson operations are accepted, tests must cover:

- COUNT_BASED expiry with unused credits
- validity boundary with unresolved booking/reservation
- successor activation only after terminal settlement
- PRIVATE deferred no-show releases reservation without deduction
- GROUP ABSENT releases reservation without deduction
- BOOKING_CONFIRMED no-show does not auto-restore
- no terminal booking/attendance leaves ACTIVE entitlement reservations stranded


## Phase 7–8 accepted scope

Phase 7–8 is user-approved as one coordinated A–F LESSON scope under ADR-053; Phase 9 remains excluded.
