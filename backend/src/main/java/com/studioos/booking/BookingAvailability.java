package com.studioos.booking;
import com.studioos.common.ApiException;
import com.studioos.staff.StaffRepository;
import com.studioos.studio.Studio;
import com.studioos.studio.configuration.BusinessHoursRepository;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.UUID;
@Service
public class BookingAvailability {
    private final BusinessHoursRepository hours;private final BookingRepository bookings;private final BookingBlockRepository blocks;private final StaffRepository staff;
    public BookingAvailability(BusinessHoursRepository hours,BookingRepository bookings,BookingBlockRepository blocks,StaffRepository staff){this.hours=hours;this.bookings=bookings;this.blocks=blocks;this.staff=staff;}
    public static void interval(Instant start,Instant end){if(start==null || end==null || !start.isBefore(end))throw new ApiException(400,"INVALID_INTERVAL","시작 시간은 종료 시간보다 빨라야 합니다.");}
    public void staff(UUID studio,UUID staffId){if(staffId==null || staff.findByStudioIdAndId(studio,staffId).filter(s->s.active).isEmpty())throw new ApiException(404,"STAFF_NOT_FOUND","이 사업장의 활성 담당자를 선택해 주세요.");}
    public void check(Studio studio,UUID staffId,Instant start,Instant end,UUID exclude){
        interval(start,end);if(start.isBefore(Instant.now()))throw new ApiException(400,"PAST_BOOKING","과거 시각으로 예약을 생성하거나 이동할 수 없습니다.");
        staff(studio.id,staffId);
        var localStart=start.atZone(ZoneId.of(studio.timezone));var localEnd=end.atZone(ZoneId.of(studio.timezone));
        var day=hours.findByStudioIdOrderByWeekdayAsc(studio.id).stream().filter(h->h.weekday==localStart.getDayOfWeek().getValue()).findFirst();
        if(!localStart.toLocalDate().equals(localEnd.toLocalDate()) || day.isEmpty() || day.get().closed
            || localStart.toLocalTime().isBefore(day.get().openTime) || localEnd.toLocalTime().isAfter(day.get().closeTime))
            throw new ApiException(409,"OUTSIDE_BUSINESS_HOURS","사업장 영업시간 밖입니다.");
        if(blocks.conflicts(studio.id,staffId,start,end)>0)throw new ApiException(409,"BOOKING_BLOCKED","예약이 차단된 시간입니다.");
        if(bookings.conflicts(studio.id,staffId,start,end,exclude)>0)throw new ApiException(409,"BOOKING_CONFLICT","담당자의 기존 예약과 시간이 겹칩니다.");
    }
}
