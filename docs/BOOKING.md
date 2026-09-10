# BOOKING.md

# Booking Domain Specification

## Current Phase 6 manual scope (ADR-052)

The operator may create a clearly marked manual 1:1 booking without an offering in this phase.
`manual_entry=true`, `source=OPERATOR`; kind matches the Studio category, staff is required,
and the default OWNER Staff is available. LESSON_PRIVATE creation requires PRIVATE_LESSON enabled.
LESSON_GROUP and all specialized detail/eligibility/side-effect implementations remain deferred.
The regular creation status stays CONFIRMED (ADR-016); PENDING is retained in the state machine.
Existing manual records remain distinguishable when specialized bookings are introduced.

Only PENDING/CONFIRMED records may change time, staff or note; customer/kind do not change.
Create/reschedule rejects past starts and validates Studio-local hours. The three BookingPolicy
fields do not restrict these operator commands; future public/entitlement flows apply their policies.
Terminal commands only change common status and never perform pass, treatment, deposit or revisit effects.
Common commands reject non-manual records, so future specialization cannot silently skip its rules.

Every booking/block mutation locks the Studio row first in a READ COMMITTED PostgreSQL transaction.
Then it rechecks occupancy and writes the resource plus successful idempotency result atomically.
Customer archival uses the same lock and rejects PENDING/CONFIRMED bookings. Both occupying statuses
also prevent overlapping block creation. Different staff may occupy the same interval, and adjacent
half-open intervals do not overlap. Future GROUP capacity is excluded from 1:1 occupancy queries/indexes.
There is no Redis booking lock or extension dependency; direct SQL outside this protocol is not an API.
Availability is an advisory read; creation/rescheduling always rechecks inside the write transaction.

STAFF sees assigned bookings and customer name/phone only; booking note and internal customer memo are excluded.
Successful operator command results are retained without automatic expiry or key reuse in this implementation.
Any future replay retention/cleanup policy must be specified before enabling expiry.

## 1. Goal

LESSON과 BEAUTY가 하나의 예약 엔진을 공유한다.
`Booking`은 공통 시간 점유와 상태를 관리하고 업종별 eligibility/completion 규칙은 specialization module이 처리한다.

---

## 2. Booking Types

```text
LESSON_PRIVATE
LESSON_GROUP
BEAUTY_SERVICE
```

---

## 3. Status

```text
PENDING
CONFIRMED
COMPLETED
CANCELLED
NO_SHOW
```

Allowed transitions:

```text
PENDING → CONFIRMED
PENDING → CANCELLED
CONFIRMED → COMPLETED
CONFIRMED → CANCELLED
CONFIRMED → NO_SHOW
```

Terminal:

```text
COMPLETED
CANCELLED
NO_SHOW
```

---

## 4. Capacity Occupancy

Occupying statuses:

```text
PENDING
CONFIRMED
```

---

## 5. Private Lesson Conflict

Conflict when:

```text
same studio
same staff
time overlaps
booking status occupies capacity
```

Overlap:

```text
existing.start < requested.end
AND
existing.end > requested.start
```

---

## 6. Beauty Conflict

BEAUTY 1:1 예약도 동일한 staff overlap 규칙을 사용한다.
MVP에서는 한 Booking당 BeautyService 하나다.

---

## 7. Group Lesson

Group booking은 staff-exclusive slot model이 아니다.

```text
Booking → ClassOccurrence
```

가능 여부:

```text
PENDING + CONFIRMED count < capacity_snapshot
```

동일 occurrence에 여러 Customer booking이 존재할 수 있다.

---

## 8. ClassOccurrence

```text
Class
→ ClassSchedule
→ ClassOccurrence
```

Booking과 Attendance는 ClassOccurrence에 연결한다.
향후 90일 occurrence를 생성하고 rolling generation으로 유지한다.
Past occurrence는 schedule 변경으로 수정하지 않는다.
Existing booking이 있는 future occurrence는 자동 파괴하지 않는다.

---

## 9. Business Hours

Booking은 Studio BusinessHours 내에서만 가능하다.
추가로 Studio/Staff BookingBlock, existing booking, class occurrence, service duration을 검사한다.

---

## 10. Booking Policy

```text
slotIntervalMinutes
bookingWindowDays
cancellationCutoffHours
```

Initial defaults:

```text
30
30
12
```

---

## 11. Operator Booking Flow

```text
Operator authenticated
↓
Studio membership verified
↓
Customer selected
↓
Category-specific offering selected
↓
Availability check
↓
Transaction
↓
Availability recheck
↓
Booking insert
↓
Commit
```

---

## 12. Customer Booking Portal

```text
/book/{studioSlug}
```

Identification 이후 portal session으로 접근한다.

---

## 13. LESSON Public Booking

```text
Customer identified
↓
active Enrollment
↓
active EnrollmentCycle
↓
valid entitlement
↓
eligible lesson
↓
available slot
↓
Booking
```

Validation:
- Enrollment active
- Cycle ACTIVE
- not expired
- sufficient available entitlement when count-based (`ledger balance - active entitlement reservations`)
- category LESSON
- staff/class occurrence valid
- all resources same Studio

Unknown customer auto creation 금지.

---

## 14. BEAUTY Public Booking

```text
Customer identified/created
↓
BeautyService
↓
service duration
↓
staff/default staff
↓
available slot
↓
Booking
```

Validation:
- service active
- service/staff same Studio
- category BEAUTY
- time available

---

## 15. Deduction Trigger Compatibility

### PRIVATE
Allowed:

```text
BOOKING_CONFIRMED
LESSON_COMPLETED
```

Not allowed:

```text
ATTENDANCE_PRESENT
```

### GROUP
Allowed:

```text
BOOKING_CONFIRMED
ATTENDANCE_PRESENT
```

Not allowed in MVP:

```text
LESSON_COMPLETED
```

Backend가 잘못된 조합을 거부한다.

---

## 16. FIRST_USE Handling

FIRST_USE를 사용하는 COUNT_BASED cycle은 ACTIVE 직후에도:

```text
startDate = null
validEndDate = null
```

일 수 있다.

첫 실제 deduction event에서:

```text
startDate = Studio timezone 기준 LocalDate
validEndDate = startDate + validityDays
```

를 설정하고 같은 transaction에서 ledger deduction을 수행한다.

TIME_BASED product는 MVP에서 FIRST_USE를 지원하지 않는다.

---

## 17. Lesson Booking Completion

```text
Booking CONFIRMED
→ Booking COMPLETED
```

cycle trigger가 `LESSON_COMPLETED`일 때만 `LESSON_COMPLETED -1` ledger event를 생성한다.
Repeated completion은 중복 ledger effect를 만들 수 없다.

---

## 18. Attendance Deduction

Group Attendance가 PRESENT이고 cycle trigger가 `ATTENDANCE_PRESENT`일 때만 차감한다.
동일 Attendance에 대한 deduction은 한 번만 생성한다.

---

## 19. Booking-Confirmed Deduction

cycle trigger가 `BOOKING_CONFIRMED`이면 booking confirmation 시 차감한다.
Cancellation restore는 LessonPolicy에 따라 처리한다.

---

## 20. Cancellation Restore

`restoreOnTimelyCancellation=true`이고 cancellationCutoff 이전 취소면 기존 deduction이 있을 경우 정확히 한 번 `CANCEL_RESTORE +1`을 만든다.
No-show는 MVP 기본값으로 자동 restore하지 않는다.

---

## 21. Scheduled Cycle Activation

재등록 시 기존 ACTIVE cycle이 있으면 새 cycle은 SCHEDULED다.
Enrollment당 SCHEDULED cycle은 최대 하나다.

현재 ACTIVE cycle이:

```text
COMPLETED
EXPIRED
CANCELLED
```

중 하나가 되면 SCHEDULED cycle을 ACTIVE로 전환한다.
SCHEDULED cycle은 public booking eligibility에 사용할 수 없다.

---

## 22. Booking Block

Scopes:

```text
STUDIO
STAFF
```

STUDIO block은 모든 1:1 booking을 차단한다.
STAFF block은 해당 Staff의 1:1 booking만 차단한다.
이미 존재하는 CONFIRMED booking과 겹치는 block 생성은 기본적으로 거부한다.

---

## 23. Database Correctness

PostgreSQL이 최종 source of truth다.

```text
1. validate actor/resource scope
2. transaction start
3. re-read conflicting occupancy
4. constraint / lock validation
5. insert/update
6. commit
```

Redis distributed lock은 required correctness mechanism이 아니다.

---

## 24. Booking Idempotency

Namespace:

```text
studioId
+
actorType
+
actorId
+
operation
+
idempotencyKey
```

Customer portal actorId는 portalSessionId, operator actorId는 userId를 사용한다.
Replay 전 authorization을 다시 검증한다.

```text
same requestHash → previous successful result
other requestHash → 409 Conflict
```

---

## 25. Timezone

Persisted timestamp는 `TIMESTAMPTZ`를 사용한다.
요일/날짜 policy 계산은 Studio timezone 기준이다.
Frontend browser timezone을 business rule source로 사용하지 않는다.

---

## 26. Redis

MVP Redis usage:
- Spring Session
- public rate limiting
- customer portal session
- temporary verification state
- safe configuration cache

Booking distributed lock은 테스트에서 필요성이 확인될 때만 추가한다.

---

## 26. Entitlement Reservation for Deferred Deduction

COUNT_BASED product에서 trigger가 `ATTENDANCE_PRESENT` 또는 `LESSON_COMPLETED`이면 booking confirmation 시 1회 entitlement reservation을 생성한다.

```text
available entitlement = ledger balance - active reservation count
```

Booking confirmation transaction:

```text
cycle lock/read
→ available entitlement >= 1 검증
→ Booking CONFIRMED
→ PassEntitlementReservation ACTIVE create
→ commit
```

동일 booking은 ACTIVE entitlement reservation을 최대 하나만 가질 수 있다.

실제 configured trigger 발생 시:

```text
reservation ACTIVE -> CONSUMED
+ PassUsageLedger -1
```

을 같은 transaction에서 처리한다.

Trigger 전에 booking이 CANCELLED 되면:

```text
reservation ACTIVE -> RELEASED
```

만 수행하고 `CANCEL_RESTORE` ledger는 생성하지 않는다.

`BOOKING_CONFIRMED` trigger에서는 reservation을 만들지 않고 confirmation transaction에서 ledger -1을 직접 생성한다.

---

## 27. Cycle Completion and Successor Activation

COUNT_BASED cycle은 아래가 모두 참일 때만 COMPLETED로 전환한다.

```text
ledger balance == 0
active reservation == 0
cycle과 연결된 PENDING/CONFIRMED booking == 0
```

따라서 마지막 회차가 예약되었다는 이유만으로 즉시 successor를 활성화하지 않는다. 해당 booking이 완료/출석/취소되어 entitlement 결과가 확정된 뒤 cycle terminal 여부를 평가한다.

TIME_BASED cycle은 valid end date 이후 신규 booking에 사용할 수 없다. 이미 존재하는 booking이 있으면 해당 booking이 모두 terminal 상태가 된 뒤 EXPIRED 처리한다.

Cycle이 terminal 상태가 된 뒤에만 SCHEDULED successor를 ACTIVE로 전환한다.

---

## 28. Cancellation Restoration

### Deferred deduction product

차감 전 취소:

```text
ACTIVE reservation -> RELEASED
```

ledger restore 없음.

### BOOKING_CONFIRMED deduction product

정책상 restore가 허용되는 취소라면 original cycle이 ACTIVE인 동안 정확히 한 번:

```text
CANCEL_RESTORE +1
```

을 기록한다.

Cycle terminal 전환은 unresolved booking이 모두 해결된 이후에만 일어나므로 정상 flow에서는 successor activation 이후 자동 restore가 발생하지 않는다. Terminal cycle에 대한 사후 correction은 자동 reopen이 아니라 OWNER manual adjustment로 처리한다.



## 29. COUNT_BASED Expiration

COUNT_BASED cycle에 validity가 설정되어 있으면 `valid_end_date`가 지난 이후 신규 booking eligibility를 잃는다.

Booking 생성 시 단순히 요청 시점만 확인하지 않고 실제 예정 수업 시간이 cycle validity 안에 있는지 검증한다.

```text
lesson scheduled after valid_end_date
-> booking rejected for that cycle
```

Validity boundary 시점에 unresolved booking / ACTIVE entitlement reservation이 없으면 즉시:

```text
ACTIVE -> EXPIRED
```

미해결 booking/reservation이 있으면 cycle은 settlement를 위해 일시적으로 ACTIVE 상태를 유지할 수 있지만 신규 booking에는 사용할 수 없다. 마지막 미해결 건이 해결된 뒤 `EXPIRED`로 전환한다.

남은 ledger credit은 만료 시 강제로 0으로 만들지 않는다. 역사적 unused entitlement로 보존하며, `EXPIRED` 상태 자체가 사용을 금지한다.

기존 booking이 validity 종료 후 취소되는 경우:

- deferred deduction: reservation RELEASED, ledger restore 없음
- BOOKING_CONFIRMED: cancellation policy가 허용하는 경우 original cycle에 CANCEL_RESTORE 기록

그 후 즉시 expiration/terminal 상태를 재평가한다. 복구된 credit 때문에 해당 cycle이 다시 신규 booking 가능 상태가 되지는 않는다.

---

## 30. NO_SHOW / ABSENT Reservation Resolution

Deferred deduction product에서 no-show/absence는 configured deduction trigger가 아니므로 MVP에서는 회차를 차감하지 않는다.

### PRIVATE + LESSON_COMPLETED

Booking이 `NO_SHOW`가 되면:

```text
Booking CONFIRMED -> NO_SHOW
PassEntitlementReservation ACTIVE -> RELEASED
no ledger deduction
```

### GROUP + ATTENDANCE_PRESENT

Attendance가 `ABSENT`로 기록되면:

```text
Attendance -> ABSENT
related Booking -> NO_SHOW
PassEntitlementReservation ACTIVE -> RELEASED
no ledger deduction
```

Attendance `CANCELLED`는 documented booking cancellation flow를 따른다. 차감 전이면 reservation을 release한다.

### BOOKING_CONFIRMED

이미 confirmation에서 ledger -1이 기록되므로 reservation은 존재하지 않는다.

```text
NO_SHOW / ABSENT
-> automatic restore 없음
```

No-show/absence는 cancellation이 아니므로 `CANCEL_RESTORE`를 자동 생성하지 않는다.

모든 NO_SHOW / ABSENT 처리 transaction 마지막에는 cycle completion/expiration을 재평가한다. ACTIVE reservation이 stranded 상태로 남으면 안 된다.
