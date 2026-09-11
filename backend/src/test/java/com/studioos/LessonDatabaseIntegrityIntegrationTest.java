package com.studioos;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LessonDatabaseIntegrityIntegrationTest extends LessonTestBase {
 @Test void reservationCannotReferenceABookingFromAnotherCycle() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(2,10));UUID product=jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());
  UUID scheduled=UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key","second-cycle").content("{\"passProductId\":\""+product+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated())).get("id").asText());
  assertThatThrownBy(()->jdbc.update("update pass_entitlement_reservations set enrollment_cycle_id=? where studio_id=? and booking_id=?",scheduled,f.studio(),b)).isInstanceOf(DataIntegrityViolationException.class);
  assertThat(jdbc.queryForObject("select enrollment_cycle_id from pass_entitlement_reservations where studio_id=? and booking_id=?",UUID.class,f.studio(),b)).isEqualTo(l.cycle());
 }
 @Test void occurrenceCannotMixClassAndScheduleReferences() throws Exception{
  var f=fixture();var first=clazz(f);var occurrence=occurrence(f,first);var second=clazz(f);
  assertThatThrownBy(()->jdbc.update("update class_occurrences set class_id=? where studio_id=? and id=?",second,f.studio(),occurrence)).isInstanceOf(DataIntegrityViolationException.class);
  assertThat(jdbc.queryForObject("select class_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),occurrence)).isEqualTo(first);
 }
 @Test void attendanceCannotMixBookingCustomerCycleOrOccurrenceContext() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var first=lesson(f,"ATTENDANCE_PRESENT",2,c);var firstBooking=groupBook(f,first,o);var second=lesson(f,"ATTENDANCE_PRESENT",2,c);var secondBooking=groupBook(f,second,o);
  call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key","first-attendance").content(mark(firstBooking,"PRESENT"))).andExpect(status().isOk());
  assertThatThrownBy(()->jdbc.update("update attendance set booking_id=? where studio_id=? and booking_id=?",secondBooking,f.studio(),firstBooking)).isInstanceOf(DataIntegrityViolationException.class);
  assertThat(jdbc.queryForObject("select booking_id from attendance where studio_id=? and customer_id=?",UUID.class,f.studio(),first.customer())).isEqualTo(firstBooking);
 }
 @Test void timeCycleResolvedMonthSnapshotRejectsInvalidPeriodOrDayCount() throws Exception{
  var f=fixture();String product=json(call(f.user(),post(f.base()+"/lesson/pass-products").content("{\"name\":\"Monthly\",\"productType\":\"TIME_BASED\",\"billingPeriod\":\"MONTH\",\"validityStartRule\":\"PURCHASE_DATE\",\"price\":\"10000\",\"active\":true}")).andExpect(status().isCreated())).get("id").asText();
  String enrollment=json(call(f.user(),post(f.base()+"/lesson/enrollments").header("Idempotency-Key","time-enrollment").content("{\"customerId\":\""+customer(f)+"\",\"kind\":\"PRIVATE\"}")).andExpect(status().isCreated())).get("id").asText();
  UUID cycle=UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/enrollments/"+enrollment+"/renew").header("Idempotency-Key","time-cycle").content("{\"passProductId\":\""+product+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated())).get("id").asText());
  assertThatThrownBy(()->jdbc.update("update enrollment_cycles set billing_period_snapshot='WEEK' where studio_id=? and id=?",f.studio(),cycle)).isInstanceOf(DataIntegrityViolationException.class);
  assertThatThrownBy(()->jdbc.update("update enrollment_cycles set validity_days_snapshot=27 where studio_id=? and id=?",f.studio(),cycle)).isInstanceOf(DataIntegrityViolationException.class);
 }
}
