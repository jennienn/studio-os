package com.studioos.booking;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.time.Instant;
import java.util.*;
public interface BookingBlockRepository extends Repository<BookingBlock,UUID> {
    BookingBlock save(BookingBlock block);
    Optional<BookingBlock> findByStudioIdAndId(UUID studioId,UUID id);
    void deleteByStudioIdAndId(UUID studioId,UUID id);
    @Query("select count(b) from BookingBlock b where b.studioId=:studioId and (b.scopeType='STUDIO' or b.staffId=:staffId) and b.startAt<:end and b.endAt>:start")
    long conflicts(UUID studioId,UUID staffId,Instant start,Instant end);
    @Query("select b from BookingBlock b where b.studioId=:studioId and b.startAt<:to and b.endAt>:from")
    Page<BookingBlock> range(UUID studioId,Instant from,Instant to,Pageable pageable);
}
