package com.studioos.studio.configuration;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="booking_policies")
public class BookingPolicy {
    @Id public UUID studioId;
    @Column(nullable=false) public int slotIntervalMinutes;
    @Column(nullable=false) public int bookingWindowDays;
    @Column(nullable=false) public int cancellationCutoffHours;
    @Column(nullable=false) public Instant updatedAt;
    protected BookingPolicy() {}
    public BookingPolicy(UUID studioId) { this.studioId=studioId; }
}
