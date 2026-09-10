package com.studioos.studio;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="studio_memberships")
public class StudioMembership {
    public enum Role { OWNER, MANAGER, STAFF }
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false) public UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) public Role role;
    @Column(nullable=false,length=16) public String status;
    @Column(nullable=false) public Instant createdAt;
    protected StudioMembership() {}
    public StudioMembership(UUID studio,UUID user,Role role) {
        id=UUID.randomUUID(); studioId=studio; userId=user; this.role=role;
        status="ACTIVE"; createdAt=Instant.now();
    }
}
