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
