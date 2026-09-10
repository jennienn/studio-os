package com.studioos.booking;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="bookings")
public class Booking {
    @Id public UUID id;
    @Column(nullable=false) public UUID studioId;
    @Column(nullable=false) public UUID customerId;
    public UUID staffId;
    @Column(nullable=false,length=32) public String bookingKind;
    @Column(nullable=false) public boolean manualEntry;
    @Column(nullable=false) public Instant startAt;
    @Column(nullable=false) public Instant endAt;
    @Column(nullable=false,length=16) public String status;
    @Column(length=2000) public String note;
    @Column(nullable=false,length=32) public String source;
    @Column(nullable=false) public Instant createdAt;
    @Column(nullable=false) public Instant updatedAt;
    protected Booking(){}
    public Booking(UUID studio,UUID customer,UUID staff,String kind,Instant start,Instant end,String note){
        id=UUID.randomUUID();studioId=studio;customerId=customer;staffId=staff;bookingKind=kind;startAt=start;endAt=end;this.note=note;
        manualEntry=true;status="CONFIRMED";source="OPERATOR";createdAt=updatedAt=Instant.now();
    }
}
