package com.studioos.booking;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="booking_blocks")
public class BookingBlock {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false,length=16) public String scopeType;
    public UUID staffId;
    @Column(nullable=false) public Instant startAt;
    @Column(nullable=false) public Instant endAt;
    @Column(length=2000) public String reason;
    @Column(nullable=false) public Instant createdAt;
    protected BookingBlock(){}
    public BookingBlock(UUID studio,String scope,UUID staff,Instant start,Instant end,String reason){id=UUID.randomUUID();studioId=studio;scopeType=scope;staffId=staff;startAt=start;endAt=end;this.reason=reason;createdAt=Instant.now();}
}
