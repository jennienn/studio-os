# Agent Instructions

이 파일은 Codex/AI agent가 코드 변경 전에 반드시 따라야 하는 최상위 규칙이다.

## Required reading

작업 시작 전 현재 작업과 관련된 문서를 `/docs`에서 읽는다.

- 제품: `docs/PRODUCT.md`
- 기술: `docs/TECH_STACK.md`
- 도메인: `docs/DOMAIN_RULES.md`
- 용어: `docs/GLOSSARY.md`
- 기능: `docs/FEATURES.md`
- 온보딩: `docs/ONBOARDING.md`
- IA: `docs/IA.md`
- 데이터: `docs/DATA_MODEL.md`
- 멀티테넌시: `docs/MULTITENANCY.md`
- 인증: `docs/AUTH.md`
- 예약: `docs/BOOKING.md`
- 결제: `docs/PAYMENTS.md`
- 알림: `docs/NOTIFICATIONS.md`
- UI: `docs/UI_GUIDELINES.md`
- 테스트: `docs/TESTING.md`

## Hard rules

1. `LESSON`과 `BEAUTY`의 하위 업종 taxonomy를 절대 섞지 않는다.
2. `LESSON` 선택 화면에 네일/속눈썹/붙임머리 등 BEAUTY 업종을 노출하지 않는다.
3. `BEAUTY` 선택 화면에 댄스/필라테스/PT 등 LESSON 업종을 노출하지 않는다.
4. `businessCategory`와 `businessType`은 계층형이다. flat array로 만들지 않는다.
5. 공통 도메인과 업종별 모듈을 분리한다.
6. tenant-owned 데이터는 반드시 `studio_id` 범위로 격리한다.
7. client가 전달한 `studioId`만 신뢰하지 않는다. 인증 사용자의 membership을 backend에서 검증한다.
8. PostgreSQL이 영속 비즈니스 데이터의 source of truth다.
9. Redis lock만으로 예약 정합성을 보장하지 않는다. DB transaction/constraint/recheck가 최종 보장해야 한다.
10. 핵심 비즈니스 규칙을 frontend에만 구현하지 않는다.
11. Élanor의 4/8/12회, 특정 클래스명, 특정 요일 같은 값을 하드코딩하지 않는다.
12. 고객(Customer)과 SaaS 운영자(User)를 같은 계정 모델로 합치지 않는다.
13. 기존 기능을 덮어쓰는 재등록 대신 cycle/history를 보존한다.
14. 회차 증감은 추적 가능한 ledger를 사용한다.
15. 요구사항이 모호하면 구현 전에 ambiguity/risk를 먼저 보고한다.
16. 현재 작업 scope 밖의 기능을 임의로 추가하지 않는다.
17. 새 dependency 추가 시 이유를 설명하고 기존 문서 결정과 충돌 여부를 확인한다.
18. 작업 후 관련 build/test를 실행하고 실패를 수정한다.
19. 문서와 구현이 달라졌다면 관련 문서를 함께 업데이트한다.
20. 중요한 구조 변경은 `docs/DECISIONS.md`에 기록한다.

## Work pattern

코딩 요청을 받으면 기본적으로 다음 순서를 따른다.

1. 관련 문서 읽기
2. 요구사항 요약
3. ambiguity / risk 확인
4. 구현 계획 제시
5. 허용된 scope만 구현
6. 테스트/빌드
7. 변경 요약
8. 남은 위험/후속 작업 보고
