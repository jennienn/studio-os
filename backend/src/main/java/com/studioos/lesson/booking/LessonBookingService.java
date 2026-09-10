package com.studioos.lesson.booking;
import com.studioos.booking.*;
import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import com.studioos.lesson.cycle.*;
import com.studioos.lesson.enrollment.EnrollmentService;
import com.studioos.security.AuthorizedStudioContext;
import com.studioos.staff.StaffRepository;
import com.studioos.studio.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.*;
@Service
public class LessonBookingService implements BookingSpecialization {
 public record PrivateCreate(@NotNull UUID customerId,@NotNull UUID enrollmentCycleId,@NotNull UUID staffId,@NotNull Instant startAt,@NotNull Instant endAt,@Size(max=2000) String note){}
 public record Created(UUID id,UUID enrollmentCycleId,String status){}
 private final LessonAccess access;private final StudioRepository studios;private final BookingRepository bookings;private final BookingAvailability availability;
 private final LessonEntitlement entitlement;private final CycleService cycles;private final EnrollmentService enrollments;private final StaffRepository staff;private final JdbcTemplate jdbc;private final IdempotencyService idem;
 public LessonBookingService(LessonAccess access,StudioRepository studios,BookingRepository bookings,BookingAvailability availability,LessonEntitlement entitlement,CycleService cycles,EnrollmentService enrollments,StaffRepository staff,JdbcTemplate jdbc,IdempotencyService idem){this.access=access;this.studios=studios;this.bookings=bookings;this.availability=availability;this.entitlement=entitlement;this.cycles=cycles;this.enrollments=enrollments;this.staff=staff;this.jdbc=jdbc;this.idem=idem;}
 @Transactional public Created create(UUID studio,String key,PrivateCreate body){var actor=access.lock(studio);return idem.execute(actor,"CREATE_PRIVATE_LESSON",key,body,201,Created.class,()->{
  var c=entitlement.eligible(studio,body.enrollmentCycleId(),body.customerId(),"PRIVATE",null,body.startAt(),null);
  availability.check(studios.findById(studio).orElseThrow(),body.staffId(),body.startAt(),body.endAt(),null);
  var b=new Booking(studio,body.customerId(),body.staffId(),"LESSON_PRIVATE",body.startAt(),body.endAt(),body.note());b.manualEntry=false;b=bookings.save(b);studios.flush();
  jdbc.update("insert into lesson_booking_details(booking_id,studio_id,enrollment_cycle_id,lesson_mode) values (?,?,?,'PRIVATE')",b.id,studio,c.id);
  entitlement.confirm(c,b,actor.authenticatedUserId());studios.flush();return new Created(b.id,c.id,b.status);
 });}
 public boolean supports(Booking b){return !b.manualEntry&&b.bookingKind.equals("LESSON_PRIVATE");}
 public void authorize(Booking b,AuthorizedStudioContext actor,String command){access.member(b.studioId);
  if(actor.membershipRole()==StudioMembership.Role.STAFF){
   var own=staff.findByStudioIdAndUserId(b.studioId,actor.authenticatedUserId()).filter(s->s.active).orElseThrow(()->new ApiException(403,"ASSIGNMENT_REQUIRED","배정된 수업만 처리할 수 있습니다."));
   if(!own.id.equals(b.staffId)||!Set.of("COMPLETE","NO_SHOW").contains(command))throw new ApiException(403,"ASSIGNMENT_REQUIRED","배정된 수업 완료·노쇼만 처리할 수 있습니다.");
  }
  if(command.equals("CONFIRM"))throw new ApiException(409,"ALREADY_CONFIRMED","레슨은 생성 시 확정됩니다.");
 }
 public void afterTransition(Booking b,AuthorizedStudioContext actor,String command){entitlement.resolve(cycle(b),b,command,actor.authenticatedUserId(),null);}
 public void validateEdit(Booking b,BookingDto.Edit body){access.manager(b.studioId);var c=cycle(b);entitlement.eligible(b.studioId,c.id,b.customerId,"PRIVATE",null,body.startAt(),b.id);}
 private EnrollmentCycle cycle(Booking b){var ids=jdbc.queryForList("select enrollment_cycle_id from lesson_booking_details where studio_id=? and booking_id=? and lesson_mode='PRIVATE'",UUID.class,b.studioId,b.id);if(ids.size()!=1)throw new ApiException(409,"LESSON_DETAIL_REQUIRED","수강 연결을 확인해 주세요.");return cycles.find(b.studioId,ids.getFirst());}
}
