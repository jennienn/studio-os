package com.studioos.studio.configuration;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;
@Entity @Table(name="business_hours")
public class BusinessHours {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false) public int weekday;
    public LocalTime openTime;
    public LocalTime closeTime;
    @Column(nullable=false) public boolean closed;
    protected BusinessHours() {}
    public BusinessHours(UUID studioId,int weekday) {
        id=UUID.randomUUID(); this.studioId=studioId; this.weekday=weekday;
    }
}
