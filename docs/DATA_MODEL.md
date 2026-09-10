# DATA_MODEL.md

# Data Model v1

PostgreSQL 기준. 모든 tenant-owned entity는 explicit `studio_id`를 가진다.

## 1. User
```text
User
- id UUID PK
- email VARCHAR nullable
- name VARCHAR
- status
- created_at
- updated_at
```

## 2. AuthAccount
```text
AuthAccount
- id UUID PK
- user_id UUID FK
- provider ENUM
- provider_user_id VARCHAR
- password_hash VARCHAR nullable
- created_at
```

Provider:
```text
EMAIL
GOOGLE
KAKAO
```

Unique:
```text
(provider, provider_user_id)
```

## 3. Studio
```text
Studio
- id UUID PK
- name VARCHAR NOT NULL
- slug VARCHAR NOT NULL
- business_category
- business_type
- configuration_version BIGINT NOT NULL DEFAULT 0 (Phase 3 stale-update guard)
- timezone
- status
- created_at
- updated_at
```

Constraint:
```text
UNIQUE(slug)
```

`slug`는 globally unique public tenant identifier다.

Phase 3: PRE_ONBOARDING은 category/type이 모두 null이고 ACTIVE는 유효한 category/type 조합이 필수다.
configuration_version은 설정 쓰기 transaction마다 증가하며 Studio 행 잠금과 함께 동시 변경을 보호한다.
설정 5개 테이블은 studio_id FK를 가지며 capability/day uniqueness 및 policy 값 CHECK를 사용한다.
category 간 capability/policy 제약은 backend가 강제한다 (ADR-051).

## 4. StudioMembership
```text
StudioMembership
- id UUID PK
- studio_id UUID FK
- user_id UUID FK
- role
- status
- created_at
```

Role:
```text
OWNER
MANAGER
STAFF
```

Unique:
```text
(studio_id, user_id)
```

## 5. Staff
```text
Staff
- id UUID PK
- studio_id UUID FK
- user_id UUID nullable FK
- name
- phone nullable
- active
- created_at
- updated_at
```

사업장 생성 시 OWNER 기본 Staff를 자동 생성한다.

## 6. Customer
```text
Customer
- id UUID PK
- studio_id UUID FK
- name
- phone
- normalized_phone
- memo
- status
- created_at
- updated_at
- archived_at nullable
```

Indexes:
```text
(studio_id, normalized_phone)
(studio_id, name)
```

Phase 4 Customer implementation: ACTIVE/ARCHIVED, non-destructive archive, UNIQUE(studio_id,
normalized_phone,name) including archived rows. Names are trimmed (100 characters), phone
input is at most 32 characters, and internal memo is at most 2000 characters. Matching strips
spaces/hyphens and accepts domestic 0-prefixed numeric values of 9–11 digits; no international
conversion or phone ownership verification is performed. Composite UNIQUE(studio_id,id)
supports tenant-safe foreign keys from payments/bookings. See ADR-052 for archive restrictions.

## 7. StudioCapability
```text
StudioCapability
- id UUID PK
- studio_id UUID FK
- capability
- enabled
```

Unique:
```text
(studio_id, capability)
```

## 8. BusinessHours
```text
BusinessHours
- id UUID PK
- studio_id UUID FK
- weekday
- open_time
- close_time
- closed
```

## 9. BookingPolicy
```text
BookingPolicy
- studio_id UUID PK/FK
- slot_interval_minutes
- booking_window_days
- cancellation_cutoff_hours
- updated_at
```

## 10. LessonPolicy
```text
LessonPolicy
- studio_id UUID PK/FK
- low_balance_threshold
- expiry_alert_days
- restore_on_timely_cancellation
- updated_at
```

## 11. BeautyPolicy
```text
BeautyPolicy
- studio_id UUID PK/FK
- deposit_enabled
- no_show_enabled
- updated_at
```

## 12. Booking
```text
Booking
- id UUID PK
- studio_id UUID FK
- customer_id UUID FK
- staff_id UUID nullable FK
- booking_kind
- start_at TIMESTAMPTZ
- end_at TIMESTAMPTZ
- status
- note nullable
- source
- created_at
- updated_at
```

Kind:
```text
LESSON_PRIVATE
LESSON_GROUP
BEAUTY_SERVICE
```

Status:
```text
PENDING
CONFIRMED
COMPLETED
CANCELLED
NO_SHOW
```

Source:
```text
OPERATOR
CUSTOMER_PORTAL
```

Phase 6 adds `Booking.manual_entry BOOLEAN NOT NULL` to distinguish offering-free manual operator
records from future specialized records. Manual entries require non-null staff, a 1:1 kind and OPERATOR
source. Booking/customer/staff references use composite tenant foreign keys. Interval and scope CHECKs
protect row integrity; Studio row locking plus transaction rechecks enforce cross-row occupancy (ADR-052).

## 13. BookingBlock

```text
BookingBlock
- id UUID PK
- studio_id UUID FK
- scope_type
- staff_id UUID nullable FK
- start_at TIMESTAMPTZ
- end_at TIMESTAMPTZ
- reason nullable
- created_at
```

Scope:
```text
STUDIO
STAFF
```

## 14. LessonBookingDetail
```text
LessonBookingDetail
- booking_id UUID PK/FK
- studio_id UUID FK
- enrollment_cycle_id UUID nullable FK
- class_occurrence_id UUID nullable FK
- lesson_mode
```

Mode:
```text
PRIVATE
GROUP
```

PRIVATE requires enrollment_cycle_id and no class_occurrence_id.
GROUP requires class_occurrence_id.

## 15. BeautyBookingDetail
```text
BeautyBookingDetail
- booking_id UUID PK/FK
- studio_id UUID FK
- beauty_service_id UUID FK
- service_name_snapshot
- price_snapshot BIGINT
- duration_minutes_snapshot
```

## 16. Payment
```text
Payment
- id UUID PK
- studio_id UUID FK
- customer_id UUID FK
- amount BIGINT
- currency
- method
- status
- reference_type
- reference_id UUID nullable
- paid_at nullable
- created_at
- created_by_user_id UUID nullable
```

Currency MVP: `KRW`

Method:
```text
CARD
CASH
TRANSFER
OTHER
```

Status:
```text
PENDING
PAID
REFUNDED
CANCELLED
```

## 17. PaymentRefund
```text
PaymentRefund
- id UUID PK
- studio_id UUID FK
- payment_id UUID FK
- amount BIGINT
- reason
- refunded_at
- created_by_user_id UUID
```

MVP full refund only.

Phase 5 implementation: Payment and PaymentRefund reference their parent with composite
tenant foreign keys. UNIQUE(studio_id,payment_id) limits full refunds to one per payment.
Monetary columns are BIGINT; JSON amounts use decimal strings to preserve long precision in browsers.
The current API only accepts OTHER/null references. Successful operator idempotency results
are stored transactionally, without expiry/key reuse or cleanup (expires_at remains null).
Future retention/cleanup needs an explicit replay-window decision before being enabled.

## 18. Notification
```text
Notification
- id UUID PK
- studio_id UUID FK
- customer_id UUID nullable FK
- type
- channel
- status
- scheduled_at nullable
- sent_at nullable
- created_at
```

## 19. PassProduct
```text
PassProduct
- id UUID PK
- studio_id UUID FK
- name
- product_type
- total_count nullable
- validity_days nullable
- validity_start_rule
- price BIGINT
- deduction_trigger nullable
- billing_period nullable
- active
- created_at
- updated_at
```

ProductType:
```text
COUNT_BASED
TIME_BASED
```

ValidityStartRule:
```text
PURCHASE_DATE
FIRST_USE
```

DeductionTrigger:
```text
BOOKING_CONFIRMED
ATTENDANCE_PRESENT
LESSON_COMPLETED
```

MVP validation:
- PRIVATE: BOOKING_CONFIRMED or LESSON_COMPLETED
- GROUP: BOOKING_CONFIRMED or ATTENDANCE_PRESENT
- TIME_BASED: PURCHASE_DATE only

## 20. Class
```text
Class
- id UUID PK
- studio_id UUID FK
- name
- capacity
- instructor_staff_id UUID nullable FK
- active
- created_at
```

## 21. ClassSchedule
```text
ClassSchedule
- id UUID PK
- studio_id UUID FK
- class_id UUID FK
- weekday
- start_time
- duration_minutes
- active
```

## 22. ClassOccurrence
```text
ClassOccurrence
- id UUID PK
- studio_id UUID FK
- class_id UUID FK
- class_schedule_id UUID nullable FK
- start_at TIMESTAMPTZ
- end_at TIMESTAMPTZ
- capacity_snapshot
- status
```

Status:
```text
SCHEDULED
CANCELLED
COMPLETED
```

## 23. Enrollment
```text
Enrollment
- id UUID PK
- studio_id UUID FK
- customer_id UUID FK
- kind
- class_id UUID nullable FK
- status
- created_at
- ended_at nullable
```

Kind:
```text
PRIVATE
GROUP
```

Status:
```text
ACTIVE
ENDED
```

## 24. EnrollmentCycle
```text
EnrollmentCycle
- id UUID PK
- studio_id UUID FK
- enrollment_id UUID FK
- pass_product_id UUID nullable FK
- product_name_snapshot
- product_type_snapshot
- purchased_count nullable
- validity_days_snapshot nullable
- purchase_price_snapshot BIGINT
- deduction_trigger_snapshot nullable
- validity_start_rule_snapshot
- start_date nullable
- valid_end_date nullable
- payment_date
- next_due_date nullable
- status
- created_at
```

Status:
```text
SCHEDULED
ACTIVE
COMPLETED
EXPIRED
CANCELLED
```

Constraints:
```sql
CREATE UNIQUE INDEX uq_enrollment_active_cycle
ON enrollment_cycles (enrollment_id)
WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_enrollment_scheduled_cycle
ON enrollment_cycles (enrollment_id)
WHERE status = 'SCHEDULED';
```

FIRST_USE cycle은 ACTIVE 상태에서도 start_date/valid_end_date가 null일 수 있다.

## 25. PassUsageLedger
```text
PassUsageLedger
- id UUID PK
- studio_id UUID FK
- enrollment_cycle_id UUID FK
- event_type
- amount INTEGER
- reason nullable
- reference_type NOT NULL
- reference_id UUID NOT NULL
- created_by_user_id UUID nullable
- created_at
```

Event:
```text
PURCHASE
BOOKING_DEDUCTION
ATTENDANCE
LESSON_COMPLETED
CANCEL_RESTORE
MANUAL_ADJUSTMENT
```

Authoritative balance:
```text
SUM(amount)
```

모든 ledger business effect는 reference를 반드시 가진다. 다음 tuple로 중복 방지한다.
```text
(studio_id, enrollment_cycle_id, event_type, reference_type, reference_id)
```


## 26. PassEntitlementReservation
```text
PassEntitlementReservation
- id UUID PK
- studio_id UUID FK
- enrollment_cycle_id UUID FK
- booking_id UUID UNIQUE FK
- status
- created_at
- resolved_at nullable
```

Status:
```text
ACTIVE
CONSUMED
RELEASED
```

COUNT_BASED + deferred deduction (`ATTENDANCE_PRESENT`, `LESSON_COMPLETED`)에서만 사용한다.

Available entitlement:
```text
SUM(PassUsageLedger.amount) - ACTIVE reservation count
```

동일 Booking은 entitlement reservation을 최대 하나만 가진다. Reservation consume과 ledger deduction은 같은 transaction에서 처리한다.

## 27. Attendance
```text
Attendance
- id UUID PK
- studio_id UUID FK
- class_occurrence_id UUID FK
- customer_id UUID FK
- enrollment_cycle_id UUID FK
- status
- recorded_at
- recorded_by_user_id UUID
```

Status:
```text
PRESENT
ABSENT
CANCELLED
```

Unique:
```text
(class_occurrence_id, customer_id)
```

## 28. BeautyService
```text
BeautyService
- id UUID PK
- studio_id UUID FK
- name
- duration_minutes
- price BIGINT
- revisit_days nullable
- active
- created_at
- updated_at
```

## 29. TreatmentRecord
```text
TreatmentRecord
- id UUID PK
- studio_id UUID FK
- customer_id UUID FK
- booking_id UUID UNIQUE FK
- beauty_service_id UUID FK
- staff_id UUID nullable FK
- service_name_snapshot
- service_price_snapshot BIGINT
- duration_minutes_snapshot
- internal_note nullable
- treated_at
- revisit_due_date nullable
- created_at
```

## 30. Deposit
```text
Deposit
- id UUID PK
- studio_id UUID FK
- booking_id UUID UNIQUE FK
- payment_id UUID nullable FK
- amount BIGINT
- status
- paid_at nullable
- created_at
```

Status:
```text
REQUIRED
PAID
REFUNDED
FORFEITED
```

## 31. IdempotencyRecord
```text
IdempotencyRecord
- id UUID PK
- studio_id UUID NOT NULL FK
- actor_type
- actor_id VARCHAR NOT NULL
- operation
- idempotency_key
- request_hash
- response_status nullable
- response_body nullable
- resource_type nullable
- resource_id UUID nullable
- created_at
- expires_at
```

ActorType:
```text
OPERATOR_USER
CUSTOMER_PORTAL
SYSTEM
```

Operation examples:
```text
CREATE_BOOKING
COMPLETE_LESSON
COMPLETE_TREATMENT
REFUND_PAYMENT
RENEW_ENROLLMENT
```

Unique:
```text
(studio_id, actor_type, actor_id, operation, idempotency_key)
```

Replay:
- same request_hash → previous result
- different request_hash → 409 Conflict

## 32. Future P1
```text
TreatmentPhoto
CustomerOtpChallenge
StaffWorkingHours
CustomerPortalBookingHistory
PartialRefund
BeautyBookingServiceItems
```


## PassEntitlementReservation Resolution Rules

`PassEntitlementReservation` status lifecycle must support:

```text
ACTIVE
CONSUMED
RELEASED
```

Resolution:

- configured deduction event -> `CONSUMED` + ledger deduction atomically
- cancellation before deferred deduction -> `RELEASED`
- PRIVATE `NO_SHOW` with `LESSON_COMPLETED` trigger -> `RELEASED`
- GROUP `ABSENT` with `ATTENDANCE_PRESENT` trigger -> `RELEASED`

No terminal Booking/Attendance outcome may leave an ACTIVE entitlement reservation stranded.

COUNT_BASED expiration does not modify ledger rows merely to zero unused entitlement. Historical positive balance may remain on an EXPIRED cycle.
