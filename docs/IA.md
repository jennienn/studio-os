# IA.md

## 1. Public routes

```text
/
/login
/signup
/onboarding
/book/{studioSlug}
```

## 2. Operator routes — common

```text
/app
/app/customers
/app/customers/{id}
/app/bookings
/app/payments
/app/settings
```

## 3. LESSON default navigation

```text
홈
수업 일정
회원
이용권
결제 / 재등록
설정
```

Conditional:
- `bookingEnabled` → 예약
- `groupClassEnabled` → 그룹 수업
- `attendanceEnabled` → 출석

Example group studio:
```text
홈
수업 일정
예약
그룹 수업
출석
회원
이용권
결제 / 재등록
설정
```

## 4. BEAUTY default navigation

```text
홈
예약 일정
고객
시술 이력
결제
재방문
설정
```

Conditional:
- `depositEnabled` → 예약금 states shown
- `photoHistoryEnabled` → treatment photos (P1 only; MVP에서는 capability/UI 미노출)
- `revisitEnabled` → revisit navigation

## 5. Prohibited cross-mode UI

LESSON UI에서 기본 노출 금지:
- 시술
- 네일
- 속눈썹
- 예약금(향후 공통화 전)
- 시술 사진

BEAUTY UI에서 기본 노출 금지:
- 출석
- 반
- 회차권
- 수강
- 재등록

## 6. Public landing product preview

Preview segmented switch:
- 레슨 / 스튜디오
- 뷰티 / 예약샵

이 switch는 랜딩 데모용이며 실제 로그인 Studio category를 변경하지 않는다.

## 7. Phase 3 routes

/onboarding은 인증된 운영자의 현재 PRE_ONBOARDING Studio를 설정하는 화면이다.
ACTIVE Studio는 /app으로 이동한다. PRE_ONBOARDING의 /app 및 하위 경로는 /onboarding으로 이동한다.
Phase 3 시점의 ACTIVE 메뉴는 홈(/app)과 설정(/app/settings)이었다. Phase 4–6 메뉴는 §8을 따르고,
현재 Phase 7–8 LESSON 메뉴는 §9를 따른다.
상대 category의 /app/lesson/* 또는 /app/beauty/* 직접 접근은 /app으로 이동한다.
아직 구현되지 않은 다른 /app 하위 업무 경로도 /app으로 이동하며 가짜 업무 화면을 제공하지 않는다.

## 8. Phases 4–6 routes

OWNER/MANAGER: 홈, 회원(LESSON)/고객(BEAUTY), 결제, 예약, 설정.
STAFF: 홈, 배정된 예약, 조회용 설정. 고객 전체 목록과 결제 화면은 접근을 거부한다.
/app/customers, /app/customers/new, /app/customers/{id}는 공통 Customer 화면이다.
/app/payments, /app/payments/new, /app/payments/{id}는 수동 결제 및 이력 화면이다.
/app/bookings는 Studio 시간대 기준 날짜 선택/주간 탐색/당일 목록, 수동 예약과 차단 화면이다.
Phase 4–6 범위에서는 전문 이용권·출석·시술·예약금 메뉴와 대시보드 지표를 추가하지 않았다.

## 9. Phase 7–8 LESSON routes

OWNER/MANAGER LESSON navigation adds:

```text
/app/lesson/pass-products
/app/lesson/enrollments
/app/lesson/classes        (GROUP_CLASS enabled)
/app/lesson/attendance     (GROUP_CLASS and ATTENDANCE enabled)
```

`/app/bookings` retains manual 1:1 records and adds pass-backed private lesson creation when
`PRIVATE_LESSON` is enabled. `/app/customers/{id}` shows the customer's enrollment and cycle
history. STAFF does not receive product/enrollment/class administration routes; assigned lesson
outcomes remain available only through the permitted operation screen. BEAUTY navigation does not
expose any of these LESSON routes.
