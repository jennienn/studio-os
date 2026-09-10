package com.studioos.booking;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
public interface BookingRepository extends Repository<Booking,UUID> {
    Booking save(Booking booking);
    Optional<Booking> findByStudioIdAndId(UUID studioId,UUID id);
    @Query("select b from Booking b where b.studioId=:studioId and b.startAt<:to and b.endAt>:from and (:staffId is null or b.staffId=:staffId) and (:status='ALL' or b.status=:status)")
    Page<Booking> range(UUID studioId,Instant from,Instant to,UUID staffId,String status,Pageable pageable);
    @Query("select count(b) from Booking b where b.studioId=:studioId and b.status in ('PENDING','CONFIRMED') and b.bookingKind in ('LESSON_PRIVATE','BEAUTY_SERVICE') and b.startAt<:end and b.endAt>:start and (:staffId is null or b.staffId=:staffId) and (:excludeId is null or b.id<>:excludeId)")
    long conflicts(UUID studioId,UUID staffId,Instant start,Instant end,UUID excludeId);
    boolean existsByStudioIdAndCustomerIdAndStatusIn(UUID studioId,UUID customerId,Collection<String> statuses);
}
