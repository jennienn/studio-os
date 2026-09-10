# DOMAIN_RULES.md

# Domain Rules

이 문서는 제품 domain의 source of truth이다.
코드 구현이 본 문서와 충돌하면 본 문서가 우선한다.

## 1. Product

```text
예약 기반 소규모 서비스 사업장을 위한 맞춤형 운영 SaaS
```

Business category:

```text
LESSON
BEAUTY
```

---

## 2. Strict Category Hierarchy

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

LESSON에서 BEAUTY subtype을, BEAUTY에서 LESSON subtype을 노출할 수 없다.
Backend에서도 category/subtype 조합을 검증한다.

---

## 3. Category Change

온보딩 완료 후 ordinary settings에서 `LESSON ↔ BEAUTY` 변경은 금지한다.
향후 별도 migration 기능에서만 지원한다.

OWNER는 현재 category 안에서만 businessType을 변경할 수 있다 (ADR-051).
세부 업종 변경은 과거 데이터의 삭제나 자동 migration을 수행하지 않는다.

---

## 4. Common Core

```text
Studio
User
AuthAccount
StudioMembership
Staff
Customer
Booking
BookingBlock
Payment
Notification
BusinessHours
BookingPolicy
```

---

## 5. LESSON Exclusive

```text
PassProduct
Enrollment
EnrollmentCycle
PassUsageLedger
Class
ClassSchedule
ClassOccurrence
Attendance
```

BEAUTY Studio에서 활성화할 수 없다.

---

## 6. BEAUTY Exclusive

```text
BeautyService
TreatmentRecord
Deposit
RevisitRule
TreatmentPhoto(P1)
```

LESSON Studio에서 활성화할 수 없다.

---

## 7. Customer

Persistence name은 `Customer` 하나다.
LESSON UI는 `회원`, BEAUTY UI는 `고객`을 사용한다.
별도 Member table을 만들지 않는다.
Customer와 User를 합치지 않는다.

---

## 8. Business Configuration

Category는 기능의 최대 boundary를 결정한다.
Capabilities는 category 내부 기능만 활성화/비활성화한다.
Capability로 다른 category domain을 활성화할 수 없다.

Phase 3 allowlist (ADR-051):
- LESSON: PRIVATE_LESSON, GROUP_CLASS, ATTENDANCE, CUSTOMER_BOOKING, PASS_MANAGEMENT
- BEAUTY: CUSTOMER_BOOKING, DEPOSIT, REVISIT

ACTIVE LESSON은 PASS_MANAGEMENT 필수이며 PRIVATE_LESSON 또는 GROUP_CLASS가 하나 이상 켜져야 한다.
ATTENDANCE는 GROUP_CLASS가 켜져 있을 때만 허용한다. 두 기능을 같은 설정 요청에서 함께 끄는 것은 가능하다.
CUSTOMER_BOOKING은 독립적으로 선택 가능하다. BEAUTY의 DEPOSIT와 BeautyPolicy.depositEnabled는 항상 같다.
후속 LESSON 구현에서 ATTENDANCE_PRESENT는 GROUP_CLASS와 ATTENDANCE가 모두 켜진 경우에만 허용한다.

---

## 9. Capability Disable

```text
feature disabled
→ new operations hidden/restricted
→ historical data preserved
```

기존 데이터를 삭제하지 않는다.
Integrity 문제가 생기면 disable 요청을 거부한다.

---

## 10. LESSON Pass

Pass count는 arbitrary integer다.
4/8/12를 hardcode하지 않는다.

---

## 11. Pass Balance

Pass balance의 source of truth는 `PassUsageLedger`다.

```text
remaining = SUM(ledger.amount)
```

MVP에서 mutable remaining_count/used_count를 authoritative data로 사용하지 않는다.

---

## 12. Deduction Trigger Rules

Count-based cycle에는 정확히 하나의 trigger가 존재한다.

PRIVATE allowed:

```text
BOOKING_CONFIRMED
LESSON_COMPLETED
```

GROUP allowed:

```text
BOOKING_CONFIRMED
ATTENDANCE_PRESENT
```

금지:

```text
PRIVATE + ATTENDANCE_PRESENT
GROUP + LESSON_COMPLETED
```

Backend가 이 규칙을 강제한다.

---

## 13. Enrollment Cycle Lifecycle

Enrollment당:

```text
ACTIVE <= 1
SCHEDULED <= 1
```

재등록 시 ACTIVE cycle이 존재하면 새 cycle은 SCHEDULED다.
기존 ACTIVE cycle이 `COMPLETED`, `EXPIRED`, `CANCELLED`가 되면 SCHEDULED cycle이 ACTIVE로 전환된다.
SCHEDULED cycle은 booking eligibility에 사용할 수 없다.

---

## 14. Validity Rules

COUNT_BASED:

```text
PURCHASE_DATE
FIRST_USE
```

TIME_BASED MVP:

```text
PURCHASE_DATE only
```

FIRST_USE는 최초 configured deduction event다.
FIRST_USE 이전 ACTIVE cycle의 startDate/validEndDate는 null일 수 있다.
첫 deduction transaction에서 날짜와 ledger를 함께 처리한다.

---

## 15. Renewal

재등록은 기존 cycle overwrite가 아니다.

```text
old cycle preserved
+
new cycle
```

기존 booking/attendance/ledger/payment history를 보존한다.
자동 잔여 회차 이월은 없다.
수동 이월은 `MANUAL_ADJUSTMENT`와 reason으로 처리한다.

---

## 16. LESSON Public Booking

```text
name + phone
→ existing Customer
→ active Enrollment
→ active EnrollmentCycle
```

없는 회원 자동 생성 금지.

---

## 17. BEAUTY Public Booking

```text
name + phone
→ existing customer OR new customer
```

신규 고객 예약을 허용한다.

---

## 18. Booking Core

LESSON/BEAUTY가 별도 booking store를 만들지 않는다.
모든 예약은 common `Booking`을 사용하고 category-specific detail만 분리한다.

---

## 19. Private Occupancy

```text
same staff + overlapping time + occupying status
→ conflict
```

---

## 20. Group Capacity

Group booking은 `ClassOccurrence.capacity_snapshot`을 기준으로 한다.
동일 occurrence에 여러 Customer booking이 가능하다.

---

## 21. Completion

LESSON completion은 trigger-dependent ledger effect를 포함한다.
BEAUTY completion은 TreatmentRecord + revisitDueDate를 포함한다.
모두 transaction으로 처리한다.

---

## 22. Beauty Service

MVP:

```text
Booking 1 → BeautyService 1
```

multi-service는 P1.

---

## 23. Treatment History

TreatmentRecord는 완료 시 생성되는 historical record다.
서비스명/가격/시간 snapshot을 포함한다.

---

## 24. Revisit

Treatment 완료 시 revisit due date를 snapshot한다.
BeautyService rule 변경이 과거 TreatmentRecord를 바꾸지 않는다.

---

## 25. Public Studio Identity

Studio.slug는 globally unique다.

```text
/book/{studioSlug}
```

는 정확히 하나의 Studio를 식별해야 한다.

---

## 26. Idempotency Boundary

```text
Studio
Actor
Operation
Key
```

다른 tenant/actor의 동일 key가 서로 영향을 주면 안 된다.
Replay에서도 actor authorization을 다시 검증한다.

---

## 27. Redis Rule

Redis는 예약 정합성의 source of truth가 아니다.

MVP responsibilities:

```text
Spring Session
Customer Portal Session
Rate Limiting
Temporary Verification State
Configuration Cache
```

Booking correctness는 PostgreSQL transaction/recheck/constraints/locking으로 보장한다.

---

## 28. Kakao Scope

```text
Kakao OAuth login → MVP
Kakao 알림톡 → P1
```

---

## 29. Customer Verification

MVP portal:

```text
name + phone
+
short-lived Redis portal session
```

SMS OTP는 P1이며 name+phone 방식은 controlled pilot 범위에서 사용한다.

---

## 30. Tenant Rule

모든 tenant-owned domain object는 explicit `studio_id`를 가진다.
다른 Studio의 entity를 reference할 수 없다.

---

## 31. Business Logic Location

다음 판단은 Spring backend에서 수행한다.

```text
booking eligibility
booking availability
tenant authorization
deduction
renewal
refund
treatment completion
revisit calculation
category/capability validation
```

Frontend를 source of truth로 사용하지 않는다.

---

## 26. Deferred Deduction Reservation

COUNT_BASED cycle에서 차감 trigger가 `ATTENDANCE_PRESENT` 또는 `LESSON_COMPLETED`이면 예약 확정 시 회차를 즉시 ledger에서 차감하지 않는다. 대신 1회 entitlement를 `PassEntitlementReservation`으로 hold한다.

```text
available entitlement
= ledger balance - active reservations
```

available entitlement가 1 미만이면 추가 예약을 확정할 수 없다.

실제 trigger가 발생하면 reservation consume과 ledger -1을 같은 transaction에서 처리한다. 예약이 trigger 전에 취소되면 reservation만 release한다.

`BOOKING_CONFIRMED` trigger는 confirmation 시 ledger를 직접 차감하므로 별도 reservation을 만들지 않는다.

---

## 27. Cycle Completion

COUNT_BASED cycle은 다음 조건을 모두 만족할 때 `COMPLETED`가 된다.

```text
ledger balance == 0
active entitlement reservations == 0
cycle에서 차감/예약된 unresolved PENDING/CONFIRMED booking == 0
```

TIME_BASED cycle은 valid end date가 지나면 신규 예약 eligibility를 잃는다. 기존 예약이 모두 해결된 뒤 `EXPIRED`가 된다.

이 규칙 때문에 successor SCHEDULED cycle은 이전 cycle의 취소/복구 가능성이 남아 있는 동안 ACTIVE가 되지 않는다.

Terminal cycle은 자동으로 reopen하지 않는다. 예외적인 사후 보정은 OWNER의 `MANUAL_ADJUSTMENT`와 reason으로만 처리한다.

---

## 28. Lesson Refund

MVP lesson full refund는 해당 cycle에 실제 사용 완료분과 unresolved booking/reservation이 없을 때만 허용한다.

허용 조건:

```text
cycle = ACTIVE or SCHEDULED
no consumed negative usage effect
no active entitlement reservation
no PENDING/CONFIRMED booking tied to cycle
```

성공 시 Payment는 REFUNDED, cycle은 CANCELLED가 된다. 사용 이력 또는 미해결 예약이 있으면 자동 refund를 거부한다.



## 29. COUNT_BASED Expiration

Validity가 있는 COUNT_BASED cycle은 unused credits가 남아 있어도 만료될 수 있다.

`valid_end_date` 이후:

```text
new booking eligibility = false
```

미해결 booking/reservation이 없으면 `EXPIRED`로 전환한다. 미해결 건이 있으면 신규 booking에는 사용할 수 없는 settlement-only ACTIVE 상태로 남았다가, 마지막 미해결 건 해결 후 `EXPIRED`가 된다.

Unused ledger credit은 synthetic deduction으로 0 처리하지 않는다. 역사적으로 보존한다.

SCHEDULED successor는 기존 cycle이 실제 terminal 상태(`COMPLETED`, `EXPIRED`, `CANCELLED`)가 된 후에만 ACTIVE가 된다.

Booking 예정 시간이 cycle validity 밖이면 해당 cycle로 예약할 수 없다.

---

## 30. NO_SHOW / ABSENT and Deferred Entitlement

Deferred deduction에서 no-show/absence는 MVP deduction trigger가 아니다.

PRIVATE + `LESSON_COMPLETED`:

```text
NO_SHOW
-> reservation RELEASED
-> no ledger deduction
```

GROUP + `ATTENDANCE_PRESENT`:

```text
ABSENT
-> Booking NO_SHOW
-> reservation RELEASED
-> no ledger deduction
```

`BOOKING_CONFIRMED` product는 이미 차감되었으므로 NO_SHOW/ABSENT에서 자동 restore하지 않는다. No-show/absence는 cancellation으로 간주하지 않는다.

NO_SHOW/ABSENT 처리 후 cycle terminal/expiration 상태를 반드시 다시 평가한다.

MVP에서는 no-show penalty를 별도 deduction trigger로 추가하지 않는다.
