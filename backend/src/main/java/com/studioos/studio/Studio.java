package com.studioos.studio;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="studios")
public class Studio {
    @Id public UUID id;
    @Column(nullable=false,length=100) public String name;
    @Column(nullable=false,length=63) public String slug;
    @Column(length=16) public String businessCategory;
    @Column(length=32) public String businessType;
    @Column(nullable=false,length=64) public String timezone;
    @Column(nullable=false,length=32) public String status;
    @Column(nullable=false) public long configurationVersion;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    protected Studio() {}
    public Studio(String name,String slug,String timezone) {
        id=UUID.randomUUID(); this.name=name; this.slug=slug; this.timezone=timezone;
        status="PRE_ONBOARDING"; createdAt=updatedAt=Instant.now();
    }
}
