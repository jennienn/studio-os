# SECURITY.md

# Security Rules

## 1. Core Threats

- cross-tenant data access
- broken object-level authorization
- account takeover
- booking abuse/spam
- OAuth CSRF/state attacks
- leaked customer PII
- insecure file upload
- duplicate booking / duplicate deduction race
- customer portal impersonation/enumeration

---

## 2. Authorization Contexts

### Operator APIs

Operator object access requires:

```text
Authenticated User
→ StudioMembership validation
→ role validation
→ AuthorizedStudioContext
→ resource ownership validation
```

Knowing a resource ID or sending `studioId` is never sufficient authorization.

### Customer Portal APIs

Customer portal is an explicit exception to operator membership authorization. It uses:

```text
/book/{studioSlug}
→ Customer identification
→ short-lived PortalSession
→ studioId + customerId scoped authorization
```

PortalSession must never grant operator permissions.

---

## 3. MVP Permission Matrix

### OWNER
- full studio operations
- business configuration
- payment refund
- manual pass adjustment
- membership/role management

### MANAGER
- customer management
- booking management
- payment recording
- attendance/treatment
- renewal
- normal operational settings
- no ownership transfer

### STAFF
- own/assigned schedule
- attendance or treatment completion required for assigned work
- limited operational customer information
- no refund
- no manual pass adjustment
- no tenant/security configuration

Granular staff permission customization is P1.

### Phase 3 configuration permissions (ADR-051)

All active members may read their Studio configuration. Initial onboarding is OWNER-only.
Only OWNER may change businessType within the existing category, enable/disable capabilities,
or change DEPOSIT / BeautyPolicy.depositEnabled (these two values must agree).
No role may change an ACTIVE Studio's businessCategory through ordinary settings.
MANAGER may update BusinessHours, BookingPolicy, LessonPolicy.lowBalanceThreshold,
LessonPolicy.expiryAlertDays, LessonPolicy.restoreOnTimelyCancellation and
BeautyPolicy.noShowEnabled. STAFF has no Phase 3 configuration write permission.
The backend compares structural fields with persisted values; hidden controls alone are not authorization.

### Phases 4–6 operational permissions (ADR-052)

OWNER/MANAGER may manage customers, record/read/confirm/cancel unpaid payments, and manage bookings/blocks.
Full refund is OWNER-only. STAFF can only read their assigned bookings and the associated customer's
name/phone. STAFF cannot access customer memo, payment records, full customer lists or mutation commands
in these phases. All operational APIs require an ACTIVE Studio and active membership.

---

## 4. Authentication

MVP operator authentication is fixed as:

```text
Email + Password
Google OAuth
Kakao OAuth
```

Session strategy:

```text
Spring Security
Spring Session
Redis
HttpOnly Cookie
```

Production cookie requirements:

```text
HttpOnly=true
Secure=true
SameSite=Lax
```

Password hashing:

```text
BCrypt
```

Password reset and email verification are MVP.

OAuth requires state validation. Automatic account merge based only on matching provider email is forbidden.

---

## 5. CSRF

Cookie-authenticated state-changing requests require CSRF protection. Operator and customer portal sessions follow the same principle.

---

## 6. PII and Logging

Sensitive data includes:

- customer phone
- OAuth access/refresh tokens
- auth cookies/session IDs
- password hashes/passwords
- verification codes
- private treatment notes/photos

Do not expose these in logs or generic error responses. Phone values should be masked when logging is necessary.

---

## 7. Public Booking Protection

Required for MVP:

- rate limiting
- input validation
- generic customer-identification errors
- minimum data response
- 30-minute Redis portal session
- mandatory `Idempotency-Key` for booking create
- replay authorization check
- no operator memo/private treatment/payment history exposure

SMS OTP is P1. Name + phone is allowed only for controlled pilot scope.

---

## 8. File Upload

Treatment photos are P1. When implemented with R2:

- allowed content types
- size limits
- random object keys
- explicit studio association
- authorized reads
- no unrestricted public bucket exposure

---

## 9. Database Integrity

- PostgreSQL is the final source of truth
- parameterized queries/JPA/QueryDSL
- Flyway-controlled migrations
- least-privilege DB credentials
- booking correctness uses transaction/recheck/constraints/locking
- Redis lock is not a correctness requirement
- duplicate ledger/treatment/refund effects require DB uniqueness/idempotency
- all tenant-owned tables use explicit `studio_id`

---

## 10. Rate Limiting

MVP rate limiting targets:

- email login
- password reset request
- customer identification
- public booking creation

OAuth abuse protection follows provider/state validation and endpoint-level limits where appropriate. OTP send/verify limits are added in P1 with OTP.
