package com.studioos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.studioos.lesson.LessonMaintenance;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LessonIntegrityIntegrationTest extends LessonTestBase {
 @Autowired LessonMaintenance maintenance;
 @Test void expiryWaitsForOutstandingThenPreservesHistoricalRestoration() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));
  jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),l.cycle());maintenance.expire(f.studio());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("ACTIVE");
  call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","expired").content(privateBody(f,l,future(3,10)))).andExpect(status().isConflict());
  call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel")).andExpect(status().isOk());assertThat(balance(f,l)).isEqualTo(2);
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("EXPIRED");
 }
 @Test void ledgerFailureRollsBackBookingCompletionAndReservation() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(1,10));
  jdbc.execute("create function reject_lesson_effect() returns trigger language plpgsql as $$ begin raise exception 'test rollback'; end $$");jdbc.execute("create trigger reject_lesson_effect before insert on pass_usage_ledger for each row execute function reject_lesson_effect()");
  try{call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","retry")).andExpect(status().is5xxServerError());}finally{jdbc.execute("drop trigger reject_lesson_effect on pass_usage_ledger");jdbc.execute("drop function reject_lesson_effect()");}
  assertThat(jdbc.queryForObject("select status from bookings where studio_id=? and id=?",String.class,f.studio(),b)).isEqualTo("CONFIRMED");
  assertThat(jdbc.queryForObject("select status from pass_entitlement_reservations where studio_id=? and booking_id=?",String.class,f.studio(),b)).isEqualTo("ACTIVE");
  call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","retry")).andExpect(status().isOk());assertThat(balance(f,l)).isEqualTo(1);
 }
 @Test void restoredUnusedRefundAllowedButConsumedUsageRejected() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));
  call(f.user(),post(f.base()+"/payments/"+l.payment()+"/refund").header("Idempotency-Key","outstanding").content("{\"reason\":\"request\"}")).andExpect(status().isConflict());
  call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel")).andExpect(status().isOk());
  call(f.user(),post(f.base()+"/payments/"+l.payment()+"/refund").header("Idempotency-Key","unused").content("{\"reason\":\"request\"}")).andExpect(status().isOk());
  var used=lesson(f,"LESSON_COMPLETED",2,null);var usedBooking=book(f,used,future(3,10));call(f.user(),post(path(f,usedBooking,"complete")).header("Idempotency-Key","done")).andExpect(status().isOk());
  call(f.user(),post(f.base()+"/payments/"+used.payment()+"/refund").header("Idempotency-Key","used").content("{\"reason\":\"request\"}")).andExpect(status().isConflict());
 }
 @Test void lessonResourcesRejectForeignTenantAndBeautyAndForeignOccurrence() throws Exception{
  var a=fixture();var c=clazz(a);var o=occurrence(a,c);var l=lesson(a,"ATTENDANCE_PRESENT",2,c);var b=groupBook(a,l,o);var other=fixture();
  for(String url:List.of(a.base()+"/lesson/enrollments/"+l.enrollment(),a.base()+"/lesson/enrollments/"+l.enrollment()+"/cycles",a.base()+"/lesson/cycles/"+l.cycle()+"/ledger",a.base()+"/lesson/classes",a.base()+"/lesson/occurrences/"+o+"/attendance"))call(other.user(),get(url)).andExpect(status().isForbidden());
  group(other);var foreignClass=clazz(other);var foreignOccurrence=occurrence(other,foreignClass);
  call(a.user(),post(a.base()+"/lesson/group-bookings").header("Idempotency-Key","foreign").content(body(l,foreignOccurrence))).andExpect(status().isNotFound());
  call(other.user(),put(other.base()+"/lesson/occurrences/"+foreignOccurrence+"/attendance").header("Idempotency-Key","foreign").content(mark(b,"PRESENT"))).andExpect(status().isNotFound());
  jdbc.update("update studios set business_category='BEAUTY',business_type='NAIL' where id=?",other.studio());
  call(other.user(),get(other.base()+"/lesson/classes")).andExpect(status().isForbidden());call(other.user(),get(other.base()+"/lesson/enrollments")).andExpect(status().isForbidden());
 }
}
