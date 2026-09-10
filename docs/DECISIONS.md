# DECISIONS.md

# Architecture & Product Decisions

이 문서는 개발 중 임의로 변경해서는 안 되는 주요 제품 및 기술 결정을 기록한다.
새로운 결정이 필요한 경우 구현 중 임의로 판단하지 말고 이 문서를 먼저 수정한다.

---

## ADR-001 — Product Category Model
Status: Accepted

서비스는 하나의 SaaS이며 사업장은 가입 시 다음 두 상위 카테고리 중 하나를 선택한다.

```text
LESSON
BEAUTY
```

하위 업종은 반드시 상위 카테고리에 종속된다.

### LESSON
```text
DANCE
PILATES
YOGA
PT
POLE
VOCAL_MUSIC
OTHER_LESSON
```

### BEAUTY
```text
NAIL
EYELASH
HAIR_EXTENSION
WAXING
HAIR
OTHER_BEAUTY
```

LESSON과 BEAUTY 하위 업종을 하나의 flat list로 관리하지 않는다.
가입 완료 이후 일반 설정 화면에서는 `businessCategory`를 변경할 수 없다.
카테고리 변경은 향후 별도의 migration workflow로만 지원한다.

---

## ADR-002 — Architecture
Status: Accepted

초기 시스템은 Modular Monolith로 개발한다.

```text
Frontend: Next.js
Backend: Spring Boot
Database: PostgreSQL
Cache / Session: Redis
Object Storage: Cloudflare R2
```

Microservice architecture는 사용하지 않는다.

---

## ADR-003 — Repository
Status: Accepted

하나의 repository 안에 frontend와 backend를 함께 관리한다.

```text
/
├── frontend/
├── backend/
├── docs/
├── AGENTS.md
└── docker-compose.yml
```

---

## ADR-004 — Operator Authentication
Status: Accepted

MVP에서 다음 세 가지 운영자 로그인 방식을 모두 지원한다.

```text
Email + Password
Google OAuth
Kakao OAuth
```

Authentication framework:

```text
Spring Security
Spring Security OAuth2 Client
Spring Session
Redis
```

브라우저 authentication은 server-side session을 사용한다.
JWT access token을 localStorage에 저장하는 방식은 사용하지 않는다.

Session cookie:

```text
HttpOnly
Secure in production
SameSite=Lax
```

세션 inactivity timeout 기본값은 12시간이다.
로그인 성공 시 session fixation 방지를 위해 session ID를 rotate한다.
로그아웃 시 server-side session을 즉시 invalidate한다.
Remember-me 기능은 MVP에서 지원하지 않는다.

---

## ADR-005 — Password Authentication
Status: Accepted

Email login은 email + password 방식이다.
Password는 BCrypt로 hash한다.

금지:

```text
plaintext password
reversible encryption
password logging
```

Password reset과 email verification은 MVP에 포함한다.

---

## ADR-006 — OAuth Account Model
Status: Accepted

Provider identity를 User에 직접 저장하지 않는다.

```text
User
AuthAccount
```

```text
User
- id
- email
- name

AuthAccount
- id
- userId
- provider
- providerUserId
```

Provider:

```text
EMAIL
GOOGLE
KAKAO
```

`provider + providerUserId`는 unique 해야 한다.
OAuth provider의 이메일이 기존 User 이메일과 같다는 이유만으로 자동 account linking 하지 않는다.
Account linking은 이미 인증된 사용자가 명시적으로 연결하는 방식으로만 지원한다.

---

## ADR-007 — Multi-Tenancy
Status: Accepted

Tenant 단위는 `Studio`이다.
모든 tenant-owned persistent entity는 명시적인 `studio_id`를 가진다.
간접적인 관계만으로 tenant ownership을 추론하지 않는다.

Operator request authorization:

```text
Authenticated User
→ StudioMembership validation
→ Authorized Studio Context
→ domain operation
```

client가 전달한 `studioId`만 신뢰하지 않는다.

---

## ADR-008 — Roles
Status: Accepted

Role model:

```text
OWNER
MANAGER
STAFF
```

MVP 기본 permission:

### OWNER
- 모든 운영 기능
- 사업장 설정
- 환불
- 수동 회차 조정
- 권한 관리

### MANAGER
- 고객
- 예약
- 결제
- 출석 / 시술
- 재등록
- 일부 운영 설정

### STAFF
- 자신의 일정
- 출석 / 시술 완료 처리
- 업무에 필요한 제한된 고객 정보

상세 permission matrix는 SECURITY.md에서 관리한다.

---

## ADR-009 — Staff MVP Scope
Status: Accepted

Staff domain은 MVP schema에 포함한다.
사업장 생성 시 OWNER와 연결된 기본 Staff profile 하나를 자동 생성한다.

MVP:

```text
default owner staff
staff assignment in bookings
```

P1:

```text
staff invitation
multiple staff management UI
staff-specific permission management
staff-specific availability UI
```

---

## ADR-010 — Customer Identity
Status: Accepted

`User`와 `Customer`는 다른 entity이다.

```text
User = SaaS 운영자 계정
Customer = 각 Studio의 고객
```

LESSON UI에서는 Customer를 `회원`, BEAUTY UI에서는 `고객`으로 표시한다.
Persistence entity는 `Customer` 하나만 사용한다.

---

## ADR-011 — Customer Booking Portal
Status: Accepted

Public route:

```text
/book/{studioSlug}
```

MVP identification:

```text
name + phone
```

전화번호는 normalized value를 기준으로 비교한다.

### LESSON
- 기존 Customer가 반드시 존재해야 한다.
- active Enrollment / EnrollmentCycle을 확인한다.
- 없는 고객은 자동 생성하지 않는다.

### BEAUTY
- 기존 Customer가 있으면 연결한다.
- 없으면 name + phone을 이용해 신규 Customer 생성이 가능하다.

---

## ADR-012 — Customer Portal Session
Status: Accepted

name + phone 확인 성공 후 short-lived customer portal session을 생성한다.

```text
Redis + HttpOnly Cookie
```

기본 portal session lifetime:

```text
30 minutes
```

고객에게 다음 정보는 노출하지 않는다.

```text
operator memo
treatment private notes
treatment photos
payment history
other customers
internal notification data
```

SMS OTP는 P1이다.
Public identification endpoint에는 rate limiting을 적용한다.

---

## ADR-013 — Booking Engine
Status: Accepted

LESSON과 BEAUTY는 하나의 common Booking engine을 사용한다.

```text
Booking
├── LessonBookingDetail
└── BeautyBookingDetail
```

LESSON private booking:

```text
Booking → Staff → EnrollmentCycle
```

LESSON group booking:

```text
Booking → ClassOccurrence
```

BEAUTY booking:

```text
Booking → Staff → BeautyService
```

---

## ADR-014 — Booking Occupancy
Status: Accepted

1:1 예약 충돌 기준:

```text
same studio
same staff
overlapping time
```

Group booking:

```text
same ClassOccurrence
confirmed/pending booking count < capacity
```

단순히 같은 시간에 Booking이 하나 존재한다는 이유로 모든 예약을 거부하지 않는다.

---

## ADR-015 — Class Occurrence
Status: Accepted

Recurring `ClassSchedule`과 실제 날짜별 수업을 분리한다.

```text
Class
→ ClassSchedule
→ ClassOccurrence
```

Booking과 Attendance는 `ClassOccurrence`에 연결한다.
ClassSchedule 생성/수정 시 향후 90일 범위의 occurrence를 생성하고 rolling generation으로 향후 90일을 유지한다.
과거 occurrence는 schedule 변경으로 수정하지 않는다.
예약이 존재하는 future occurrence 변경은 별도 검증이 필요하다.

---

## ADR-016 — Booking Status
Status: Accepted

```text
PENDING
CONFIRMED
COMPLETED
CANCELLED
NO_SHOW
```

Capacity를 점유하는 상태:

```text
PENDING
CONFIRMED
```

MVP의 일반 예약 생성 기본 상태는 `CONFIRMED`이다.
PENDING은 향후 승인/외부결제 flow를 위해 유지한다.

---

## ADR-017 — Booking Correctness
Status: Accepted

PostgreSQL이 예약 정합성의 최종 source of truth이다.

```text
transaction
+
availability recheck
+
database constraint / locking
```

Redis distributed lock은 MVP 필수 요구사항이 아니다.
실제 contention test에서 필요성이 확인될 경우만 추가한다.

---

## ADR-018 — Idempotency
Status: Accepted

다음 command는 idempotent 해야 한다.

```text
public booking create
lesson completion
attendance deduction
payment refund
renewal
beauty treatment completion
```

외부/public command에는 `Idempotency-Key`를 지원한다.
동일 business event로 동일 ledger/treatment/refund가 두 번 생성되어서는 안 된다.

---

## ADR-019 — Lesson Product Types
Status: Accepted

MVP lesson product model:

```text
COUNT_BASED
TIME_BASED
```

Examples:

```text
10회권 → COUNT_BASED
1회권 → COUNT_BASED totalCount=1
30일 이용권 → TIME_BASED
월 이용권 → TIME_BASED billingPeriod=MONTH
```

4/8/12 등 특정 숫자를 코드에 hardcode하지 않는다.

---

## ADR-020 — Lesson Deduction Trigger
Status: Accepted

Count-based product는 정확히 하나의 deduction trigger를 가진다.

```text
BOOKING_CONFIRMED
ATTENDANCE_PRESENT
LESSON_COMPLETED
```

동일 cycle에서 여러 trigger가 동시에 적용되어서는 안 된다.

Default:

```text
private lesson → LESSON_COMPLETED
group lesson → ATTENDANCE_PRESENT
```

---

## ADR-021 — Pass Ledger
Status: Accepted

Pass balance의 authoritative source는 `PassUsageLedger`이다.

```text
PURCHASE            +10
BOOKING_DEDUCTION   -1
ATTENDANCE          -1
LESSON_COMPLETED    -1
CANCEL_RESTORE      +1
MANUAL_ADJUSTMENT   ±N
```

MVP에서는 `used_count`/`remaining_count`를 authoritative mutable column으로 저장하지 않는다.
잔여 회차는 ledger balance를 기준으로 계산한다.

각 ledger business effect는 `referenceType`, `referenceId`를 가져야 하고 동일 reference로 동일 종류의 effect가 중복 생성되지 않도록 unique constraint를 사용한다.

---

## ADR-022 — Enrollment Cycle
Status: Accepted

재등록은 기존 cycle을 수정하지 않는다.

```text
Enrollment
├── Cycle 1
├── Cycle 2
└── Cycle 3
```

Cycle status:

```text
SCHEDULED
ACTIVE
COMPLETED
EXPIRED
CANCELLED
```

Enrollment 하나에는 최대 하나의 ACTIVE cycle만 존재할 수 있다.
현재 ACTIVE cycle이 남아 있는 상태에서 미리 재등록하면 새 cycle은 `SCHEDULED`로 생성한다.

---

## ADR-023 — Cycle Snapshot
Status: Accepted

EnrollmentCycle 생성 시 다음 값을 snapshot한다.

```text
productName
productType
purchasedCount
validityDays
purchasePrice
deductionTrigger
validityStartRule
```

상품 변경이 기존 구매 이력에 영향을 주면 안 된다.

---

## ADR-024 — Validity
Status: Accepted

Supported validity start rules:

```text
PURCHASE_DATE
FIRST_USE
```

Default:

```text
COUNT_BASED → FIRST_USE
TIME_BASED → PURCHASE_DATE
```

유효기간은 Studio timezone의 LocalDate 기준으로 판단한다.

---

## ADR-025 — Renewal & Carry Over
Status: Accepted

자동 잔여 회차 이월은 MVP에서 지원하지 않는다.
이월이 필요한 경우 OWNER가 manual adjustment로 새 cycle에 회차를 추가하고 reason을 기록한다.
재등록 cycle은 실제 결제가 확인된 시점에 생성한다.
`결제 필요` 상태만으로 cycle을 생성하지 않는다.

---

## ADR-026 — Beauty Booking
Status: Accepted

MVP에서는 Booking 하나당 BeautyService 하나만 선택할 수 있다.
Multiple services per booking은 P1이다.

---

## ADR-027 — Treatment Snapshot
Status: Accepted

Booking 완료 시 TreatmentRecord를 생성하고 다음 값을 snapshot한다.

```text
serviceName
servicePrice
serviceDurationMinutes
```

BeautyService가 나중에 수정되어도 기존 TreatmentRecord는 변경되지 않는다.

---

## ADR-028 — Revisit
Status: Accepted

BeautyService는 optional `revisitDays`를 가진다.
Treatment 완료 시 `treatedDate + revisitDays`로 revisitDueDate를 계산하고 TreatmentRecord에 snapshot한다.
과거 TreatmentRecord의 revisitDueDate는 이후 service rule 변경으로 자동 변경하지 않는다.
Manual revisit override는 P1이다.

---

## ADR-029 — Deposit
Status: Accepted

MVP에서 실제 PG 결제는 구현하지 않는다.
Deposit은 운영자가 수동으로 상태를 기록한다.

```text
REQUIRED
PAID
REFUNDED
FORFEITED
```

Deposit은 Booking과 Payment record에 연결할 수 있어야 한다.

---

## ADR-030 — Payment
Status: Accepted

MVP currency는 `KRW`이고 금액은 정수 원 단위 `BIGINT`로 저장한다.
MVP에서는 full refund만 지원한다.
Partial refund는 P1이다.
Paid payment의 반환은 삭제가 아니라 Refund record를 생성해 이력을 보존한다.

---

## ADR-031 — Configuration Persistence
Status: Accepted

핵심 운영 설정을 하나의 자유형 JSONB에 몰아서 저장하지 않는다.

```text
StudioCapability
BusinessHours
BookingPolicy
LessonPolicy
BeautyPolicy
```

Category exclusive capability는 backend allowlist로 검증한다.

---

## ADR-032 — Feature Disable
Status: Accepted

Capability를 끄더라도 기존 business data를 삭제하지 않는다.

```text
feature disabled
→ new operations hidden/restricted
→ historical data preserved
```

기존 active data 때문에 integrity 문제가 발생하는 기능은 disable 요청을 거부하고 명확한 이유를 반환한다.

---

## ADR-033 — External Notifications
Status: Accepted

Notification domain과 external provider를 분리한다.
MVP에서는 notification intent/status 저장과 mock/manual state까지 구현한다.
실제 Kakao 알림톡/SMS/Email provider 연동은 P1이다.
Kakao OAuth 로그인과 Kakao 알림톡은 별개의 기능이다.

---

## ADR-034 — Actual PG
Status: Accepted

실제 online PG integration은 MVP가 아니다.

```text
manual payment record
manual deposit confirmation
refund record
```

P2에서 실제 PG integration을 검토한다.

---

## ADR-035 — EnrollmentCycle Lifecycle
Status: Accepted

한 Enrollment에는 동시에 다음 cycle만 허용한다.

```text
ACTIVE    최대 1개
SCHEDULED 최대 1개
```

새 재등록 결제가 확인됐을 때 기존 ACTIVE cycle이 없으면 새 cycle은 ACTIVE, 기존 ACTIVE cycle이 있으면 SCHEDULED가 된다.
기존 ACTIVE cycle이 `COMPLETED`, `EXPIRED`, `CANCELLED` 중 하나가 되면 SCHEDULED cycle을 자동으로 ACTIVE로 전환한다.
여러 SCHEDULED cycle을 동시에 쌓는 기능은 MVP에서 지원하지 않는다.

---

## ADR-036 — Validity Start
Status: Accepted

COUNT_BASED는 `PURCHASE_DATE`, `FIRST_USE`를 지원한다.
TIME_BASED는 MVP에서 `PURCHASE_DATE`만 지원한다.

`FIRST_USE`는 해당 cycle의 configured deduction trigger가 최초로 성공한 시점이다.
FIRST_USE 이전 ACTIVE cycle은 `start_date = null`, `valid_end_date = null`일 수 있다.
첫 사용 transaction에서 start/end date와 ledger deduction을 함께 확정한다.

---

## ADR-037 — Deduction Trigger Compatibility
Status: Accepted

### PRIVATE
허용:

```text
BOOKING_CONFIRMED
LESSON_COMPLETED
```

금지:

```text
ATTENDANCE_PRESENT
```

### GROUP
허용:

```text
BOOKING_CONFIRMED
ATTENDANCE_PRESENT
```

MVP 금지:

```text
LESSON_COMPLETED
```

Backend가 잘못된 조합을 거부한다.

---

## ADR-038 — Studio Slug
Status: Accepted

`Studio.slug`는 public tenant identifier이므로 globally unique다.

```text
UNIQUE(slug)
```

Public route는 `/book/{studioSlug}`다.
Slug 변경은 OWNER만 가능하고 기존 URL redirect는 MVP에서 지원하지 않는다.

---

## ADR-039 — Idempotency Ownership
Status: Accepted

Idempotency namespace:

```text
studio
+
actor
+
operation
+
idempotency key
```

Actor type:

```text
OPERATOR_USER
CUSTOMER_PORTAL
SYSTEM
```

Replay는 `same studio + same actor + same operation + same request hash`일 때만 허용한다.
동일 namespace/key에 다른 request body가 들어오면 `409 Conflict`를 반환한다.

---

## ADR-040 — Redis Booking Coordination
Status: Accepted

예약 정합성의 최종 source of truth는 PostgreSQL이다.
MVP에서는 Redis distributed lock을 필수 사용하지 않는다.

```text
PostgreSQL transaction
+
availability recheck
+
database constraints
+
appropriate database locking
```

실제 동시성 테스트에서 필요성이 확인될 경우에만 Redis booking coordination을 추가한다.

---

## ADR-041 — Kakao OAuth Scope
Status: Accepted

```text
Email + Password → MVP
Google OAuth → MVP
Kakao OAuth → MVP
Kakao 알림톡 → P1
```

---

## ADR-042 — Pass Balance Projection
Status: Accepted

MVP에서 Pass balance의 authoritative source는 `PassUsageLedger`이다.
잔여 회차는 `SUM(PassUsageLedger.amount)`로 계산한다.
`used_count`, `remaining_count`를 authoritative mutable column으로 사용하지 않는다.
성능 문제가 실제로 확인될 경우에만 projection/cache를 추가한다.

---

## ADR-043 — Customer OTP Timing
Status: Accepted

MVP customer booking portal identification은 `name + phone`이다.
Identification 성공 후 short-lived Redis portal session을 사용한다.
SMS OTP는 P1이다.
Name + phone 방식은 controlled pilot 범위에서만 사용한다.

---

## Remaining Deferred Decisions

다음은 Phase 0 blocker가 아니며 해당 단계 전에 ADR로 확정한다.

- exact PostgreSQL booking locking/constraint implementation: resolved by ADR-052
- production hosting provider before production deployment
- external Kakao/SMS/Email notification provider before P1-D
- actual PG provider before P2
---

## ADR-044 — Entitlement Reservation for Deferred Deduction
Status: Accepted

COUNT_BASED lesson products whose deduction trigger is deferred until `ATTENDANCE_PRESENT` or `LESSON_COMPLETED` must reserve entitlement at booking confirmation time.

The reservation does not write a usage deduction to `PassUsageLedger`. Instead, a separate `PassEntitlementReservation` records one reserved unit.

Available entitlement is:

```text
ledger balance
- active entitlement reservations
```

A booking can be confirmed only when available entitlement is at least 1.

When the configured deduction event occurs:

```text
active reservation consumed
+
PassUsageLedger -1
```

are completed atomically in the same transaction.

When a booking is cancelled before deduction, the reservation is released without writing a restore ledger event.

`BOOKING_CONFIRMED` products do not create entitlement reservations because the ledger deduction already occurs at confirmation time.

---

## ADR-045 — Cycle Terminal Conditions and Restored Credits
Status: Accepted

A COUNT_BASED cycle becomes `COMPLETED` only when all of the following are true:

```text
ledger balance == 0
AND active entitlement reservations == 0
AND no occupying booking remains whose entitlement was deducted/reserved from the cycle
```

This prevents a successor cycle from becoming ACTIVE while unresolved bookings from the previous cycle can still restore or consume entitlement.

A TIME_BASED cycle becomes `EXPIRED` after its valid end date only after outstanding bookings tied to that cycle are resolved. After the end date, no new booking may use the cycle.

If timely cancellation occurs while the original cycle is still ACTIVE:

- deferred deduction: release the entitlement reservation; no ledger restore is needed.
- `BOOKING_CONFIRMED` deduction: create exactly one `CANCEL_RESTORE +1` on the same cycle.

A terminal cycle is never reopened automatically. Because successor activation waits for unresolved bookings to settle, normal automatic cancellation restoration must occur before successor activation. Exceptional post-terminal corrections are OWNER-only manual adjustments and must include an audit reason.

---

## ADR-046 — Lesson Payment Refund Policy
Status: Accepted

MVP supports a full refund of a lesson-cycle payment only when the related cycle has no consumed entitlement and no outstanding booking/reservation dependency.

Refund is allowed when all are true:

```text
no negative usage ledger effect has been permanently consumed
no active entitlement reservation
no PENDING/CONFIRMED booking tied to the cycle
cycle status is SCHEDULED or ACTIVE
```

Refund transaction:

```text
PaymentRefund create
+ Payment -> REFUNDED
+ EnrollmentCycle -> CANCELLED
+ activate SCHEDULED successor if lifecycle rule requires it
```

If the cycle has used entitlement, completed attendance/lesson usage, or unresolved bookings, automatic full refund is rejected in MVP. The operator must first resolve bookings and usage according to documented business rules. Partial or retroactive entitlement-proration logic is P1+.

Terminal `COMPLETED` or `EXPIRED` lesson cycles are not automatically refundable in MVP.



## ADR-047 — COUNT_BASED Expiration
Status: Accepted

COUNT_BASED cycles with a configured validity period can expire even when unused ledger credits remain.

A COUNT_BASED cycle becomes booking-ineligible immediately after `valid_end_date` in the Studio timezone. After that boundary, no new booking may use the cycle.

If there are no unresolved bookings or ACTIVE entitlement reservations tied to the cycle, the cycle transitions to `EXPIRED` immediately.

If unresolved bookings/reservations exist at the validity boundary, the cycle remains technically `ACTIVE` only as a settlement state, but it is not eligible for any new booking. Those existing bookings must resolve first. After the last unresolved booking/reservation settles, the cycle transitions to `EXPIRED`.

Unused ledger credits are not zeroed with a synthetic deduction when the cycle expires. They remain in the ledger as historical unused entitlement, but an `EXPIRED` cycle is never booking-eligible.

A `SCHEDULED` successor becomes `ACTIVE` only after the previous cycle reaches `COMPLETED`, `EXPIRED`, or `CANCELLED`.

If a pre-existing booking is cancelled after the validity boundary while the cycle is waiting for settlement:

- deferred-deduction product: release the ACTIVE entitlement reservation with no ledger restore; then re-evaluate expiration.
- `BOOKING_CONFIRMED` product: apply any cancellation restore required by the accepted cancellation policy to the original cycle for historical correctness; then re-evaluate expiration. Restored credit does not make the expired/expiring cycle newly bookable.

Booking creation must validate that the scheduled lesson date/time itself is within the cycle validity window. A booking for a lesson occurring after `valid_end_date` cannot be created using that cycle merely because the booking request was submitted earlier.

---

## ADR-048 — Deferred Reservation Resolution for NO_SHOW / ABSENT
Status: Accepted

For deferred-deduction products, `NO_SHOW` and `ABSENT` do not create a usage deduction in MVP because neither event is an accepted deduction trigger.

### PRIVATE + LESSON_COMPLETED

If a confirmed private lesson booking becomes `NO_SHOW` before `LESSON_COMPLETED` occurs:

```text
PassEntitlementReservation ACTIVE -> RELEASED
Booking -> NO_SHOW
no PassUsageLedger deduction
```

### GROUP + ATTENDANCE_PRESENT

If attendance is recorded as `ABSENT`:

```text
Attendance -> ABSENT
related Booking -> NO_SHOW
PassEntitlementReservation ACTIVE -> RELEASED
no PassUsageLedger deduction
```

If group attendance is recorded as `CANCELLED` because the booking/class participation was cancelled before attendance, the related reservation is released and the Booking follows the documented cancellation flow.

### BOOKING_CONFIRMED products

`BOOKING_CONFIRMED` products already deducted entitlement at confirmation, so there is no ACTIVE entitlement reservation to release. `NO_SHOW` or `ABSENT` does not automatically restore that deduction. Only the accepted cancellation-restoration policy may create `CANCEL_RESTORE`; no-show/absence is not a cancellation.

After any NO_SHOW/ABSENT resolution, cycle terminal/expiration evaluation runs in the same transaction boundary so reservations cannot remain stranded.

No-show penalty deductions outside the configured trigger are not supported in MVP. If a future product requires no-show consumption as its own policy, it requires a new Accepted decision and explicit deduction semantics.

## ADR-049 — Email Verification Gate
Status: Accepted

Confirmed by the user during Phase 2 implementation: an EMAIL account must complete
email verification before receiving an authenticated operator session or creating a Studio.
Pending accounts may use the public verification instructions and resend flow only.
A successful password check on a pending account returns an explicit verification-required result.
Verification does not automatically log the user in. OAuth identity authentication remains separate;
OAuth-only users may have no email and are not gated by an EMAIL account verification token.

## ADR-050 — Phase 2 Pre-Onboarding Studio
Status: Accepted

The Phase 2 task explicitly permits Studio creation before Phase 3 business onboarding.
Such a Studio has status `PRE_ONBOARDING`, with both `business_category` and
`business_type` null. No category default or business capability is inferred.
Studio, OWNER membership and the owner's default Staff are created in one transaction.
This is an incomplete setup state, not completed business onboarding under ADR-001.
Phase 3 owns the migration and validated transition to a configured category/subtype.

## ADR-051 — Phase 3 Configuration Ownership and Dependencies
Status: Accepted

Confirmed by the user during Phase 3:

- After onboarding, businessCategory is immutable through ordinary configuration.
- Only OWNER may change businessType, and only within the current businessCategory.
  A subtype change does not delete or migrate historical data; no automatic migration is implemented.
- Initial onboarding and structural configuration (category, subtype, capabilities) are OWNER-only.
- MANAGER may modify BusinessHours, BookingPolicy, all LessonPolicy operational values,
  and BeautyPolicy.noShowEnabled. STAFF may read but cannot modify Phase 3 configuration.
- BEAUTY DEPOSIT capability and BeautyPolicy.depositEnabled must always agree.
  Only OWNER may change either value, in the same validated update.
- ACTIVE LESSON requires PASS_MANAGEMENT and at least one of PRIVATE_LESSON/GROUP_CLASS.
- ATTENDANCE requires GROUP_CLASS. GROUP_CLASS can be disabled only if ATTENDANCE is
  also disabled in the same validated update. CUSTOMER_BOOKING is independently optional.
- Later lesson phases must allow ATTENDANCE_PRESENT only with both GROUP_CLASS and
  ATTENDANCE enabled. Phase 3 does not create PassProduct or implement deduction.

Implementation: onboarding uses browser form memory until final submit. A PostgreSQL
transaction stores the entire validated configuration and changes PRE_ONBOARDING to ACTIVE.
Studio row locking serializes writes; configuration_version rejects stale full updates (409).
Repeated completion returns 409. Configuration rows are updated in place, never deleted
when a capability is disabled. No Redis configuration cache or lock is required in Phase 3.

## ADR-052 — Coordinated Phases 4–6 Operational Core
Status: Accepted

Confirmed by the user for the combined Customer → Payment → Common Booking scope:

- Archived customers keep historical relationships. New payments/bookings are forbidden;
  existing history, refund/cancellation and existing booking handling remain available.
  Archive is rejected while PENDING/CONFIRMED bookings exist. The tenant/name/normalized-phone
  duplicate boundary includes archived customers. Restoration is outside this scope.
- OWNER/MANAGER manage customers, payments (record/read/unpaid cancellation), bookings and blocks.
  Refund is OWNER-only. STAFF reads only assigned bookings and those customers' names/phones;
  no internal customer memo, payment access, full customer list or mutation commands in this phase.
  Specialized attendance/treatment completion permissions are connected in their later phases.
- Phase 6 explicitly permits manual operator 1:1 bookings without offerings. The category-matching
  LESSON_PRIVATE or BEAUTY_SERVICE kind is used, staff assignment is required, and LESSON_GROUP
  creation is rejected until its domain exists. A persisted manual-flow marker and clear UI wording
  distinguish these records from future specialized bookings. No specialized side effects execute.
- Only PENDING/CONFIRMED bookings may change time/staff/note; customer and kind are immutable.
  Creation/rescheduling rejects past start times and must respect Studio-local business hours.
  bookingWindowDays, slotIntervalMinutes and cancellationCutoffHours do not restrict these operator
  commands. Public booking and entitlement-restoration policy integration remain later-phase work.
- Manual payments allow OTHER with null referenceId only. Create PENDING or PAID; PENDING can be
  explicitly confirmed PAID or cancelled. Only PAID can be fully refunded. paidAt is operator supplied
  or defaults to server time for PAID; future paidAt is rejected.
- PostgreSQL Studio row pessimistic locking serializes booking creation, rescheduling, status commands
  and block creation/deletion. Conflicts are re-read inside that transaction. Different staff may still
  book the same interval. No Redis booking lock or additional PostgreSQL extension is introduced.

Phase 4 tests must pass before Payment implementation; Payment tests must pass before Booking implementation.
The task explicitly defers LessonBookingDetail, BeautyBookingDetail and all Phase 7+ domain records.


## ADR-053 — Coordinated LESSON Phases 7–8
Status: Accepted

User-approved scope proceeds A products/enrollment → B cycles/ledger/renewal → C recurring classes
→ D private lessons → E group/attendance → F full transaction/concurrency verification. Phase 9 is excluded.

- Enrollment termination rejects PENDING/CONFIRMED bookings or ACTIVE reservations. Otherwise it
  ends Enrollment and cancels remaining ACTIVE/SCHEDULED cycles, without successor activation,
  automatic refund, deletion or zeroing ledger credits. Refund is a separate explicit operation.
- Schedule changes preserve past occurrences and future occurrences with ANY booking history.
  Only future unbooked occurrences may be regenerated. The new schedule governs new generation.
- TIME_BASED PURCHASE_DATE purchase is rejected while any ACTIVE cycle exists. It always starts
  ACTIVE; no SCHEDULED TIME_BASED purchase in MVP. Exactly one of validityDays or billingPeriod=MONTH
  defines its period; resolved historical period values are persisted. COUNT_BASED renewal rules remain.
- FIRST_USE BOOKING_CONFIRMED initializes dates at confirmation using Studio-local today. Deferred
  FIRST_USE uses the earliest outstanding lesson date as provisional start, with a validityDays window.
  Booking validates the resulting window; release of the earliest reservation can move it later.
  Actual successful completion/PRESENT initializes real dates and consumes/deducts atomically.
  Previously accepted future bookings must not cause real completion/attendance to fail or be auto-cancelled.
- Bulk attendance is all-or-nothing. PRESENT maps to Booking COMPLETED, ABSENT to NO_SHOW;
  CANCELLED follows the allowed booking cancellation state machine (or already CANCELLED).
  Same finalized result is idempotent; changing finalized attendance is rejected in this phase.
  Occurrence completion requires all linked bookings terminal and adds no implicit deduction.
- OWNER/MANAGER manage products, enrollments, classes/schedules, bookings, attendance, completion and
  renewal. Only OWNER adjusts counts or refunds. STAFF may complete/no-show assigned private lessons
  and read/finalize attendance for classes they instruct. Server verifies assignment. STAFF has no
  product/enrollment/class administration, payments, refund, adjustment, arbitrary reschedule/cancel or memo access.
- PassProduct price must be positive. Free passes, discounts and zero-price products are outside scope.

## ADR-054 — Group Rebooking and Instructor Occupancy
Status: Accepted

Confirmed by the user's instruction to apply the proposed ADR and proceed through Steps A–F:
- A customer cannot hold duplicate PENDING/CONFIRMED bookings in the same occurrence.
  Rebooking after CANCELLED is allowed only before attendance finalization. Prior booking and ledger
  history remain intact. Finalized attendance prevents another booking in the occurrence.
- One SCHEDULED occurrence occupies its instructor's time, independently of member capacity.
  Multiple members in that occurrence are allowed; overlapping private bookings or other occurrences
  for the same instructor are rejected. Preserved occurrences participate in this validation.
