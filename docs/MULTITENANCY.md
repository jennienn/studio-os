# MULTITENANCY.md

# Multi-Tenancy Rules

## 1. Tenant

Tenant는 `Studio`이다.
한 Studio의 데이터는 다른 Studio에서 접근할 수 없다.

---

## 2. Explicit Tenant Ownership

모든 tenant-owned table은 명시적인 `studio_id`를 가진다.

```text
Customer
Staff
Booking
BookingBlock
Payment
Notification
StudioCapability
BusinessHours
BookingPolicy
LessonPolicy
BeautyPolicy
PassProduct
Enrollment
EnrollmentCycle
PassUsageLedger
Class
ClassSchedule
ClassOccurrence
Attendance
BeautyService
TreatmentRecord
Deposit
RevisitRule
```

간접 FK만으로 Studio를 찾을 수 있다는 이유로 `studio_id`를 생략하지 않는다.

---

## 3. Global Identity Tables

다음은 tenant-owned가 아니다.

```text
User
AuthAccount
```

Studio와 User의 관계는 `StudioMembership`으로 관리한다.

---

## 4. Authorization Flow

```text
Authentication
↓
User
↓
StudioMembership
↓
AuthorizedStudioContext
↓
Domain Service
↓
Repository
```

Repository 호출 전에 authorized studio가 결정되어 있어야 한다.

---

## 5. Never Trust Client Studio ID

request의 studioId는 authorization proof가 아니다.

```text
requested studio
↓
StudioMembership validation
↓
authorizedStudioId
↓
repository operation
```

---

## 6. Resource Ownership Validation

다른 entity를 참조하는 command에서는 모든 referenced resource가 같은 Studio인지 검증한다.

```text
customer.studio_id == authorizedStudioId
staff.studio_id == authorizedStudioId
service.studio_id == authorizedStudioId
```

하나라도 다르면 request를 거부한다.

---

## 7. Repository Rule

Tenant-owned query에는 반드시 Studio 조건이 포함된다.

```sql
SELECT *
FROM customers
WHERE studio_id = :studioId
AND id = :customerId;
```

`WHERE id = :customerId`만 사용하는 tenant-owned 조회는 금지한다.

---

## 8. Unique Constraints

Tenant-local uniqueness는 Studio와 함께 정의한다.

Examples:

```text
(studio_id, normalized_phone, name)
(studio_id, class_name)
```

### Studio Slug Exception

`Studio.slug`는 public tenant identifier이므로 globally unique다.

```text
UNIQUE(slug)
```

`(studio_id, slug)` unique constraint를 사용하지 않는다.

---

## 9. Cache Keys

Redis cache key는 tenant scope를 포함한다.

```text
studio:{studioId}:config
studio:{studioId}:booking-policy
```

---

## 10. R2 Objects

R2 object key에도 tenant boundary를 포함한다.

```text
studios/{studioId}/treatments/{treatmentId}/{uuid}.jpg
```

Object metadata row도 `studio_id`를 가진다.

---

## 11. Customer Portal Tenant Context

Customer portal은 operator membership authorization을 사용하지 않는다.

```text
/book/{studioSlug}
↓
globally unique Studio
↓
Customer identification
↓
PortalSession(studioId, customerId)
```

PortalSession의 studioId/customerId가 public request scope를 결정한다.
client가 별도 studioId/customerId를 보내더라도 session scope와 다르면 거부한다.

---

## 12. EnrollmentCycle

EnrollmentCycle은 반드시 explicit `studio_id`를 가진다.

---

## 13. Idempotency Tenant Boundary

Idempotency namespace:

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

다른 Studio의 동일 key가 서로 영향을 주면 안 된다.
Replay request에서도 현재 Studio/actor authorization을 다시 검증한다.

---

## 14. Cross-Tenant Tests

모든 tenant-owned API는 최소 다음 테스트를 가진다.

```text
Studio A owner → Studio A resource → allowed
Studio A owner → Studio B resource → denied
```

read/update/delete/list/bulk/booking/payment/refund/attendance/treatment/pass adjustment에 적용한다.

---

## 15. PostgreSQL RLS

PostgreSQL Row Level Security는 MVP에서 도입하지 않는다.
MVP는 application authorization + explicit tenant predicates를 사용한다.
향후 필요 시 별도 ADR로 검토한다.
