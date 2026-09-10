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
 @Test void concurrentCancellationRestoresOnce() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));Callable<Integer> task=()->call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","same")).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsOnly(200);assertThat(balance(f,l)).isEqualTo(2);
 }
 @Test void concurrentAttendanceWithDifferentKeysIsSemanticallyIdempotent() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);var l=lesson(f,"ATTENDANCE_PRESENT",2,c);var b=groupBook(f,l,o);Callable<Integer> task=()->call(f.user(),put(f.base()+"/lesson/occurrences/"+o+"/attendance").header("Idempotency-Key",UUID.randomUUID()).content(mark(b,"PRESENT"))).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsOnly(200);assertThat(balance(f,l)).isEqualTo(1);
 }
 @Test void concurrentRenewalCannotCreateTwoScheduledPayments() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);UUID p=jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());
  Callable<Integer> task=()->call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key",UUID.randomUUID()).content("{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}")).andReturn().getResponse().getStatus();assertThat(race(task,task)).containsExactlyInAnyOrder(201,409);
  assertThat(jdbc.queryForObject("select count(*) from payments where studio_id=?",Integer.class,f.studio())).isEqualTo(2);
 }
 @Test void concurrentExpiryActivatesOnlyOneSuccessorAndPreservesUnusedCredits() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);UUID p=jdbc.queryForObject("select pass_product_id from enrollment_cycles where studio_id=? and id=?",UUID.class,f.studio(),l.cycle());
  call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/renew").header("Idempotency-Key","next").content("{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated());
  jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),l.cycle());
  Callable<Integer> task=()->{maintenance.expire(f.studio());return 1;};race(task,task);
  assertThat(jdbc.queryForObject("select count(*) from enrollment_cycles where studio_id=? and status='ACTIVE'",Integer.class,f.studio())).isEqualTo(1);assertThat(balance(f,l)).isEqualTo(2);
 }
 @Test void concurrentGenerationIsDuplicateFree() throws Exception{
  var f=fixture();var c=clazz(f);var o=occurrence(f,c);UUID s=jdbc.queryForObject("select class_schedule_id from class_occurrences where studio_id=? and id=?",UUID.class,f.studio(),o);int before=jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio());
  Callable<Integer> task=()->{maintenance.generate(f.studio(),s);return 1;};race(task,task);assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio())).isEqualTo(before);
 }
}
