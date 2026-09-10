# ONBOARDING.md

## 1. Goal

온보딩은 단순 업종 설문이 아니라 `StudioCapabilities`와 초기 운영 설정을 생성하는 과정이다.

## 2. Step 1 — category

질문:
**어떤 형태의 사업장을 운영하시나요?**

- 레슨 / 스튜디오 (`LESSON`)
- 뷰티 / 예약 서비스 (`BEAUTY`)

## 3. Step 2 — subtype

### LESSON only
- 댄스
- 필라테스
- 요가
- PT
- 폴댄스
- 보컬/음악
- 기타 레슨

### BEAUTY only
- 네일
- 속눈썹
- 붙임머리
- 왁싱
- 1인 미용
- 기타 뷰티

상대 category의 업종은 절대 렌더링하지 않는다.

## 4. LESSON flow

아래 §§4–6은 전체 MVP 목표 흐름이다. 현재 Phase 3 구현 범위와 저장 결과는 §8을 따른다.
초기 상품/반/서비스 및 차감 시점 설정은 해당 도메인이 구현되는 후속 Phase로 미룬다.

1. 사업장 형태
2. 세부 업종
3. 수업 형태
   - 개인
   - 그룹
4. 상품 형태
   - 회차권
   - 기간권
   - 월정액
   - 단회
5. 예약 운영
   - 예약 사용 여부
   - 예약 가능 기간
   - 취소 가능 시간
6. 출석/차감
   - 출석 관리 여부
   - 차감 시점
7. 재등록 알림
   - 잔여 회차 기준
   - 만료일 기준
8. 초기 상품/반 생성

## 5. BEAUTY flow

1. 사업장 형태
2. 세부 업종
3. 예약 방식
   - 1:1 예약
   - 담당자 지정 여부
4. 시술 메뉴
   - 이름
   - 소요시간
   - 가격
5. 예약금
   - 사용 여부
   - 금액/비율
6. 고객 기록
   - 시술 이력 (MVP)
   - 메모 (MVP)
   - 시술 사진 (P1, MVP 온보딩에서는 설정하지 않음)
7. 재방문
   - 재방문 관리 여부
   - 서비스별 권장 주기
8. 노쇼 정책

## 6. Result

온보딩 완료 시 저장:

```text
Studio.businessCategory
Studio.businessType
StudioCapabilities
StudioBookingPolicy
LessonPolicy OR BeautyPolicy
initial products/classes/services
```

## 7. Editing after onboarding

온보딩 결과는 영구 고정이 아니다.
설정 화면에서 운영 방식 변경 가능해야 한다.

단 category 변경은 데이터 영향이 크므로 MVP에서는 일반 설정으로 제공하지 않고 별도 migration flow로 제한한다.

OWNER만 현재 category 안에서 businessType을 변경할 수 있으며 과거 데이터는 삭제/자동 migration하지 않는다.
구조 설정과 기능 활성화는 OWNER-only다. MANAGER는 영업시간, 예약 정책, 레슨 정책,
뷰티 노쇼 설정만 수정할 수 있다. STAFF는 조회만 가능하다 (ADR-051, SECURITY.md).

## 8. Phase 3 flow and persistence

6단계: category → 해당 subtype → 기능 → 영업시간 → 예약/업종별 정책 → 요약/완료.
초안은 브라우저 폼 메모리에만 유지하며, 새로고침하면 미저장 초안은 초기화된다.
OWNER의 최종 제출에서 backend가 전체 설정을 검증하고 하나의 transaction으로 저장한다.
실패하면 PRE_ONBOARDING을 유지하며, 성공하면 ACTIVE로 전환한다. 반복 완료는 409다.

저장: Studio.category/type, StudioCapability, BusinessHours, BookingPolicy 및 LessonPolicy OR BeautyPolicy.
각 요일(월요일 1~일요일 7)에 한 행, 영업일은 시작 < 종료, 휴무일은 시간이 필요 없다.
예약 기본값은 BOOKING.md의 30분 간격 / 30일 / 취소 12시간 전이다.
레슨 알림 기준 및 취소 복구, 뷰티 노쇼 정책은 사용자가 명시적으로 선택한다.

LESSON은 PASS_MANAGEMENT가 필수다. PRIVATE_LESSON/GROUP_CLASS 중 하나 이상이 필요하며,
ATTENDANCE는 GROUP_CLASS가 켜져 있을 때만 허용한다. CUSTOMER_BOOKING은 선택 사항이다.
BEAUTY는 CUSTOMER_BOOKING/DEPOSIT/REVISIT만 선택하며 DEPOSIT와 depositEnabled는 항상 함께 변경한다.
상품·반·시술·예약·결제 등 업무 레코드는 Phase 3에서 생성하지 않는다.

완료 후 /app에서 저장된 설정을 확인하고 /app/settings에서 권한에 맞게 수정한다.
수정은 전체 설정과 version을 보내며, 다른 변경으로 version이 달라졌으면 최신 설정을 다시 불러온다.
