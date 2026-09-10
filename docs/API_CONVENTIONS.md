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
