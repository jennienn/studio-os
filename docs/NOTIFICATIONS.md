# NOTIFICATIONS.md

## 1. Goal

알림은 business event와 external provider를 분리한다.

```text
Business Event
→ NotificationService
→ Provider Adapter
```

## 2. Channels

- Kakao 알림톡
- SMS
- Email

MVP에서는 mock/manual status로 시작 가능하다.

## 3. LESSON notification types

- PASS_LOW
- PASS_EXPIRING
- RENEWAL_DUE
- CLASS_REMINDER
- PAYMENT_DUE

## 4. BEAUTY notification types

- BOOKING_REMINDER
- REVISIT_DUE
- DEPOSIT_REQUIRED
- PAYMENT_DUE

## 5. Status

- PENDING
- SENT
- FAILED
- SKIPPED

## 6. Provider isolation

금지:
```text
EnrollmentService 내부에서 Kakao SDK 직접 호출
```

권장:
```text
Enrollment event
→ Notification application service
→ Kakao adapter
```

## 7. Privacy

로그에 메시지 전문/전화번호/인증코드를 불필요하게 남기지 않는다.
