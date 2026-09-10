# API_CONVENTIONS.md

## 1. Base

```text
/api/v1
```

Examples:
```text
/api/v1/customers
/api/v1/bookings
/api/v1/payments
/api/v1/lesson/classes
/api/v1/lesson/passes
/api/v1/lesson/attendance
/api/v1/beauty/services
/api/v1/beauty/treatments
/api/v1/beauty/revisits
```

## 2. Principles

- REST resource-oriented naming
- nouns, not action-heavy paths where avoidable
- validation errors consistent
- operator tenant scope is validated from StudioMembership / AuthorizedStudioContext
- public customer scope comes from PortalSession
- path/body studioId never establishes authorization by itself

## 3. Response

성공 응답은 resource 또는 explicit result DTO를 사용한다.
Entity를 그대로 JSON에 노출하지 않는다.

## 4. Error shape

```json
{
  "code": "BOOKING_CONFLICT",
  "message": "이미 예약된 시간입니다.",
  "fieldErrors": [],
  "traceId": "..."
}
```

## 5. Pagination

Query:
```text
page
size
sort
```

Response:
```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

## 6. Idempotency

`Idempotency-Key` is mandatory for MVP command endpoints where duplicate side effects are unsafe.

Required examples:
- public booking create
- lesson completion
- treatment completion
- payment refund
- enrollment renewal

Namespace:
```text
studioId + actorType + actorId + operation + idempotencyKey
```

Replay rules:
- same namespace/key + same request hash → return the previous result
- same namespace/key + different request hash → `409 Conflict`
- different studio or actor → independent namespace

Replay handling must revalidate the current actor and Studio authorization before returning a stored result.

## 7. Versioning

초기 API는 `/api/v1`.
Breaking change가 필요할 때만 새 version을 만든다.

## 8. Phase 3 configuration API

Under `/api/v1/studios/{studioId}`:
- GET `/onboarding` or `/configuration`: authorized configuration, category-scoped catalog, permissions and version.
- POST `/onboarding/complete`: OWNER-only, complete validated configuration, atomic activation; repeat returns 409.
- PUT `/configuration`: full validated replacement values (rows updated in place), version required; stale version returns 409.
- GET `/configuration/lesson-policy` or `/configuration/beauty-policy`: membership plus ACTIVE/category guard; opposite category returns 403.

Commands include version, businessCategory, businessType, all current-category capability flags,
seven businessHours rows, bookingPolicy, and only the matching category policy.
The opposite category policy must be null/absent. Capability dependencies and field-level role
restrictions follow ADR-051. All mutations retain server-session CSRF protection.

## 9. Phases 4–6 APIs

All paths below are under `/api/v1/studios/{studioId}` and require an ACTIVE Studio.

- Customers: GET/POST `/customers`, GET/PUT `/customers/{id}`, POST `/customers/{id}/archive`.
  List: search (name or formatted/normalized phone), status ACTIVE/ARCHIVED/ALL, page, size (1–100).
- Payments: GET/POST `/payments`, GET `/payments/{id}`, POST `/payments/{id}/confirm`, `/cancel`, `/refund`.
  List: customerId, status, page, size. Money is a decimal JSON string, validated/stored as positive long KRW.
- Bookings: GET/POST `/bookings`, GET/PUT `/bookings/{id}`, POST `/bookings/{id}/confirm`, `/cancel`, `/complete`, `/no-show`.
  List: from/to ISO timestamps, status, page, size. PUT replaces only staff/time/note.
- Blocks: GET/POST `/booking-blocks`, DELETE `/booking-blocks/{id}`. List: from/to, page, size.
- GET `/booking-staff` returns active staff IDs/names for operator assignment, without introducing Staff management.
- GET `/availability?staffId=...&startAt=...&endAt=...` returns advisory availability or validation errors.

Payment, booking and block mutations require Idempotency-Key (1–128 ASCII letters/digits or `._:-`).
Keys are scoped by Studio/operator/operation; request hashes include the target resource ID for resource commands.
Same successful request replays; different payload under the same key returns 409. Failed transactions retain
neither business changes nor a successful replay record. No key expiry/reuse is currently enabled.
Role restrictions and manual-scope exceptions are defined by ADR-052 and SECURITY.md.
