# 검증 결과

- `npm install`: 성공, audit 취약점 0건 (PostCSS / sharp 패치 override 적용)
- `npm run typecheck`: 성공
- `npm run build`: 성공, 랜딩 및 13개 제품 페이지 생성
- 프로덕션 서버 Chromium 검증: 성공
  - 랜딩 → 데모 입장 → 7단계 온보딩 → 대시보드
  - 개인 레슨만 선택하면 그룹 출석 메뉴 숨김
  - 새로고침 후 스튜디오 설정과 회차 유지
  - 이용권 생성, 회원 생성·검색·상세
  - 설정에서 그룹 레슨 다시 활성화
  - 전체 선택·일괄 출석·재진입 시 중복 차감 없음
  - 달력 월 이동
  - 390px에서 랜딩·온보딩·대시보드·일정 문서 가로 넘침 없음
  - 브라우저 JavaScript 오류 없음
  - 입금 확인 → 재등록 → 재결제, 이전 주기 보존 및 회차 초기화
  - 수업 생성 → 예약 → 취소

스크린샷: `landing-desktop.png`, `landing-mobile.png`, `dashboard-desktop.png`.
테스트는 로컬에 이미 설치된 Playwright/Chromium을 사용했으며 제품 런타임 의존성에는 추가하지 않았습니다.

## 2026-09-10 사업장 모드 확장

프로덕션 Chromium 검증 통과: 레슨/뷰티 미리보기 전환, 선택 모드 온보딩 전달, 뷰티 온보딩 완료, 재진입 후 설정 유지, 예약 추가/방문 완료, 시술 메모 저장, 예약금 결제 확인, 기록/재방문/노쇼 기능 끄기, 직접 URL 비활성 안내, 혼합 모듈 메뉴, 기존 저장 데이터 마이그레이션. 390px에서 랜딩·온보딩·대시보드·예약·설정 가로 넘침 없음. 브라우저 오류 없음.

화면: `beauty-landing-desktop.png`, `beauty-landing-mobile.png`, `beauty-dashboard.png`.
