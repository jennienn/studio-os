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
