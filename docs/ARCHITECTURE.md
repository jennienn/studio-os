# ARCHITECTURE.md

# System Architecture

## 1. High-Level Architecture

```text
Browser
  ↓
Next.js Frontend
  ↓ REST / JSON
Spring Boot Modular Monolith
  ├── Auth
  ├── Studio
  ├── Customer
  ├── Booking
  ├── Payment
  ├── Notification
  ├── Staff
  ├── Lesson Module
  └── Beauty Module
  ↓
PostgreSQL

Supporting infrastructure:
- Redis: sessions, portal sessions, rate limiting, temporary state, cache
- Cloudflare R2: treatment photos and files (P1 use cases)
```

---

## 2. Repository Structure

```text
/
├── frontend/
├── backend/
├── docs/
├── AGENTS.md
├── README.md
└── docker-compose.yml
```

---

## 3. Backend Style

Spring Boot modular monolith.
예상 package boundaries:

```text
auth
studio
customer
staff
booking
payment
notification
lesson.pass
lesson.enrollment
lesson.class
lesson.attendance
beauty.service
beauty.treatment
beauty.revisit
beauty.deposit
```

Domain boundary를 package로 명확히 분리한다.

---

## 4. Frontend Responsibility

Next.js는:
- landing
- auth UI
- onboarding
- operator dashboard
- customer/member management
- booking calendar
- lesson UI
- beauty UI
- customer booking portal

을 담당한다.

Frontend는 business rule의 source of truth가 아니다.

---

## 5. Persistence

PostgreSQL은 모든 영속 business data의 source of truth다.
Flyway로 schema를 관리한다.
Hibernate `ddl-auto=validate`를 사용한다.

---

## 6. Multi-Tenancy

Tenant boundary는 Studio다.
모든 tenant-owned row는 explicit `studio_id`를 가진다.
Operator request는 StudioMembership을 검증한 후 AuthorizedStudioContext를 만든다.

---

## 7. Authentication

Operator:

```text
Email + Password
Google OAuth
Kakao OAuth
→ Spring Security
→ Spring Session
→ Redis
→ HttpOnly Cookie
```

Customer portal:

```text
name + phone
→ studio-local identification
→ short-lived Redis portal session
→ HttpOnly Cookie
```

---

## 8. Common Booking Engine

```text
Booking
├── LessonBookingDetail
└── BeautyBookingDetail
```

LESSON private:

```text
Booking → Staff → EnrollmentCycle
```

LESSON group:

```text
Booking → ClassOccurrence
```

BEAUTY:

```text
Booking → Staff → BeautyService
```

---

## 9. Booking Correctness

PostgreSQL이 최종 source of truth다.

```text
authorization
↓
transaction
↓
availability recheck
↓
database constraint / locking
↓
Booking insert/update
↓
commit
```

Redis distributed lock은 MVP 필수가 아니다.
실제 concurrency test에서 필요성이 확인된 경우에만 추가한다.

---

## 10. Transaction Boundaries

### Lesson Booking Confirmation

Cycle deduction trigger가 `BOOKING_CONFIRMED`일 때만:

```text
Booking confirmation
+
FIRST_USE initialization if required
+
PassUsageLedger deduction
```

을 하나의 transaction으로 처리한다.

### Lesson Completion

```text
Booking CONFIRMED
→ Booking COMPLETED
```

`deductionTrigger == LESSON_COMPLETED`인 경우에만 ledger -1을 같은 transaction에서 처리한다.
Completion 자체가 항상 deduction을 의미하지 않는다.

### Group Attendance

`deductionTrigger == ATTENDANCE_PRESENT`이면:

```text
Attendance PRESENT
+
FIRST_USE initialization if required
+
PassUsageLedger -1
```

을 동일 transaction에서 처리한다.
동일 Attendance event의 중복 deduction은 reference uniqueness로 막는다.

### Renewal

재등록은 기존 ACTIVE cycle을 강제로 종료하지 않는다.

```text
Payment PAID
+
new EnrollmentCycle
```

기존 ACTIVE cycle이 없으면 새 cycle은 ACTIVE.
기존 ACTIVE cycle이 있으면 새 cycle은 SCHEDULED.
기존 ACTIVE cycle이 terminal 상태가 되면 SCHEDULED cycle을 ACTIVE로 전환한다.

### Beauty Completion

```text
Booking COMPLETED
+
TreatmentRecord create
+
revisitDueDate calculation
```

을 하나의 transaction으로 처리한다.
동일 Booking에서 TreatmentRecord는 최대 하나다.

---

## 11. Pass Ledger

PassUsageLedger가 authoritative source다.

```text
balance = SUM(amount)
```

MVP에서 used_count/remaining_count를 source of truth로 사용하지 않는다.

---

## 12. Notifications

Application domain은 vendor와 분리한다.

```text
NotificationService
→ KakaoNotificationProvider (P1)
→ SmsNotificationProvider (P1)
→ EmailNotificationProvider (P1)
```

MVP는 intent/status persistence와 mock/manual delivery state까지 구현한다.

---

## 13. Files

Cloudflare R2는 파일 binary를 저장한다.
DB에는 object key/metadata만 저장한다.
Treatment photos는 P1.

---

## 14. API

REST `/api/v1`을 기본으로 한다.
DTO를 사용하고 JPA entity를 직접 serialize하지 않는다.
에러 응답은 traceId를 포함한다.

---

## 15. Observability

Backend:
- Actuator
- Micrometer

향후:
- Prometheus
- Grafana

민감 정보는 로그에 남기지 않는다.

---

## Entitlement Reservation for Deferred Lesson Deduction

For COUNT_BASED cycles using `ATTENDANCE_PRESENT` or `LESSON_COMPLETED`, booking confirmation reserves one entitlement without immediately writing a negative usage ledger entry.

```text
available entitlement
= ledger balance - ACTIVE PassEntitlementReservation count
```

At the configured deduction event, reservation consumption and the ledger -1 happen atomically. Cancellation before deduction releases the reservation.

This prevents multiple future bookings from consuming the same remaining credit.

## Cycle Terminal Evaluation

COUNT_BASED cycles become COMPLETED only when balance is zero, no ACTIVE entitlement reservation remains, and no PENDING/CONFIRMED booking tied to that cycle remains. TIME_BASED cycles stop accepting new bookings after valid end date and become EXPIRED after outstanding bookings are resolved.

Successor SCHEDULED activation occurs only after this terminal evaluation.

## Lesson Refund Integrity

A lesson-cycle full refund is allowed in MVP only when no entitlement has been consumed and no outstanding booking/reservation remains. The refund transaction updates Payment/PaymentRefund and cancels the associated cycle atomically.

## Phase 7–8 LESSON integration

The LESSON module extends the common Booking aggregate through specialization and additional
occupancy services. The common booking transaction still owns time/status changes; LESSON handlers
validate the linked cycle, reserve or consume entitlement and append ledger effects inside that same
transaction. Scheduled occurrences participate in instructor occupancy without applying exclusive
member capacity semantics to the shared Booking table.

Cycle purchase, renewal, termination, refund effects and configuration dependency checks stay in
Spring services. PostgreSQL Studio-row locking serializes cross-aggregate writes, and occurrence-row
locking protects group capacity and attendance. Partial unique indexes and reference uniqueness remain
the final guard for cycle slots, generated occurrences, attendance and ledger effects. Redis stores
server sessions only and is not consulted for LESSON balance or booking correctness.
