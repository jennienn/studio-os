# DEVELOPMENT_WORKFLOW.md

## 1. Planning before code

각 feature 개발 전:
1. 관련 요구사항 확인
2. 문서 갱신
3. agent에게 "코딩하지 말고 계획/ambiguity/risk만" 요청
4. 계획 검토
5. 구현 scope 승인
6. 구현
7. 테스트/빌드
8. 문서와 구현 비교

## 2. Agent prompt pattern

```text
Read AGENTS.md and the relevant /docs files first.
Do not code yet.

For this task:
1. summarize the requirements,
2. identify ambiguities,
3. identify domain/architecture risks,
4. propose a minimal implementation plan,
5. list tests required.

Do not change code until the plan is accepted.
```

구현 단계:

```text
Implement only the accepted plan.
Do not expand scope.
Respect DOMAIN_RULES.md and feature visibility rules.
Run the required tests and build.
Report changed files, tests, and remaining risks.
```

## 3. Definition of done

- 요구사항 충족
- LESSON/BEAUTY taxonomy 위반 없음
- tenant isolation 확인
- backend business validation 존재
- unit/integration test 추가
- frontend relevant test 추가
- build 성공
- 관련 문서 최신 상태

## 4. Avoid

- 한 프롬프트에 전체 SaaS 구현 요청
- agent가 product decision을 임의 결정하게 방치
- 테스트 없는 대규모 리팩터링
- 여러 도메인을 한 번에 변경
