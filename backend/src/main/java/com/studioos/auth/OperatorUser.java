package com.studioos.auth;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="users")
public class OperatorUser {
    @Id public UUID id;
    @Column(length=254) public String email;
    @Column(nullable=false,length=100) public String name;
    @Column(nullable=false,length=32) public String status;
    @Column(nullable=false) public long securityVersion;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    protected OperatorUser() {}
    public OperatorUser(String email, String name, String status) {
        this.id=UUID.randomUUID(); this.email=email; this.name=name; this.status=status;
        this.createdAt=this.updatedAt=Instant.now();
    }
}
