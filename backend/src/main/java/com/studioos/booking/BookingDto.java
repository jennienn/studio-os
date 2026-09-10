package com.studioos.booking;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class BookingDto {
    private BookingDto(){}
    public enum Kind{LESSON_PRIVATE,LESSON_GROUP,BEAUTY_SERVICE}
    public enum Scope{STUDIO,STAFF}
    public record Create(@NotNull UUID customerId,@NotNull UUID staffId,@NotNull Kind bookingKind,
        @NotNull Instant startAt,@NotNull Instant endAt,@Size(max=2000) String note){}
    public record Edit(@NotNull UUID staffId,@NotNull Instant startAt,@NotNull Instant endAt,@Size(max=2000) String note){}
    public record BlockCreate(@NotNull Scope scopeType,UUID staffId,@NotNull Instant startAt,@NotNull Instant endAt,@Size(max=2000) String reason){}
    public record View(UUID id,UUID studioId,UUID customerId,String customerName,String customerPhone,UUID staffId,String staffName,
        String bookingKind,boolean manualEntry,Instant startAt,Instant endAt,String status,String note,String source){}
    public record BlockView(UUID id,String scopeType,UUID staffId,Instant startAt,Instant endAt,String reason){}
    public record StaffView(UUID id,String name){}
    public record Availability(boolean available,String code,String message){}
    public record Deleted(UUID id,boolean deleted){}
}
