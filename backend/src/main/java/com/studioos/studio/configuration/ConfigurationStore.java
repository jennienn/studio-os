package com.studioos.studio.configuration;

import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.*;
import static com.studioos.studio.configuration.StudioTaxonomy.*;
import static com.studioos.studio.configuration.ConfigurationDto.*;

/** Called only inside the service transaction, with its authorized Studio ID. */
@Component
class ConfigurationStore {
    private final StudioCapabilityRepository capabilities;
    private final BusinessHoursRepository hours;
    private final BookingPolicyRepository booking;
    private final LessonPolicyRepository lesson;
    private final BeautyPolicyRepository beauty;

    ConfigurationStore(StudioCapabilityRepository capabilities,BusinessHoursRepository hours,
                       BookingPolicyRepository booking,LessonPolicyRepository lesson,BeautyPolicyRepository beauty) {
        this.capabilities=capabilities; this.hours=hours; this.booking=booking; this.lesson=lesson; this.beauty=beauty;
    }

    Settings read(UUID studioId) {
        Map<Capability,Boolean> flags=new EnumMap<>(Capability.class);
        capabilities.findByStudioIdOrderByCapabilityAsc(studioId).forEach(c -> flags.put(c.capability,c.enabled));
        List<Hours> days=hours.findByStudioIdOrderByWeekdayAsc(studioId).stream()
            .map(h -> new Hours(h.weekday,h.openTime,h.closeTime,h.closed)).toList();
        Booking b=booking.findByStudioId(studioId)
            .map(p -> new Booking(p.slotIntervalMinutes,p.bookingWindowDays,p.cancellationCutoffHours)).orElse(null);
        Lesson l=lesson.findByStudioId(studioId)
            .map(p -> new Lesson(p.lowBalanceThreshold,p.expiryAlertDays,p.restoreOnTimelyCancellation)).orElse(null);
        Beauty a=beauty.findByStudioId(studioId)
            .map(p -> new Beauty(p.depositEnabled,p.noShowEnabled)).orElse(null);
        return new Settings(flags,days,b,l,a);
    }

    void write(UUID studioId,Command command) {
        Map<Capability,StudioCapability> existing=new EnumMap<>(Capability.class);
        capabilities.findByStudioIdOrderByCapabilityAsc(studioId).forEach(c -> existing.put(c.capability,c));
        command.capabilities().forEach((key,enabled) -> {
            var row=existing.getOrDefault(key,new StudioCapability(studioId,key));
            row.enabled=enabled; capabilities.save(row);
        });
        // Update rows in place; disabling a feature never deletes any row or history.
        Map<Integer,BusinessHours> days=new HashMap<>();
        hours.findByStudioIdOrderByWeekdayAsc(studioId).forEach(h -> days.put(h.weekday,h));
        for (Hours input:command.businessHours()) {
            var row=days.getOrDefault(input.weekday(),new BusinessHours(studioId,input.weekday()));
            row.closed=input.closed();
            row.openTime=row.closed ? null : input.openTime();
            row.closeTime=row.closed ? null : input.closeTime();
            hours.save(row);
        }
        var bp=booking.findByStudioId(studioId).orElseGet(() -> new BookingPolicy(studioId));
        bp.slotIntervalMinutes=command.bookingPolicy().slotIntervalMinutes();
        bp.bookingWindowDays=command.bookingPolicy().bookingWindowDays();
        bp.cancellationCutoffHours=command.bookingPolicy().cancellationCutoffHours();
        bp.updatedAt=Instant.now(); booking.save(bp);
        if (command.businessCategory()==Category.LESSON) {
            var lp=lesson.findByStudioId(studioId).orElseGet(() -> new LessonPolicy(studioId));
            lp.lowBalanceThreshold=command.lessonPolicy().lowBalanceThreshold();
            lp.expiryAlertDays=command.lessonPolicy().expiryAlertDays();
            lp.restoreOnTimelyCancellation=command.lessonPolicy().restoreOnTimelyCancellation();
            lp.updatedAt=Instant.now(); lesson.save(lp);
        } else {
            var ap=beauty.findByStudioId(studioId).orElseGet(() -> new BeautyPolicy(studioId));
            ap.depositEnabled=command.beautyPolicy().depositEnabled();
            ap.noShowEnabled=command.beautyPolicy().noShowEnabled();
            ap.updatedAt=Instant.now(); beauty.save(ap);
        }
    }
}
