# TESTING.md

## 1. Rule

핵심 business rule은 UI 수동 테스트만으로 끝내지 않는다.

## 2. Backend unit tests

도메인 규칙:
- pass deduction
- cancellation restore
- renewal cycle creation
- validity check
- booking eligibility
- revisit date calculation
- deposit state transitions
- role authorization

Tools:
- JUnit 5
- AssertJ
- Mockito where appropriate

## 3. Backend integration tests

Tools:
- Spring Boot Test
- Testcontainers PostgreSQL
- Redis container when needed

필수:
- Flyway migration boot
- repository query
- transaction rollback behavior
- tenant isolation
- booking race/conflict

## 4. Frontend tests

- category subtype filtering
- LESSON에 BEAUTY subtype이 나타나지 않음
- BEAUTY에 LESSON subtype이 나타나지 않음
- capability-based navigation
- forms/validation

Tools:
- Vitest
- React Testing Library

## 5. E2E critical flows

### LESSON
```text
signup
→ onboarding LESSON
→ customer create
→ pass create
→ enrollment
→ customer booking
→ lesson complete
→ pass deduction
→ renewal
```

### BEAUTY
```text
signup
→ onboarding BEAUTY
→ customer create
→ service create
→ booking
→ treatment complete
→ revisit due
```

## 6. Regression from Élanor

LESSON MVP에서 확인:
- 개인 예약
- 예약 시간 차단
- 수업 완료
- 회차 차감/취소 복구
- 단체 출석
- 재등록 cycle history
- 결제 필요 상태

## 7. Phase 3 verification scope

ConfigurationIntegrationTest uses PostgreSQL/Redis Testcontainers and the Spring Security
filter chain. It covers both categories, invalid subtype/capability/policy requests, OWNER/
MANAGER/STAFF permissions, cross-tenant reads/writes, revoked membership, category guards,
transaction rollback after a DB failure, repeated completion and concurrent/stale writes.
ConfigurationValidationTest covers the subtype matrix and incomplete/invalid configuration.

Frontend configuration tests cover category cards, filtered subtypes, branch-only policy
questions and summary wording, draft reset, role-disabled fields and route decisions.
Playwright configuration.spec.ts uses real signup/verification/login and disposable databases
for LESSON and BEAUTY on desktop/mobile, including completion, reload, settings persistence
and incompatible route redirects. Future business operations are outside this suite's Phase 3 scope.

## 8. Phases 4–6 verification

Step A: CustomerIntegrationTest + KoreanPhoneTest, customer.test.tsx.
Step B: PaymentIntegrationTest (including concurrent refund and rollback), payment.test.tsx and Customer regressions.
Step C: BookingIntegrationTest, booking.test.tsx, then the entire backend/frontend/E2E suite.

Booking tests cover time/hours/timezone, block scopes, state machine, STAFF privacy, tenant references,
idempotency replay/reauthorization/rollback, archival guards, rescheduling and released occupancy.
Concurrent tests include conflicting bookings, same-key booking retries, booking-vs-block creation,
customer-archive-vs-booking creation, and payment full refunds. Actual PostgreSQL Testcontainers are used.
The operations Playwright flow runs for both categories on desktop/mobile using real local-auth sessions,
Customer CRUD/archive/search, payment/refund, booking conflict/cancel/complete/no-show and block creation/removal.
Specialized booking side effects remain explicitly out of scope for the Phase 4–6 suite.

## 9. Phase 7–8 verification

Backend PostgreSQL/Redis integration tests cover pass validation and category/capability guards,
private/group enrollment compatibility, cycle purchase/renewal/activation, ledger-derived balance,
FIRST_USE windows, termination/refund guards, selective occurrence regeneration, private booking
deduction/restore, group capacity and attendance. Cross-tenant references and OWNER/MANAGER/STAFF
assignment rules are exercised through the Spring Security filter chain.

Concurrency tests cover same-key and different-key private completion, group capacity races,
duplicate member booking, attendance replay, renewal races, schedule-update versus booking and
termination versus booking. Each write uses PostgreSQL transactions and the documented Studio or
occurrence row lock; Redis remains session storage rather than business source of truth.

Frontend tests cover capability-based LESSON navigation, pass/enrollment/class/attendance panels,
private eligibility selection and category isolation. Playwright runs the integrated LESSON product
→ enrollment → private completion/cancellation → renewal → class/group attendance flow on desktop
and mobile with local authentication and disposable containers.
