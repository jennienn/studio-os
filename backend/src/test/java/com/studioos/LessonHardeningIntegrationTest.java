package com.studioos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.studioos.lesson.LessonMaintenance;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LessonHardeningIntegrationTest extends LessonTestBase {
 @Autowired LessonMaintenance maintenance;
 String renew(Fixture f,Lesson l,UUID product,String key) throws Exception{return json(call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key",key).content("{\"passProductId\":\""+product+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated())).get("id").asText();}
 UUID product(Fixture f,Lesson l){return jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());}
 String scheduleBody(int weekday,String time){return "{\"weekday\":"+weekday+",\"startTime\":\""+time+"\",\"durationMinutes\":60,\"active\":true}";}

 @Test void exactSuccessfulReplaysSurviveCapabilityDisable() throws Exception{
  var f=fixture();var c=clazz(f);String classBody="{\"name\":\"Replay\",\"capacity\":2,\"instructorStaffId\":\""+f.staff()+"\",\"active\":true}";
  var made=json(call(f.user(),post(f.base()+"/lesson/classes").header("Idempotency-Key","class-replay").content(classBody)).andExpect(status().isCreated()));UUID replayClass=UUID.fromString(made.get("id").asText());
  String schedule=scheduleBody(2,"11:00");var saved=json(call(f.user(),post(f.base()+"/lesson/classes/"+replayClass+"/schedules").header("Idempotency-Key","schedule-replay").content(schedule)).andExpect(status().isCreated()));
  var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);String bookingBody=body(l,o);var booked=json(call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","booking-replay").content(bookingBody)).andExpect(status().isCreated()));UUID b=UUID.fromString(booked.get("id").asText());
  jdbc.update("update studio_capabilities set enabled=false where studio_id=? and capability='GROUP_CLASS'",f.studio());
  assertThat(json(call(f.user(),post(f.base()+"/lesson/classes").header("Idempotency-Key","class-replay").content(classBody)).andExpect(status().isCreated())).get("id")).isEqualTo(made.get("id"));
  assertThat(json(call(f.user(),post(f.base()+"/lesson/classes/"+replayClass+"/schedules").header("Idempotency-Key","schedule-replay").content(schedule)).andExpect(status().isCreated())).get("id")).isEqualTo(saved.get("id"));
  assertThat(json(call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","booking-replay").content(bookingBody)).andExpect(status().isCreated())).get("id")).isEqualTo(booked.get("id"));
  call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","booking-replay").content(bookingBody.replace(l.customer().toString(),UUID.randomUUID().toString()))).andExpect(status().isConflict());
  jdbc.update("update studio_capabilities set enabled=true where studio_id=? and capability='GROUP_CLASS'",f.studio());String attendance=f.base()+"/lesson/occurrences/"+o+"/attendance";
  call(f.user(),put(attendance).header("Idempotency-Key","attendance-replay").content(mark(b,"PRESENT"))).andExpect(status().isOk());jdbc.update("update studio_capabilities set enabled=false where studio_id=? and capability='ATTENDANCE'",f.studio());
  call(f.user(),put(attendance).header("Idempotency-Key","attendance-replay").content(mark(b,"PRESENT"))).andExpect(status().isOk());
 }

 @Test void exactBookingOutcomeReplayPrecedesCurrentCategoryBusinessValidation() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(2,10));call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","outcome-replay")).andExpect(status().isOk());
  jdbc.update("update studios set business_category='BEAUTY',business_type='NAIL' where id=?",f.studio());
  call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","outcome-replay")).andExpect(status().isOk());
  call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","new-outcome")).andExpect(status().isForbidden());
  assertThat(balance(f,l)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=? and event_type='LESSON_COMPLETED'",Integer.class,f.studio(),l.cycle())).isEqualTo(1);
 }

 @Test void bookingDateAfterCycleValidityIsRejectedEvenBeforeExpiryJobRuns() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);jdbc.update("update enrollment_cycles set start_date=current_date,valid_end_date=current_date+1 where studio_id=? and id=?",f.studio(),l.cycle());
  call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","after-validity").content(privateBody(f,l,future(2,10)))).andExpect(status().isConflict());
  assertThat(jdbc.queryForObject("select count(*) from lesson_booking_details where studio_id=? and enrollment_cycle_id=?",Integer.class,f.studio(),l.cycle())).isZero();
 }

 @Test void firstUseMultipleReservationsInitializeOnFirstDeductionAndAllSettle() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var first=book(f,l,future(1,10));var second=book(f,l,future(20,10));
  assertThat(jdbc.queryForObject("select start_date is null from enrollment_cycles where studio_id=? and id=?",Boolean.class,f.studio(),l.cycle())).isTrue();
  call(f.user(),post(path(f,first,"complete")).header("Idempotency-Key","first-complete")).andExpect(status().isOk());
  LocalDate started=jdbc.queryForObject("select start_date from enrollment_cycles where studio_id=? and id=?",LocalDate.class,f.studio(),l.cycle());LocalDate end=jdbc.queryForObject("select valid_end_date from enrollment_cycles where studio_id=? and id=?",LocalDate.class,f.studio(),l.cycle());
  assertThat(started).isEqualTo(LocalDate.now(ZoneId.of("Asia/Seoul")));assertThat(end).isEqualTo(started.plusDays(30));
  call(f.user(),post(path(f,second,"complete")).header("Idempotency-Key","second-complete")).andExpect(status().isOk());
  assertThat(balance(f,l)).isZero();assertThat(jdbc.queryForObject("select count(*) from pass_entitlement_reservations where studio_id=? and enrollment_cycle_id=? and status='CONSUMED'",Integer.class,f.studio(),l.cycle())).isEqualTo(2);
 }

 @Test void expiredCycleWaitsForDeferredCancellationOrNoShowBeforeActivatingSuccessor() throws Exception{
  for(String outcome:List.of("cancel","no-show")){
   var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var booking=book(f,l,future(2,10));String successor=renew(f,l,product(f,l),"renew-"+outcome);
   jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),l.cycle());maintenance.expire(f.studio());
   assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("ACTIVE");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),UUID.fromString(successor))).isEqualTo("SCHEDULED");
   call(f.user(),post(path(f,booking,outcome)).header("Idempotency-Key","resolve-"+outcome)).andExpect(status().isOk());
   assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("EXPIRED");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),UUID.fromString(successor))).isEqualTo("ACTIVE");
  }
 }

 @Test void expiredCycleWaitsForGroupAbsenceBeforeActivatingSuccessor() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,l,o);String successor=renew(f,l,product(f,l),"renew-group");
  jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),l.cycle());maintenance.expire(f.studio());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("ACTIVE");
  call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","absent-expired").content(mark(b,"ABSENT"))).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("EXPIRED");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),UUID.fromString(successor))).isEqualTo("ACTIVE");
 }

 @Test void refundScheduledOrActiveCycleKeepsSuccessorLifecycleConsistent() throws Exception{
  var scheduledFixture=fixture();var active=lesson(scheduledFixture,"LESSON_COMPLETED",2,null);String scheduled=renew(scheduledFixture,active,product(scheduledFixture,active),"scheduled");UUID scheduledPayment=jdbc.queryForObject("select payment_id from lesson_cycle_payments where studio_id=? and enrollment_cycle_id=?",UUID.class,scheduledFixture.studio(),UUID.fromString(scheduled));
  call(scheduledFixture.user(),post(scheduledFixture.base()+"/payments/"+scheduledPayment+"/refund").header("Idempotency-Key","refund-scheduled").content("{\"reason\":\"Unused\"}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,scheduledFixture.studio(),active.cycle())).isEqualTo("ACTIVE");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,scheduledFixture.studio(),UUID.fromString(scheduled))).isEqualTo("CANCELLED");
  var activeFixture=fixture();var old=lesson(activeFixture,"LESSON_COMPLETED",2,null);String next=renew(activeFixture,old,product(activeFixture,old),"next");
  call(activeFixture.user(),post(activeFixture.base()+"/payments/"+old.payment()+"/refund").header("Idempotency-Key","refund-active").content("{\"reason\":\"Unused\"}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,activeFixture.studio(),old.cycle())).isEqualTo("CANCELLED");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,activeFixture.studio(),UUID.fromString(next))).isEqualTo("ACTIVE");
 }

 @Test void scheduleEditPreservesFutureOccurrenceWithOnlyCancelledBooking() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"BOOKING_CONFIRMED",2,c);var b=groupBook(f,l,o);call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel-history")).andExpect(status().isOk());
  UUID s=jdbc.queryForObject("select class_schedule_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),o);call(f.user(),put(f.base()+"/lesson/classes/"+c+"/schedules/"+s).header("Idempotency-Key","edit-cancelled").content(scheduleBody(1,"11:00"))).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=? and id=?",Integer.class,f.studio(),o)).isEqualTo(1);
 }

 @Test void privateAndGroupInstructorCollisionIsRejectedInBothCreationOrders() throws Exception{
  var privateFirst=fixture();var privateLesson=lesson(privateFirst,"LESSON_COMPLETED",2,null);LocalDate monday=LocalDate.now(ZoneId.of("Asia/Seoul")).with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));Instant start=monday.atTime(10,0).atZone(ZoneId.of("Asia/Seoul")).toInstant();book(privateFirst,privateLesson,start);var c1=clazz(privateFirst);
  call(privateFirst.user(),post(privateFirst.base()+"/lesson/classes/"+c1+"/schedules").header("Idempotency-Key","private-first").content(scheduleBody(1,"10:00"))).andExpect(status().isConflict());
  var groupFirst=fixture();var c2=clazz(groupFirst);var o=occurrence(groupFirst,c2);Instant groupStart=jdbc.queryForObject("select start_at from class_occurrences where studio_id=? and id=?",java.sql.Timestamp.class,groupFirst.studio(),o).toInstant();var l2=lesson(groupFirst,"LESSON_COMPLETED",2,null);
  call(groupFirst.user(),post(groupFirst.base()+"/lesson/private-bookings").header("Idempotency-Key","group-first").content(privateBody(groupFirst,l2,groupStart))).andExpect(status().isConflict());
 }

 @Test void assignedStaffCanAttendAndNonassignedStaffCannotUseLessonWritePaths() throws Exception{
  var f=fixture();UUID assignedUser=member(f,"STAFF"),assignedStaff=UUID.randomUUID();jdbc.update("insert into staff(id,studio_id,user_id,name,active,created_at,updated_at) values (?,?,?,'Assigned',true,now(),now())",assignedStaff,f.studio(),assignedUser);
  group(f);String classBody="{\"name\":\"Assigned class\",\"capacity\":2,\"instructorStaffId\":\""+assignedStaff+"\",\"active\":true}";UUID c=UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/classes").header("Idempotency-Key","assigned-class").content(classBody)).andExpect(status().isCreated())).get("id").asText());var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,l,o);
  call(assignedUser,put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","assigned-attendance").content(mark(b,"PRESENT"))).andExpect(status().isOk());
  UUID otherUser=member(f,"STAFF"),otherStaff=UUID.randomUUID();jdbc.update("insert into staff(id,studio_id,user_id,name,active,created_at,updated_at) values (?,?,?,'Other',true,now(),now())",otherStaff,f.studio(),otherUser);
  call(otherUser,put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","wrong-attendance").content(mark(b,"PRESENT"))).andExpect(status().isForbidden());
  call(otherUser,post(f.base()+"/lesson/classes").header("Idempotency-Key","staff-class").content(classBody)).andExpect(status().isForbidden());
  call(otherUser,post(f.base()+"/lesson/classes/"+c+"/schedules").header("Idempotency-Key","staff-schedule").content(scheduleBody(2,"12:00"))).andExpect(status().isForbidden());
  call(otherUser,post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","staff-booking").content(body(l,o))).andExpect(status().isForbidden());
  call(otherUser,post(f.base()+"/lesson/cycles/"+l.cycle()+"/adjustments").header("Idempotency-Key","staff-adjust").content("{\"amount\":1,\"reason\":\"No\"}")).andExpect(status().isForbidden());
 }
}
