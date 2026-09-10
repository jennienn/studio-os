package com.studioos.lesson.booking;
import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import com.studioos.lesson.cycle.*;
import com.studioos.lesson.enrollment.EnrollmentService;
import com.studioos.customer.CustomerRepository;
import com.studioos.booking.Booking;
import com.studioos.studio.StudioRepository;
import com.studioos.studio.configuration.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import static com.studioos.lesson.cycle.CycleService.conflict;
@Service
public class LessonEntitlement {
 private final CycleService cycles;private final LessonLedger ledger;private final EnrollmentService enrollments;private final LessonAccess access;
 private final CustomerRepository customers;private final StudioRepository studios;private final JdbcTemplate jdbc;private final LessonPolicyRepository lessonPolicy;private final BookingPolicyRepository bookingPolicy;
 public LessonEntitlement(CycleService cycles,LessonLedger ledger,EnrollmentService enrollments,LessonAccess access,CustomerRepository customers,StudioRepository studios,JdbcTemplate jdbc,LessonPolicyRepository lessonPolicy,BookingPolicyRepository bookingPolicy){this.cycles=cycles;this.ledger=ledger;this.enrollments=enrollments;this.access=access;this.customers=customers;this.studios=studios;this.jdbc=jdbc;this.lessonPolicy=lessonPolicy;this.bookingPolicy=bookingPolicy;}
 public EnrollmentCycle eligible(UUID studio,UUID cycle,UUID customer,String mode,UUID clazz,Instant start,UUID exclude){
  var c=cycles.find(studio,cycle);var e=enrollments.find(studio,c.enrollmentId);
  if(!e.customerId.equals(customer)||!e.kind.equals(mode)||!Objects.equals(e.classId,clazz))conflict("회원·수강·반이 일치하지 않습니다.");
  if(!e.status.equals("ACTIVE")||!c.status.equals("ACTIVE")||!customers.findByStudioIdAndId(studio,customer).orElseThrow().status.equals("ACTIVE"))conflict("활성 회원·수강·이용권이 필요합니다.");
  access.mode(studio,mode,c.deductionTriggerSnapshot);window(c,start,exclude);
  if(exclude==null&&c.productTypeSnapshot.equals("COUNT_BASED")&&ledger.balance(studio,cycle)-ledger.reserved(studio,cycle)<1)conflict("예약 가능한 회차가 부족합니다.");
  return c;
 }
 public void window(EnrollmentCycle c,Instant start,UUID exclude){
  var zone=ZoneId.of(studios.findById(c.studioId).orElseThrow().timezone);var date=start.atZone(zone).toLocalDate();var today=cycles.today(c.studioId);
  if(c.validEndDate!=null&&(today.isAfter(c.validEndDate)||date.isAfter(c.validEndDate)))conflict("이용권 유효기간 밖의 수업입니다.");
  if(c.startDate!=null&&date.isBefore(c.startDate))conflict("이용 시작일 이전입니다.");
  if(c.startDate==null&&c.validityDaysSnapshot!=null){
   if("BOOKING_CONFIRMED".equals(c.deductionTriggerSnapshot)){if(date.isAfter(today.plusDays(c.validityDaysSnapshot)))conflict("최초 사용 유효기간 밖의 수업입니다.");}
   else {
    var dates=jdbc.query("select b.start_at from bookings b join pass_entitlement_reservations r on r.studio_id=b.studio_id and r.booking_id=b.id where r.studio_id=? and r.enrollment_cycle_id=? and r.status='ACTIVE' and (?::uuid is null or b.id<>?)",(r,n)->r.getTimestamp(1).toInstant().atZone(zone).toLocalDate(),c.studioId,c.id,exclude,exclude);
    var earliest=date;var latest=date;for(var d:dates){if(d.isBefore(earliest))earliest=d;if(d.isAfter(latest))latest=d;}
    if(latest.isAfter(earliest.plusDays(c.validityDaysSnapshot)))conflict("첫 예약일부터 임시 유효기간 안으로 일정을 선택해 주세요.");
   }
  }
 }
 public void confirm(EnrollmentCycle c,Booking b,UUID actor){
  if(!c.productTypeSnapshot.equals("COUNT_BASED"))return;
  if(c.deductionTriggerSnapshot.equals("BOOKING_CONFIRMED")){cycles.initialize(c,cycles.today(c.studioId));ledger.append(c.studioId,c.id,"BOOKING_DEDUCTION",-1,"예약 확정","BOOKING",b.id,actor);}
  else jdbc.update("insert into pass_entitlement_reservations(id,studio_id,enrollment_cycle_id,booking_id,status,created_at) values (?,?,?,?,'ACTIVE',now())",UUID.randomUUID(),c.studioId,c.id,b.id);
 }
 public void resolve(EnrollmentCycle c,Booking b,String outcome,UUID actor,UUID attendance){
  if(c.productTypeSnapshot.equals("COUNT_BASED")){
   boolean deferred=!c.deductionTriggerSnapshot.equals("BOOKING_CONFIRMED");
   boolean deduct=outcome.equals("COMPLETE")&&c.deductionTriggerSnapshot.equals("LESSON_COMPLETED")||outcome.equals("PRESENT")&&c.deductionTriggerSnapshot.equals("ATTENDANCE_PRESENT");
   if(deferred){
    int count=jdbc.update("update pass_entitlement_reservations set status=?,resolved_at=now() where studio_id=? and enrollment_cycle_id=? and booking_id=? and status='ACTIVE'",deduct?"CONSUMED":"RELEASED",c.studioId,c.id,b.id);
    if(count!=1)conflict("회차 예약 상태를 확인해 주세요.");
    if(deduct){cycles.initialize(c,cycles.today(c.studioId));ledger.append(c.studioId,c.id,outcome.equals("PRESENT")?"ATTENDANCE":"LESSON_COMPLETED",-1,outcome.equals("PRESENT")?"출석":"수업 완료",attendance==null?"BOOKING":"ATTENDANCE",attendance==null?b.id:attendance,actor);}
   }else if(outcome.equals("CANCEL")&&lessonPolicy.findByStudioId(c.studioId).orElseThrow().restoreOnTimelyCancellation&&Instant.now().isBefore(b.startAt.minusSeconds(bookingPolicy.findByStudioId(c.studioId).orElseThrow().cancellationCutoffHours*3600L))){
    if(!c.status.equals("ACTIVE"))conflict("종료된 cycle은 자동 복구할 수 없습니다.");
    ledger.append(c.studioId,c.id,"CANCEL_RESTORE",1,"기한 내 예약 취소","BOOKING",b.id,actor);
   }
  }
  studios.flush();cycles.settle(c,cycles.today(c.studioId));
 }
}
