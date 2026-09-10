package com.studioos.studio.configuration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="lesson_policies")
public class LessonPolicy {
    @Id public UUID studioId;
    @Column(nullable=false) public int lowBalanceThreshold;
    @Column(nullable=false) public int expiryAlertDays;
    @Column(nullable=false) public boolean restoreOnTimelyCancellation;
    @Column(nullable=false) public Instant updatedAt;
    protected LessonPolicy() {}
    public LessonPolicy(UUID studioId) { this.studioId=studioId; }
}
