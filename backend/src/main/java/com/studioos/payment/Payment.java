package com.studioos.payment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payments")
public class Payment {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false) public UUID customerId;
    @Column(nullable=false) public long amount;
    @Column(nullable=false,length=3) public String currency;
    @Column(nullable=false,length=16) public String method;
    @Column(nullable=false,length=16) public String status;
    @Column(nullable=false,length=32) public String referenceType;
    public UUID referenceId;
    public Instant paidAt;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public UUID createdByUserId;
    protected Payment(){}
    public Payment(UUID studio,UUID customer,UUID actor){id=UUID.randomUUID();studioId=studio;customerId=customer;createdByUserId=actor;createdAt=Instant.now();currency="KRW";referenceType="OTHER";}
}
