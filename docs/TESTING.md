# TESTING.md

## 1. Rule

핵심 business rule은 UI 수동 테스트만으로 끝내지 않는다.

## 2. Backend unit tests

도메인 규칙:
- pass deduction
- cancellation restore
- renewal cycle creation
- validity check
- booking eligibility
- revisit date calculation
- deposit state transitions
- role authorization

Tools:
- JUnit 5
- AssertJ
- Mockito where appropriate

## 3. Backend integration tests

Tools:
- Spring Boot Test
- Testcontainers PostgreSQL
- Redis container when needed

필수:
- Flyway migration boot
- repository query
- transaction rollback behavior
- tenant isolation
- booking race/conflict

## 4. Frontend tests

- category subtype filtering
- LESSON에 BEAUTY subtype이 나타나지 않음
- BEAUTY에 LESSON subtype이 나타나지 않음
- capability-based navigation
- forms/validation

Tools:
- Vitest
- React Testing Library

## 5. E2E critical flows

### LESSON
```text
signup
→ onboarding LESSON
→ customer create
→ pass create
→ enrollment
→ customer booking
→ lesson complete
→ pass deduction
→ renewal
```

### BEAUTY
```text
signup
→ onboarding BEAUTY
→ customer create
→ service create
→ booking
→ treatment complete
→ revisit due
```

## 6. Regression from Élanor

LESSON MVP에서 확인:
- 개인 예약
- 예약 시간 차단
- 수업 완료
- 회차 차감/취소 복구
- 단체 출석
- 재등록 cycle history
- 결제 필요 상태
