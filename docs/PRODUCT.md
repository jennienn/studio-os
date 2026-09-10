# PRODUCT.md

## 1. Product definition

**예약 기반 소규모 서비스 사업장을 위한 맞춤형 운영 SaaS**

사업장 유형과 실제 운영 방식에 따라 고객, 예약, 결제, 출석·회차권, 시술이력·재방문 등의 기능을 맞춤 구성한다.

## 2. Initial target

### LESSON / 레슨·스튜디오
- 댄스
- 필라테스
- 요가
- PT
- 폴댄스
- 보컬/음악레슨
- 기타 개인/그룹 레슨

### BEAUTY / 뷰티·예약 서비스
- 네일
- 속눈썹
- 붙임머리
- 왁싱
- 1인 미용
- 기타 예약형 뷰티 서비스

## 3. Shared problem

소규모 사업장은 별도 운영 인력이 부족하여 서비스 제공자가 예약, 고객관리, 결제, 일정, 이력, 재등록/재방문까지 직접 관리하는 경우가 많다.

기존 도구는 다음 중 하나의 문제가 있다.

- 특정 업종에 지나치게 고정됨
- 단순 예약만 가능하고 운영 전반을 연결하지 못함
- 소규모 사업장에 비해 기능이 과도하고 복잡함
- 회차/결제/재방문 같은 실제 운영 흐름이 분리되어 있음

## 4. Product principle

이 서비스는 모든 업종에 동일한 기능을 강요하지 않는다.

```text
Common Core
+ Business Category Defaults
+ Operation Configuration
= Personalized Admin
```

업종 선택은 recommended default를 정하는 데 사용하고 실제 기능 노출은 운영 설정(capabilities)에 의해 결정한다.

## 5. Common core

- Customer
- Booking
- Payment
- Staff
- Notification
- Notes
- Basic analytics
- Business configuration

## 6. Lesson specialization

- 수업/반
- 개인·그룹 수강
- 이용권/회차권
- 출석
- 회차 차감/복구
- 재등록
- 보강/결석 정책

## 7. Beauty specialization

- 시술 메뉴
- 시술 이력
- 시술 메모/사진
- 예약금
- 노쇼
- 재방문 주기

## 8. Existing validated base

기존 Élanor 학원 운영 시스템의 실사용 경험에서 다음 기능을 계승한다.

- 회원 관리
- 개인/단체 수강 등록
- 회차 관리
- 결제/재등록
- 수업 일정
- 고객 직접 예약
- 예약 시간 차단
- 출석
- 운영자 메모
- 알림 상태
- 과거 cycle 이력 보존

새 SaaS에서는 Élanor-specific hardcoding을 제거하고 configurable domain으로 일반화한다.

## 9. Product positioning

본 서비스는 "학원 관리 프로그램"이나 "뷰티 예약 앱" 한쪽에 고정되지 않는다.

핵심 포지션은 다음과 같다.

> 예약을 중심으로 고객의 이용 이력과 결제, 반복 방문까지 연결하는 소규모 서비스 사업장 운영 SaaS

## 10. MVP goal

MVP는 두 archetype이 동일한 core 위에서 정상적으로 동작함을 증명한다.

- LESSON: 고객 → 이용권 → 예약 → 수업완료/출석 → 회차차감 → 재등록
- BEAUTY: 고객 → 서비스 → 예약 → 시술완료 → 결제/예약금 → 재방문

모든 possible industry를 완성하는 것이 MVP의 목표가 아니다.
