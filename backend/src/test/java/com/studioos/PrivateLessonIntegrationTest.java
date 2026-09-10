package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PrivateLessonIntegrationTest extends LessonTestBase {
 @Test void deferredReservesThenCompletesExactlyOnceAndInitializesDates() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",1,null);var b=book(f,l,future(1,10));assertThat(balance(f,l)).isEqualTo(1);
  call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","second").content(privateBody(f,l,future(2,10)))).andExpect(status().isConflict());
  for(int i=0;i<2;i++)call(f.user(),post(path(f,b,"complete")).header("Idempotency-Key","done")).andExpect(status().isOk());
  assertThat(balance(f,l)).isZero();assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),l.cycle())).isEqualTo("COMPLETED");
  assertThat(jdbc.queryForObject("select start_date is not null from enrollment_cycles where studio_id=? and id=?",Boolean.class,f.studio(),l.cycle())).isTrue();
 }
 @Test void confirmedDeductionCancellationRestoresOnceAndNoShowDoesNotRestore() throws Exception{
  var f=fixture();var l=lesson(f,"BOOKING_CONFIRMED",2,null);var b=book(f,l,future(2,10));assertThat(balance(f,l)).isEqualTo(1);
  for(int i=0;i<2;i++)call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel")).andExpect(status().isOk());
  assertThat(balance(f,l)).isEqualTo(2);b=book(f,l,future(3,10));call(f.user(),post(path(f,b,"no-show")).header("Idempotency-Key","no-show")).andExpect(status().isOk());assertThat(balance(f,l)).isEqualTo(1);
 }
 @Test void deferredNoShowReleaseAndTerminationGuard() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",1,null);var b=book(f,l,future(1,10));
  call(f.user(),post(f.base()+"/lesson/enrollments/"+l.enrollment()+"/end").header("Idempotency-Key","end")).andExpect(status().isConflict());
  call(f.user(),post(path(f,b,"no-show")).header("Idempotency-Key","no-show")).andExpect(status().isOk());assertThat(balance(f,l)).isEqualTo(1);
  assertThat(jdbc.queryForObject("select status from pass_entitlement_reservations where studio_id=? and booking_id=?",String.class,f.studio(),b)).isEqualTo("RELEASED");
 }
 @Test void provisionalWindowRechecksAllDatesAndMovesAfterCancellation() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",7,null);var b=book(f,l,future(20,10));book(f,l,future(40,10));
  call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key","early").content(privateBody(f,l,future(1,10)))).andExpect(status().isConflict());
  call(f.user(),post(path(f,b,"cancel")).header("Idempotency-Key","cancel")).andExpect(status().isOk());book(f,l,future(60,10));
 }
 @Test void tenantAndAssignedStaffCompletionAreEnforced() throws Exception{
  var f=fixture();var l=lesson(f,"LESSON_COMPLETED",2,null);var b=book(f,l,future(2,10));var other=fixture();
  call(other.user(),post(path(f,b,"complete")).header("Idempotency-Key","foreign")).andExpect(status().isForbidden());
  UUID staffUser=member(f,"STAFF");jdbc.update("insert into staff(id,studio_id,user_id,name,active,created_at,updated_at) values (?,?,?,'Instructor',true,now(),now())",UUID.randomUUID(),f.studio(),staffUser);
  call(staffUser,post(path(f,b,"complete")).header("Idempotency-Key","wrong")).andExpect(status().isForbidden());
  jdbc.update("update bookings set staff_id=(select id from staff where studio_id=? and user_id=?) where studio_id=? and id=?",f.studio(),staffUser,f.studio(),b);
  call(staffUser,post(path(f,b,"cancel")).header("Idempotency-Key","no")).andExpect(status().isForbidden());
  call(staffUser,post(path(f,b,"complete")).header("Idempotency-Key","own")).andExpect(status().isOk());
 }
}
