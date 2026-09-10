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
