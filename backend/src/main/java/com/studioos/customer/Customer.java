package com.studioos.customer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="customers")
public class Customer {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false,length=100) public String name;
    @Column(nullable=false,length=32) public String phone;
    @Column(nullable=false,length=11) public String normalizedPhone;
    @Column(nullable=false,length=2000) public String memo;
    @Column(nullable=false,length=16) public String status;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    public Instant archivedAt;
    protected Customer() {}
    public Customer(UUID studioId,String name,String phone,String normalizedPhone,String memo) {
        id=UUID.randomUUID();this.studioId=studioId;this.name=name;this.phone=phone;
        this.normalizedPhone=normalizedPhone;this.memo=memo;status="ACTIVE";
        createdAt=updatedAt=Instant.now();
    }
}
