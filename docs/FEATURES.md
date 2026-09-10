# FEATURES.md

# Feature Scope

```text
MVP = 초기 실증에 반드시 필요
P1  = MVP 이후 우선 개발
P2  = 검증 후 확장
```

## 1. Public — MVP
- Public landing page
- LESSON / BEAUTY product preview
- Login / Signup
- Email + Password login
- Google OAuth
- Kakao OAuth
- Password reset
- Customer booking portal `/book/{studioSlug}`

## 2. Operator Authentication — MVP
- Email signup/login
- Google OAuth
- Kakao OAuth
- Logout
- Redis-backed server session
- Session expiration
- Email verification
- Password reset
- OWNER role
- StudioMembership validation

P1:
- OAuth account linking UI
- operator invitation UI
- detailed role management UI

## 3. Studio — MVP
- Studio creation
- globally unique Studio slug
- business category/subtype
- timezone
- onboarding
- business hours
- feature capabilities
- booking policy
- category-specific policy
- default OWNER Staff profile

Category cannot be changed through ordinary settings after onboarding.

## 4. Customer — MVP
- create/edit/detail/archive/search
- phone normalization
- operator memo
- pagination

LESSON UI = 회원
BEAUTY UI = 고객
Persistence = Customer

## 5. Common Booking — MVP
Operator:
- booking calendar
- daily list
- create/edit/cancel/complete
- booking block create/remove
- availability query
- server-side conflict validation
- idempotent booking command

Customer:
- studio booking page
- name + phone identification
- eligible item query
- slot query
- direct booking
- booking confirmation

P1:
- future booking list
- customer cancellation
- SMS OTP
- customer reschedule

## 6. Booking Block — MVP
Scopes:
```text
STUDIO
STAFF
```

## 7. LESSON — Pass Products — MVP
- arbitrary session count
- COUNT_BASED
- TIME_BASED
- single-use pass
- monthly-style time pass
- price
- validity
- validity start rule
- deduction trigger
- active/inactive

No hardcoded 4/8/12.

## 8. LESSON — Enrollment — MVP
- private/group enrollment
- multiple enrollments per customer
- end enrollment
- preserved history
- EnrollmentCycle
- SCHEDULED renewal cycle
- ACTIVE cycle
- cycle snapshots

## 9. LESSON — Ledger — MVP
```text
PURCHASE
BOOKING_DEDUCTION
ATTENDANCE
LESSON_COMPLETED
CANCEL_RESTORE
MANUAL_ADJUSTMENT
```

- authoritative balance calculation
- duplicate prevention
- manual adjustment reason
- transaction safety
- ledger history

## 10. LESSON — Private Booking — MVP
```text
name + phone
→ existing Customer
→ active Enrollment/Cycle
→ eligible lesson
→ available time
→ booking
```

- operator direct booking
- completion
- configured deduction trigger
- cancellation restore policy
- unknown customer auto-create forbidden

## 11. LESSON — Group Class — MVP
- Class create/edit
- capacity
- instructor/default staff
- recurring ClassSchedule
- ClassOccurrence generation
- group enrollment
- group booking
- occurrence capacity validation

## 12. LESSON — Attendance — MVP
- class/occurrence selection
- calendar
- attendance list
- individual/bulk attendance
- remaining pass display
- payment-needed indicator
- attendance history
- trigger-dependent deduction
- duplicate deduction prevention

## 13. LESSON — Renewal — MVP
- payment-confirmed renewal
- preserve previous cycle
- create new ACTIVE or SCHEDULED cycle
- SCHEDULED activation after previous cycle terminal
- no automatic carry-over
- manual adjustment with reason
- renewal history

## 14. BEAUTY — Service Catalog — MVP
- create/edit
- active/inactive
- price
- duration
- revisitDays
- one service per booking

P1:
- multi-service booking
- options/add-ons

## 15. BEAUTY — Booking — MVP
Customer:
```text
name + phone
→ existing Customer OR create Customer
→ service
→ available time
→ booking
```

Operator:
- create/edit/cancel/complete
- no-show

## 16. BEAUTY — Treatment — MVP
- TreatmentRecord on completion
- service/price/duration snapshot
- treatment date
- customer history
- internal treatment memo

P1:
- treatment photos
- R2 upload

## 17. BEAUTY — Revisit — MVP
- revisitDays
- due calculation
- upcoming list
- overdue list
- dashboard alert

P1:
- manual override
- customer-specific interval

## 18. BEAUTY — Deposit — MVP
```text
REQUIRED
PAID
REFUNDED
FORFEITED
```

- Booking connection
- Payment connection
- operator manual confirmation
- no online payment in MVP

## 19. Payment — MVP
- manual payment recording
- KRW integer amount
- CARD/CASH/TRANSFER/OTHER
- history
- full refund
- erroneous-record cancellation
- lesson/renewal/beauty/deposit association

P1: partial refund
P2: actual PG

## 20. Notifications — MVP
- Notification entity
- intent
- scheduledAt
- sent/pending/failed/skipped
- low pass / expiry / renewal / booking reminder / revisit / deposit alerts
- dashboard notification state

P1:
- Kakao 알림톡
- SMS
- Email

## 21. Staff
MVP:
- Staff persistence
- default OWNER Staff auto creation
- booking assignment
- class instructor relation

P1:
- invitation
- multi-staff UI
- working hours
- staff calendar
- granular permissions

## 22. Dashboard — MVP
LESSON:
- today lessons/bookings
- payment required
- low remaining count
- renewal due
- unfinished attendance

BEAUTY:
- today bookings
- expected revenue
- unpaid deposit
- revisit due
- new customers
- cancellation/no-show

Metrics come from backend/PostgreSQL.

## 23. Security — MVP
- tenant isolation
- StudioMembership authorization
- category validation
- CSRF
- secure cookies
- public rate limiting
- cross-tenant tests
- idempotency
- sensitive log filtering

## 24. P1 Summary
- SMS OTP
- Kakao 알림톡
- SMS/Email delivery
- staff management UI
- treatment photos
- multi-service beauty booking
- customer own-booking list/cancel
- partial refunds
- advanced audit UI

## 25. P2 Summary
- actual PG
- advanced analytics
- automated marketing
- broader industry modules
- category migration workflow
- accounting integrations
