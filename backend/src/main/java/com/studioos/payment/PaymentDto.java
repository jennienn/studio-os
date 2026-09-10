package com.studioos.payment;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class PaymentDto {
    private PaymentDto(){}
    public enum Method {CARD,CASH,TRANSFER,OTHER}
    public enum Status {PENDING,PAID,REFUNDED,CANCELLED}
    public record Create(@NotNull UUID customerId,@NotBlank @Pattern(regexp="[0-9]{1,19}") String amount,
        @NotNull Method method,@NotNull Status status,@NotBlank @Pattern(regexp="KRW") String currency,
        @NotBlank @Pattern(regexp="OTHER") String referenceType,UUID referenceId,@PastOrPresent Instant paidAt){}
    public record Confirm(@PastOrPresent Instant paidAt){}
    public record Refund(@NotBlank @Size(max=2000) String reason){}
    public record RefundView(UUID id,String amount,String reason,Instant refundedAt,UUID createdByUserId){}
    public record View(UUID id,UUID studioId,UUID customerId,String customerName,String amount,String currency,String method,
        String status,String referenceType,UUID referenceId,Instant paidAt,Instant createdAt,UUID createdByUserId,RefundView refund){}
}
