# PAYMENTS.md

# Payment Domain Specification

## 1. MVP Scope

MVP에서는 실제 PG 결제를 처리하지 않는다.
운영자가 실제로 받은 결제를 시스템에 기록한다.

Methods:

```text
CARD
CASH
TRANSFER
OTHER
```

Currency:

```text
KRW
```

금액은 원 단위 BIGINT로 저장한다.

---

## 2. Payment Status

```text
PENDING
PAID
REFUNDED
CANCELLED
```

---

## 3. Payment References

```text
LESSON_CYCLE
RENEWAL
BEAUTY_TREATMENT
DEPOSIT
OTHER
```

---

## 4. Renewal Payment

`결제 필요` 상태는 Payment 자체가 아니다.

```text
renewal needed
↓
operator receives payment
↓
payment confirmation
↓
Payment PAID + new EnrollmentCycle
```

하나의 transaction으로 처리한다.
기존 ACTIVE cycle이 있으면 새 cycle은 SCHEDULED, 없으면 ACTIVE다.

---

## 5. Refund

MVP는 full refund only.
Paid payment를 삭제하지 않는다.

```text
Payment PAID
↓
PaymentRefund create
↓
Payment REFUNDED
```

Refund reason 필수.

---

## 6. Cancellation

CANCELLED는 실제 결제가 없었거나 잘못 입력한 payment record를 취소하는 용도다.
이미 실제 돈을 받은 PAID payment는 refund flow를 사용한다.

---

## 7. Partial Refund

P1.

---

## 8. Deposit

Beauty deposit status:

```text
REQUIRED
PAID
REFUNDED
FORFEITED
```

MVP에서는 고객이 시스템을 통해 온라인 결제하지 않는다.
운영자가 입금/결제를 확인해 상태를 기록한다.

---

## 9. Deposit Refund / Forfeit

Refund 시 PaymentRefund와 Deposit REFUNDED를 함께 처리한다.
No-show 등 정책상 몰수 시 Deposit FORFEITED로 변경하고 reason을 기록한다.

---

## 10. Treatment Final Payment

Deposit이 있으면 UI에서 다음을 표시할 수 있다.

```text
service total
deposit paid
remaining payment
```

자동 결제는 하지 않는다.

---

## 11. Transaction Rules

Renewal:

```text
Payment PAID + EnrollmentCycle create
```

Refund:

```text
PaymentRefund create + Payment status update + Deposit update if applicable
```

---

## 12. Idempotency

동일 Payment에 successful full refund는 최대 1개다.
DB constraint와 transaction으로 보장한다.

---

## 13. Audit

Payment/Refund 기록은 삭제하지 않는다.
필요한 수정은 상태 전환 또는 adjustment로 남긴다.

---

## 14. Lesson Cycle Refund Policy

Lesson payment full refund는 payment만 되돌리는 작업이 아니다. 연결된 EnrollmentCycle entitlement도 함께 처리해야 한다.

MVP에서 lesson cycle full refund는 다음 조건을 모두 만족할 때만 허용한다.

```text
cycle status = ACTIVE or SCHEDULED
no consumed negative usage ledger effect
no active PassEntitlementReservation
no PENDING/CONFIRMED Booking tied to the cycle
```

성공 transaction:

```text
PaymentRefund create
+ Payment REFUNDED
+ EnrollmentCycle CANCELLED
+ lifecycle rule에 따라 eligible SCHEDULED successor activation
```

다음 경우 자동 full refund를 거부한다.

```text
used entitlement exists
completed lesson/attendance usage exists
outstanding booking exists
active entitlement reservation exists
cycle already COMPLETED or EXPIRED
```

MVP에서는 사용분 차감 후 부분 환불, 비례 환불, 과거 completed cycle 환불을 자동 계산하지 않는다. 해당 기능은 P1+ 별도 정책/ADR 대상이다.

