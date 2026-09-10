# AUTH.md

# Authentication & Authorization

## 1. Identity Types

서비스에는 두 종류의 identity가 존재한다.

```text
Operator User
Customer
```

둘은 같은 account model로 합치지 않는다.

---

## 2. Operator Authentication

MVP authentication methods:

```text
Email + Password
Google OAuth
Kakao OAuth
```

Backend:

```text
Spring Security
Spring Security OAuth2 Client
Spring Session
Redis
```

---

## 3. Session Strategy

JWT access token 기반 SPA authentication은 사용하지 않는다.
MVP는 server-side session을 사용한다.

```text
Browser
↓
HttpOnly session cookie
↓
Spring Security
↓
Spring Session
↓
Redis
```

Cookie:

```text
HttpOnly=true
Secure=true in production
SameSite=Lax
```

Session inactivity timeout은 12시간이다.
로그인 성공 시 session ID를 rotate한다.
로그아웃 시 Redis session을 invalidate한다.
Remember-me는 MVP에서 지원하지 않는다.

---

## 4. CSRF

Operator browser session은 cookie authentication을 사용하므로 state-changing endpoint에 CSRF protection을 적용한다.
Frontend는 backend가 발급한 CSRF token을 요청 header에 포함한다.
Public customer portal도 session cookie를 사용할 경우 동일한 보호 원칙을 적용한다.

---

## 5. Email Authentication

Email authentication:

```text
email
password
```

Password는 BCrypt로 hash한다.

MVP requirements:
- password validation
- email verification
- password reset token
- reset token expiration
- password reset 후 기존 session invalidate 가능

Password는 로그에 기록하지 않는다.

---

## 6. OAuth

Providers:

```text
GOOGLE
KAKAO
```

Model:

```text
User
AuthAccount
```

AuthAccount:

```text
id
userId
provider
providerUserId
createdAt
```

Unique:

```text
(provider, providerUserId)
```

Provider email만 이용한 자동 account merge는 금지한다.
기존 account와 연결하려면 이미 로그인된 사용자가 명시적인 linking action을 수행해야 한다.

---

## 7. Kakao OAuth

Kakao OAuth는 MVP다.
Provider-specific user mapping은 adapter 내부에서 처리한다.
Business domain이 Kakao SDK object에 직접 의존하지 않는다.

Kakao에서 email이 제공되지 않을 수 있으므로:
- providerUserId를 primary external identity로 사용한다.
- OAuth-only User의 email은 nullable할 수 있다.
- email이 필요한 기능이 생기면 별도 profile completion을 요청한다.

---

## 8. Studio Authorization

Operator request는 다음 순서로 처리한다.

```text
Authenticated User
↓
requested studio
↓
StudioMembership lookup
↓
role validation
↓
AuthorizedStudioContext
↓
business operation
```

request path/body의 `studioId`만 신뢰하지 않는다.

---

## 9. Active Studio

User가 하나의 Studio membership만 가지고 있으면 자동으로 active studio로 선택한다.
여러 membership이 있으면 session에 `activeStudioId`를 저장할 수 있다.
activeStudioId 변경 시 membership 검증을 다시 수행한다.

---

## 10. Roles

```text
OWNER
MANAGER
STAFF
```

OWNER:
- full studio access
- configuration
- payment refund
- manual pass adjustment
- role management

MANAGER:
- customer
- booking
- attendance/treatment
- payment
- renewal
- normal operational settings

STAFF:
- assigned schedule
- attendance/treatment completion
- limited customer operational data

---

## 11. Customer Booking Portal

Route:

```text
/book/{studioSlug}
```

Customer는 User login을 하지 않는다.
MVP identification:

```text
name
phone
```

Phone normalization:
- remove spaces
- remove hyphen
- normalize Korean phone format

---

## 12. LESSON Customer Identification

```text
name + phone
↓
find Customer within Studio
↓
exactly one match required
```

one match: portal session 생성
zero matches: 예약 불가, 사업장 문의 안내
multiple matches: 자동 선택 금지, 사업장 문의 안내

---

## 13. BEAUTY Customer Identification

```text
name + phone
↓
existing Customer?
```

existing이면 기존 Customer 사용.
없으면 신규 Customer 생성 가능.
동일 normalized phone과 name 기준으로 중복을 방지한다.

---

## 14. Customer Portal Session

Identification 성공 후 short-lived portal session을 발급한다.

```text
Storage: Redis
Cookie: HttpOnly, Secure in production, SameSite=Lax
TTL: 30 minutes
```

Portal session stores:

```text
studioId
customerId
category
issuedAt
expiresAt
```

---

## 15. Customer Portal Permissions

MVP 허용:
- eligible offering 조회
- availability 조회
- booking 생성

MVP 금지:
- operator memo
- treatment internal notes
- treatment photos
- full payment history
- internal notification data
- another customer's data
- Studio admin APIs

Own-booking list/cancel은 P1.

---

## 16. Public Endpoint Protection

Required:
- IP rate limit
- studio/customer scoped rate limit where possible
- generic error response
- no customer existence enumeration details
- Idempotency-Key for booking creation
- minimum response data

---

## 17. OTP

SMS OTP는 P1이다.
OTP 도입 전 name + phone 방식은 controlled pilot 범위에만 사용한다.

---

## 18. Security Logging

로그 기록 금지:

```text
password
OAuth access token
OAuth refresh token
session id
verification code
full treatment memo
```

Phone number는 필요 시 masking한다.
