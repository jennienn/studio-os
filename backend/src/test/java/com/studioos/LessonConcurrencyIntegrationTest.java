package com.studioos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.studioos.lesson.LessonMaintenance;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LessonConcurrencyIntegrationTest extends LessonTestBase {
 @Autowired LessonMaintenance maintenance;
 List<Integer> race(Callable<Integer> a,Callable<Integer> b) throws Exception{var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->{gate.await();return a.call();});var two=pool.submit(()->{gate.await();return b.call();});gate.countDown();return List.of(one.get(30,TimeUnit.SECONDS),two.get(30,TimeUnit.SECONDS));}}
 @Test void concurrentFinalEntitlementAtDifferentTimesHasOneWinner() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",1,null);
  assertThat(race(()->call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","one").content(privateBody(f,l,future(1,10)))).andReturn().getResponse().getStatus(),()->call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","two").content(privateBody(f,l,future(2,10)))).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201,409);
  assertThat(jdbc.queryForObject("select count(*) from pass_entitlement_reservations where studio_id=? and status='ACTIVE'",Integer.class,f.studio())).isEqualTo(1);
 }
 @Test void concurrentFinalSeatHasOneWinner() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);groupBook(f,lesson(f,"ATTENDANCE_PRESENT",3,c),o);var a=lesson(f,"ATTENDANCE_PRESENT",3,c);var b=lesson(f,"ATTENDANCE_PRESENT",3,c);
  assertThat(race(()->call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","one").content(body(a,o))).andReturn().getResponse().getStatus(),()->call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","two").content(body(b,o))).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201,409);
  assertThat(jdbc.queryForObject("select count(*) from bookings where studio_id=? and status='CONFIRMED'",Integer.class,f.studio())).isEqualTo(2);
 }
 @Test void concurrentCompletionWritesOneDeduction() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(1,10));Callable<Integer> task=()->call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","same")).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsOnly(200);assertThat(balance(f,l)).isEqualTo(1);
 }
 @Test void concurrentPrivateCompletionWithDifferentKeysHasOneSemanticWinner() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(1,10));
  assertThat(race(()->call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","complete-one")).andReturn().getResponse().getStatus(),()->call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","complete-two")).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
  assertThat(jdbc.queryForObject("select status from bookings where studio_id=? and id=?",String.class,f.studio(),b)).isEqualTo("COMPLETED");assertThat(balance(f,l)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=? and event_type='LESSON_COMPLETED'",Integer.class,f.studio(),l.cycle())).isEqualTo(1);
 }
 @Test void concurrentCancellationRestoresOnce() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));Callable<Integer> task=()->call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","same")).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsOnly(200);assertThat(balance(f,l)).isEqualTo(2);
 }
 @Test void repeatedAndConcurrentDifferentKeyCancellationRestoresExactlyOnce() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));
  assertThat(race(()->call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel-one")).andReturn().getResponse().getStatus(),()->call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel-two")).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
  call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel-three")).andExpect(status().isConflict());
  assertThat(balance(f,l)).isEqualTo(2);assertThat(jdbc.queryForObject("select count(*) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=? and event_type='CANCEL_RESTORE'",Integer.class,f.studio(),l.cycle())).isEqualTo(1);
 }
 @Test void concurrentAttendanceWithDifferentKeysIsSemanticallyIdempotent() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,l,o);Callable<Integer> task=()->call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key",UUID.randomUUID()).content(mark(b,"PRESENT"))).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsOnly(200);assertThat(balance(f,l)).isEqualTo(1);
 }
 @Test void concurrentRenewalCannotCreateTwoScheduledPayments() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);UUID p=jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());
  Callable<Integer> task=()->call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key",UUID.randomUUID()).content("{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}")).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsExactlyInAnyOrder(201,409);
  assertThat(jdbc.queryForObject("select count(*) from payments where studio_id=?",Integer.class,f.studio())).isEqualTo(2);
 }
 @Test void concurrentDuplicateCustomerGroupBookingHasOneWinner() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);
  assertThat(race(()->call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","group-one").content(body(l,o))).andReturn().getResponse().getStatus(),()->call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","group-two").content(body(l,o))).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201,409);
  assertThat(jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id where d.studio_id=? and d.class_occurrence_id=? and b.status='CONFIRMED'",Integer.class,f.studio(),o)).isEqualTo(1);
  assertThat(jdbc.queryForObject("select count(*) from pass_entitlement_reservations where studio_id=? and enrollment_cycle_id=? and status='ACTIVE'",Integer.class,f.studio(),l.cycle())).isEqualTo(1);
 }
 @Test void scheduleUpdateAndGroupBookingCannotLeaveBookingPointingAtDeletedOccurrence() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);UUID s=jdbc.queryForObject("select class_schedule_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),o);
  var statuses=race(()->call(f.user(),put(f.base()+"/lesson/classes/"+c+"/schedules/"+s).header("Idempotency-Key","schedule-race").content("{\"weekday\":1,\"startTime\":\"11:00\",\"durationMinutes\":60,\"active\":true}")).andReturn().getResponse().getStatus(),()->call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key","booking-race").content(body(l,o))).andReturn().getResponse().getStatus());
  assertThat(statuses.getFirst()).isEqualTo(200);assertThat(statuses.get(1)).isIn(201,404);
  assertThat(jdbc.queryForObject("select count(*) from lesson_booking_details d left join class_occurrences o on o.studio_id=d.studio_id and o.id=d.class_occurrence_id where d.studio_id=? and d.lesson_mode='GROUP' and o.id is null",Integer.class,f.studio())).isZero();
 }
 @Test void enrollmentTerminationAndBookingCannotCreateBookingOnEndedEnrollment() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);
  var statuses=race(()->call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/end").header("Idempotency-Key","end-race")).andReturn().getResponse().getStatus(),()->call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","book-race").content(privateBody(f,l,future(2,10)))).andReturn().getResponse().getStatus());
  assertThat(statuses.getFirst()).isIn(200,409);assertThat(statuses.get(1)).isIn(201,409);assertThat(statuses.stream().filter(s->s==409).count()).isEqualTo(1);String enrollment=jdbc.queryForObject("select status from enrollments where studio_id=? and id=?",String.class,f.studio(),l.enrollment());
  assertThat(jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id join enrollment_cycles c on c.studio_id=d.studio_id and c.id=d.enrollment_cycle_id join enrollments e on e.studio_id=c.studio_id and e.id=c.enrollment_id where d.studio_id=? and b.status='CONFIRMED' and e.status='ENDED'",Integer.class,f.studio())).isZero();
  if(enrollment.equals("ENDED"))assertThat(jdbc.queryForObject("select count(*) from bookings where studio_id=? and booking_kind='LESSON_PRIVATE'",Integer.class,f.studio())).isZero();
 }
 @Test void concurrentExpiryActivatesOnlyOneSuccessorAndPreservesUnusedCredits() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);UUID p=jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());
  UUID successor=UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key","next").content("{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated())).get("id").asText());
  jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),l.cycle());
  Callable<Integer> task=()->{maintenance.expire(f.studio());return 1;};race(task,task);
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("EXPIRED");assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),successor)).isEqualTo("ACTIVE");
  assertThat(jdbc.queryForObject("select count(*) from enrollment_cycles where studio_id=? and status='ACTIVE'",Integer.class,f.studio())).isEqualTo(1);assertThat(balance(f,l)).isEqualTo(2);
 }
 @Test void concurrentGenerationIsDuplicateFree() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);UUID s=jdbc.queryForObject("select class_schedule_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),o);int before=jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio());
  jdbc.update("delete from class_occurrences where studio_id=? and id=(select id from class_occurrences where studio_id=? and class_schedule_id=? order by occurrence_date desc limit 1)",f.studio(),f.studio(),s);assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio())).isEqualTo(before-1);
  Callable<Integer> task=()->{maintenance.generate(f.studio(),s);return 1;};race(task,task);assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio())).isEqualTo(before);
  assertThat(jdbc.queryForObject("select count(*)-count(distinct occurrence_date) from class_occurrences where studio_id=? and class_schedule_id=?",Integer.class,f.studio(),s)).isZero();
 }
 @Test void refundRacesWithBookingAndCompletionRemainAtomic() throws Exception{
  var bookingFixture=fixture();var available=lesson(bookingFixture,"LESSON_COMPLETED",2,null);
  var bookingRace=race(()->call(bookingFixture.user(),post(bookingFixture.base()+"/payments/"+available.payment()+"/refund").header("Idempotency-Key","refund-v-book").content("{\"reason\":\"Race\"}")).andReturn().getResponse().getStatus(),()->call(bookingFixture.user(),post(bookingFixture.base()+"/lesson/private-bookings").header("Idempotency-Key","book-v-refund").content(privateBody(bookingFixture,available,future(2,10)))).andReturn().getResponse().getStatus());
  assertThat(bookingRace.getFirst()).isIn(200,409);assertThat(bookingRace.get(1)).isIn(201,409);assertThat(bookingRace.stream().filter(s->s==409).count()).isEqualTo(1);String payment=jdbc.queryForObject("select status from payments where studio_id=? and id=?",String.class,bookingFixture.studio(),available.payment());int confirmed=jdbc.queryForObject("select count(*) from bookings where studio_id=? and status='CONFIRMED'",Integer.class,bookingFixture.studio());
  if(payment.equals("REFUNDED"))assertThat(confirmed).isZero();else {assertThat(payment).isEqualTo("PAID");assertThat(confirmed).isEqualTo(1);}
  var completionFixture=fixture();var used=lesson(completionFixture,"LESSON_COMPLETED",2,null);var b=book(completionFixture,used,future(2,10));
  assertThat(race(()->call(completionFixture.user(),post(completionFixture.base()+"/payments/"+used.payment()+"/refund").header("Idempotency-Key","refund-v-complete").content("{\"reason\":\"Race\"}")).andReturn().getResponse().getStatus(),()->call(completionFixture.user(),post(path(completionFixture,b,"complete")).header("Idempotency-Key","complete-v-refund")).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
  assertThat(jdbc.queryForObject("select status from payments where studio_id=? and id=?",String.class,completionFixture.studio(),used.payment())).isEqualTo("PAID");assertThat(balance(completionFixture,used)).isEqualTo(1);
 }
}
