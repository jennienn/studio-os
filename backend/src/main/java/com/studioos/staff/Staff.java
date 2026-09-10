package com.studioos.staff;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="staff")
public class Staff {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    public UUID userId;
    @Column(nullable=false,length=100) public String name;
    @Column(length=32) public String phone;
    @Column(nullable=false) public boolean active;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    protected Staff() {}
    public Staff(UUID studio,UUID user,String name) {
        id=UUID.randomUUID(); studioId=studio; userId=user; this.name=name; active=true;
        createdAt=updatedAt=Instant.now();
    }
}
