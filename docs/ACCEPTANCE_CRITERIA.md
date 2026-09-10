# ACCEPTANCE_CRITERIA.md

# MVP Acceptance Criteria

## 1. Category Isolation
LESSON must never render BEAUTY subtype/features. BEAUTY must never render LESSON subtype/features. Invalid direct API combinations return 4xx.

## 2. Category Persistence
After onboarding, ordinary settings cannot change LESSON ↔ BEAUTY. Backend rejects it.

## 3. Authentication
MVP must support Email + Password, Google OAuth, Kakao OAuth, logout, email verification, password reset. No browser localStorage access token. Redis server session is used.

## 4. Tenant Isolation
Studio A operator cannot read/update/delete Studio B Customer/Booking/Payment/Enrollment/Treatment even with known IDs.

## 5. Customer Terminology
LESSON UI uses 회원, BEAUTY UI uses 고객, persistence uses Customer.

## 6. Arbitrary Pass
7/10/15-session products can be created. No 4/8/12 restriction.

## 7. Pass Ledger
10 purchase → +10. One valid deduction → -1. Retry of same business event does not deduct twice.

## 8. Deduction Compatibility
PRIVATE cannot use ATTENDANCE_PRESENT. GROUP cannot use LESSON_COMPLETED in MVP. Backend rejects invalid combinations.

## 9. FIRST_USE
COUNT_BASED + FIRST_USE may have null dates before first configured deduction. First deduction transaction sets dates and ledger atomically.

## 10. Renewal Lifecycle
Existing ACTIVE cycle remains until terminal. Renewal with active cycle creates one SCHEDULED cycle. When active becomes terminal, scheduled becomes active. No more than one ACTIVE and one SCHEDULED per Enrollment.

## 11. Cancellation Restore
Eligible cancellation creates exactly one CANCEL_RESTORE. Retries do not add more credits.

## 12. Group Capacity
Capacity 10 permits 10 occupying bookings and rejects the 11th. Same occurrence supports multiple bookings.

## 13. Private Conflict
Same staff overlapping 1:1 booking conflicts. Different staff may be available.

## 14. Booking Block
Staff block affects that staff only. Studio block affects all 1:1 bookings. Existing confirmed booking conflict is not silently overwritten.

## 15. LESSON Customer Portal
Registered customer with eligible active cycle can book. Unknown customer cannot be auto-created.

## 16. BEAUTY Customer Portal
New customer can be created and book an active BeautyService.

## 17. Portal Privacy
Portal must not expose operator memo, private treatment notes/photos, full payment history, other customer data.

## 18. Portal Session
Identification creates a 30-minute Redis-backed portal session. Expired session cannot book.

## 19. Studio Slug
Studio.slug is globally unique and `/book/{studioSlug}` resolves exactly one Studio.

## 20. Idempotency
Same studio+actor+operation+key+requestHash replays prior result. Same namespace/key with changed body returns 409. Different studio/actor is independent.

## 21. Beauty Completion
Booking completion creates exactly one TreatmentRecord. Repeat completion cannot create a second one.

## 22. Beauty Snapshot
After treatment completion, later BeautyService price/duration changes do not alter historical TreatmentRecord snapshot.

## 23. Revisit
revisitDays is calculated at treatment completion and persisted. Later service rule edits do not rewrite old due dates.

## 24. Deposit
Operator can transition REQUIRED → PAID and appropriate REFUNDED/FORFEITED states with audit reason where required.

## 25. Payment Refund
Paid payment is not deleted. Full refund creates PaymentRefund and marks Payment REFUNDED. A second full refund is rejected.

## 26. Dashboard
LESSON and BEAUTY dashboards show category-appropriate backend/PostgreSQL-derived metrics.

## 27. Configuration Disable
Disabling a capability preserves historical data. Invalid cross-category capability enable is rejected.

## 28. Booking Correctness
Committed booking correctness is guaranteed by PostgreSQL transaction/recheck/constraints/locking. Redis lock is not required for correctness.

## 29. Build & Test Gate
Backend:
```text
./gradlew test
```

Frontend:
```text
npm run typecheck
npm run test
npm run build
```

Critical Playwright flows:

LESSON:
```text
signup → onboarding → customer → pass → enrollment → booking → completion → deduction → renewal
```

BEAUTY:
```text
signup → onboarding → customer → service → booking → treatment completion → revisit
```

## 30. Deferred Entitlement Reservation

Given one remaining COUNT_BASED credit with deferred trigger:

```text
Booking A confirmed → one ACTIVE reservation
Booking B confirmation attempt → rejected for insufficient available entitlement
```

Completing/attending Booking A consumes the reservation and writes exactly one ledger -1. Cancelling before deduction releases the reservation without writing a restore ledger entry.

## 31. Cycle Completion With Outstanding Booking

A cycle with zero ledger balance but an unresolved booking/reservation does not become COMPLETED and does not activate its SCHEDULED successor yet. After the unresolved booking settles, terminal evaluation runs and successor activation may occur.

## 32. Cancellation Restore Before Successor Activation

For `BOOKING_CONFIRMED` deduction, timely eligible cancellation restores exactly one credit to the still-ACTIVE original cycle. Normal lifecycle rules must prevent successor activation before that booking resolves. Terminal cycles are never automatically reopened.

## 33. Lesson Refund Integrity

Full refund of an ACTIVE/SCHEDULED lesson cycle succeeds only when there is no consumed entitlement and no outstanding booking/reservation. Successful refund creates PaymentRefund, marks Payment REFUNDED, and marks the cycle CANCELLED in one transaction. Refund with used entitlement or unresolved booking is rejected.

## 34. Security Documentation Alignment

Operator APIs require membership/role authorization. Customer portal APIs use PortalSession scope instead of StudioMembership and cannot access operator-only fields.



## 35. COUNT_BASED Expiration With Unused Credits

Given a COUNT_BASED cycle with:

```text
ledger balance = 3
valid_end_date = 2026-09-30
no unresolved booking/reservation
```

After the Studio-local validity boundary:

```text
cycle -> EXPIRED
new booking using the cycle -> rejected
ledger balance remains historically 3
```

No synthetic ledger deduction is created merely to force the balance to zero.

A lesson scheduled after `valid_end_date` cannot be booked using that cycle even if the booking request is made before expiration.

---

## 36. COUNT_BASED Expiration With Outstanding Booking

Given validity has ended but a booking/reservation from the cycle is unresolved:

- the cycle cannot accept any new booking;
- it does not activate its successor yet;
- it resolves the existing booking first;
- after the last unresolved dependency settles, the cycle becomes EXPIRED and the SCHEDULED successor may activate.

---

## 37. Deferred PRIVATE No-Show

Given PRIVATE + `LESSON_COMPLETED` with an ACTIVE entitlement reservation:

```text
Booking -> NO_SHOW
```

Then:

```text
reservation -> RELEASED
no PassUsageLedger deduction
cycle terminal/expiration evaluation runs
```

---

## 38. Deferred GROUP Absence

Given GROUP + `ATTENDANCE_PRESENT` with an ACTIVE entitlement reservation:

```text
Attendance -> ABSENT
```

Then:

```text
related Booking -> NO_SHOW
reservation -> RELEASED
no PassUsageLedger deduction
cycle terminal/expiration evaluation runs
```

---

## 39. BOOKING_CONFIRMED No-Show

Given a `BOOKING_CONFIRMED` product already deducted at confirmation:

```text
Booking -> NO_SHOW
```

Then no automatic restore is created. No-show is not cancellation and does not generate `CANCEL_RESTORE`.
