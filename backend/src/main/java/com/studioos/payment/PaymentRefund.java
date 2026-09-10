package com.studioos.payment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payment_refunds")
public class PaymentRefund {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false) public UUID paymentId;
    @Column(nullable=false) public long amount;
    @Column(nullable=false,length=2000) public String reason;
    @Column(nullable=false) public Instant refundedAt;
    @Column(nullable=false) public UUID createdByUserId;
    protected PaymentRefund(){}
    public PaymentRefund(Payment p,String reason,UUID actor){id=UUID.randomUUID();studioId=p.studioId;paymentId=p.id;amount=p.amount;this.reason=reason;createdByUserId=actor;refundedAt=Instant.now();}
}
