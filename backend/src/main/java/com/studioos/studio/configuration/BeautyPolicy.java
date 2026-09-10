package com.studioos.studio.configuration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="beauty_policies")
public class BeautyPolicy {
    @Id public UUID studioId;
    @Column(nullable=false) public boolean depositEnabled;
    @Column(nullable=false) public boolean noShowEnabled;
    @Column(nullable=false) public Instant updatedAt;
    protected BeautyPolicy() {}
    public BeautyPolicy(UUID studioId) { this.studioId=studioId; }
}
