package com.studioos.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="auth_accounts")
public class AuthAccount {
    @Id public UUID id;
    @Column(nullable=false) public UUID userId;
    @Column(nullable=false,length=16) public String provider;
    @Column(nullable=false,length=254) public String providerUserId;
    @Column(length=100) public String passwordHash;
    public Instant verifiedAt;
    @Column(nullable=false) public Instant createdAt;
    protected AuthAccount() {}
    public AuthAccount(UUID userId, String provider, String identity, String hash) {
        id=UUID.randomUUID(); this.userId=userId; this.provider=provider;
        providerUserId=identity; passwordHash=hash; createdAt=Instant.now();
    }
}
