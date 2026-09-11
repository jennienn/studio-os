package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class GroupLessonIntegrationTest extends LessonTestBase {
 @Test void presentDeductsOnceAbsentReleasesAndFinalizationIsImmutable() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var a=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,a,o);String path=f.base()+"/lesson/occurrences/"+o+"/attendance";
  for(int i=0;i<2;i++)call(f.user(),put(path).header("Idempotency-Key","present"+i).content(mark(b,"PRESENT"))).andExpect(status().isOk());assertThat(balance(f,a)).isEqualTo(1);
  call(f.user(),put(path).header("Idempotency-Key","change").content(mark(b,"ABSENT"))).andExpect(status().isConflict());
  var other=lesson(f,"ATTENDANCE_PRESENT",2,c);var absent=groupBook(f,other,o);call(f.user(),put(path).header("Idempotency-Key","absent").content(mark(absent,"ABSENT"))).andExpect(status().isOk());assertThat(balance(f,other)).isEqualTo(2);
  assertThat(jdbc.queryForObject("select count(*) from pass_entitlement_reservations where studio_id=? and status='ACTIVE'",Integer.class,f.studio())).isZero();
 }
 @Test void capacityDuplicateAndCancelledRebooking() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var a=lesson(f,"BOOKING_CONFIRMED",3,c);var b=groupBook(f,a,o);
  call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","duplicate").content(body(a,o))).andExpect(status().isConflict());
  call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel")).andExpect(status().isOk());groupBook(f,a,o);
  groupBook(f,lesson(f,"BOOKING_CONFIRMED",3,c),o);
  var third=lesson(f,"BOOKING_CONFIRMED",3,c);call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","full").content(body(third,o))).andExpect(status().isConflict());
 }
 @Test void bulkFailureRollsBackEverythingAndOccurrenceRequiresSettlement() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var a=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,a,o);
  call(f.user(),post(f.base()+"/lesson/occurrences/"+o+"/complete").header("Idempotency-Key","early")).andExpect(status().isConflict());
  String bad="{\"entries\":[{\"bookingId\":\""+b+"\",\"status\":\"PRESENT\"},{\"bookingId\":\""+UUID.randomUUID()+"\",\"status\":\"ABSENT\"}]}";
  call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","bulk").content(bad)).andExpect(status().isNotFound());assertThat(balance(f,a)).isEqualTo(2);
  assertThat(jdbc.queryForObject("select count(*) from attendance where studio_id=?",Integer.class,f.studio())).isZero();
  assertThat(jdbc.queryForObject("select status from bookings where studio_id=? and id=?",String.class,f.studio(),b)).isEqualTo("CONFIRMED");
  assertThat(jdbc.queryForObject("select status from pass_entitlement_reservations where studio_id=? and booking_id=?",String.class,f.studio(),b)).isEqualTo("ACTIVE");
  assertThat(jdbc.queryForObject("select count(*) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=? and event_type<>'PURCHASE'",Integer.class,f.studio(),a.cycle())).isZero();
 }
 @Test void staleCancelledBookingCannotFinalizeAttendanceAfterRebooking() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);var old=groupBook(f,l,o);
  call(f.user(),post(path(f,old,"cancel")).header("Idempotency-Key","cancel-old")).andExpect(status().isOk());var current=groupBook(f,l,o);String attendance=f.base()+"/lesson/occurrences/"+o+"/attendance";
  jdbc.update("update bookings set created_at=timestamp with time zone '2030-01-01 00:00:00+00' where studio_id=? and id in (?,?)",f.studio(),old,current);
  assertThat(json(call(f.user(),get(attendance))).get(0).get("bookingId").asText()).isEqualTo(current.toString());
  call(f.user(),put(attendance).header("Idempotency-Key","stale").content(mark(old,"CANCELLED"))).andExpect(status().isConflict());
  assertThat(jdbc.queryForObject("select count(*) from attendance where studio_id=? and class_occurrence_id=?",Integer.class,f.studio(),o)).isZero();
  assertThat(jdbc.queryForObject("select status from bookings where studio_id=? and id=?",String.class,f.studio(),current)).isEqualTo("CONFIRMED");
  assertThat(jdbc.queryForObject("select status from pass_entitlement_reservations where studio_id=? and booking_id=?",String.class,f.studio(),current)).isEqualTo("ACTIVE");
  call(f.user(),put(attendance).header("Idempotency-Key","current").content(mark(current,"PRESENT"))).andExpect(status().isOk());
  call(f.user(),put(attendance).header("Idempotency-Key","current-again").content(mark(current,"PRESENT"))).andExpect(status().isOk());
  assertThat(balance(f,l)).isEqualTo(1);assertThat(jdbc.queryForObject("select status from pass_entitlement_reservations where studio_id=? and booking_id=?",String.class,f.studio(),current)).isEqualTo("CONSUMED");
 }
 @Test void bookedScheduleIsPreservedAndWrongInstructorCannotAttend() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var a=lesson(f,"BOOKING_CONFIRMED",2,c);var b=groupBook(f,a,o);
  UUID s=jdbc.queryForObject("select class_schedule_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),o);
  call(f.user(),put(f.base()+"/lesson/classes/"+c+"/schedules/"+s).header("Idempotency-Key","edit").content("{\"weekday\":1,\"startTime\":\"11:00\",\"durationMinutes\":60,\"active\":true}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=? and id=?",Integer.class,f.studio(),o)).isEqualTo(1);
  call(member(f,"STAFF"),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","staff").content(mark(b,"PRESENT"))).andExpect(status().isForbidden());
  call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","present").content(mark(b,"PRESENT"))).andExpect(status().isOk());assertThat(balance(f,a)).isEqualTo(1);
 }
}
