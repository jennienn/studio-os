package com.studioos;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingIntegrationTest extends OperationalTestBase {
    Instant at(int hour){return LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(2).atTime(hour,0).atZone(ZoneId.of("Asia/Seoul")).toInstant();}
    ObjectNode body(Fixture f,UUID customer,int hour){return mapper.createObjectNode().put("customerId",customer.toString()).put("staffId",f.staff().toString()).put("bookingKind","LESSON_PRIVATE").put("startAt",at(hour).toString()).put("endAt",at(hour+1).toString()).put("note","Internal booking note");}
    String create(Fixture f,UUID customer,int hour) throws Exception {return json(call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",UUID.randomUUID()).content(body(f,customer,hour).toString())).andExpect(status().isCreated())).get("id").asText();}
    UUID staff(Fixture f,UUID user){UUID id=UUID.randomUUID();jdbc.update("insert into staff(id,studio_id,user_id,name,active,created_at,updated_at) values (?,?,?,'Second staff',true,now(),now())",id,f.studio(),user);return id;}
    ObjectNode block(Fixture f,String scope,int hour){var b=mapper.createObjectNode().put("scopeType",scope).put("startAt",at(hour).toString()).put("endAt",at(hour+1).toString()).put("reason","Unavailable");if(scope.equals("STAFF"))b.put("staffId",f.staff().toString());return b;}
    @Test void createsConfirmedManualBookingAndQueriesAvailabilityAndDailyList() throws Exception {
        var f=fixture();var customer=customer(f);String id=create(f,customer,10);
        call(f.user(),get(f.base()+"/bookings/"+id)).andExpect(jsonPath("$.status").value("CONFIRMED")).andExpect(jsonPath("$.manualEntry").value(true));
        call(f.user(),get(f.base()+"/bookings").param("from",at(8).toString()).param("to",at(22).toString())).andExpect(jsonPath("$.totalElements").value(1));
        call(f.user(),get(f.base()+"/availability").param("staffId",f.staff().toString()).param("startAt",at(10).toString()).param("endAt",at(11).toString())).andExpect(jsonPath("$.available").value(false));
        call(f.user(),get(f.base()+"/availability").param("staffId",f.staff().toString()).param("startAt",at(11).toString()).param("endAt",at(12).toString())).andExpect(jsonPath("$.available").value(true));
    }
    @Test void rejectsInvalidTimeHoursKindsPastAndArchivedCustomers() throws Exception {
        var f=fixture();var c=customer(f);
        for(var b:List.of(body(f,c,10).put("endAt",at(10).toString()),body(f,c,10).put("endAt",at(9).toString()),body(f,c,10).put("startAt",Instant.now().minusSeconds(10).toString()),body(f,c,10).put("bookingKind","BEAUTY_SERVICE")))
            call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",UUID.randomUUID()).content(b.toString())).andExpect(status().isBadRequest());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","closed").content(body(f,c,7).toString())).andExpect(status().isConflict());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","group").content(body(f,c,10).put("bookingKind","LESSON_GROUP").toString())).andExpect(status().isConflict());
        call(f.user(),post(f.base()+"/customers/"+c+"/archive")).andExpect(status().isOk());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","archived").content(body(f,c,10).toString())).andExpect(status().isConflict());
    }
    @Test void studioTimezoneAndOperatorPolicyExemptionAreApplied() throws Exception {
        var f=fixture();var c=customer(f);
        // 09:07 Studio time, beyond the configured 30-day public window, remains valid for the operator.
        var start=LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(45).atTime(9,7).atZone(ZoneId.of("Asia/Seoul")).toInstant();
        var b=body(f,c,10).put("startAt",start.toString()).put("endAt",start.plusSeconds(1200).toString());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","operator").content(b.toString())).andExpect(status().isCreated());
        jdbc.update("update business_hours set closed=true,open_time=null,close_time=null where studio_id=?",f.studio());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","closed").content(body(f,c,10).toString())).andExpect(status().isConflict());
    }
    @Test void sameStaffOverlapRejectsDifferentStaffAndAdjacentIntervalsAllow() throws Exception {
        var f=fixture();var c=customer(f);create(f,c,10);
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","overlap").content(body(f,c,10).toString())).andExpect(status().isConflict());
        create(f,c,11);
        var other=staff(f,null);
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","other").content(body(f,c,10).put("staffId",other.toString()).toString())).andExpect(status().isCreated());
    }
    @Test void terminalStatusesReleaseOccupancyButPendingStillHoldsIt() throws Exception {
        var f=fixture();var c=customer(f);
        for(String command:List.of("cancel","complete","no-show")){
            String id=create(f,c,10);
            call(f.user(),post(f.base()+"/bookings/"+id+"/"+command).header("Idempotency-Key",UUID.randomUUID())).andExpect(status().isOk());
            call(f.user(),post(f.base()+"/bookings/"+id+"/complete").header("Idempotency-Key",UUID.randomUUID())).andExpect(status().isConflict());
        }
        String pending=create(f,c,10);jdbc.update("update bookings set status='PENDING' where studio_id=? and id=?",f.studio(),UUID.fromString(pending));
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","pending-blocks").content(body(f,c,10).toString())).andExpect(status().isConflict());
        call(f.user(),post(f.base()+"/bookings/"+pending+"/complete").header("Idempotency-Key","pending-complete")).andExpect(status().isConflict());
        call(f.user(),post(f.base()+"/bookings/"+pending+"/confirm").header("Idempotency-Key","confirm")).andExpect(status().isOk());
    }
    @Test void blocksRespectScopeAndCannotOverwriteOccupyingBookings() throws Exception {
        var f=fixture();var c=customer(f);String id=create(f,c,10);
        call(f.user(),post(f.base()+"/booking-blocks").header("Idempotency-Key","conflict").content(block(f,"STUDIO",10).toString())).andExpect(status().isConflict());
        jdbc.update("update bookings set status='PENDING' where studio_id=? and id=?",f.studio(),UUID.fromString(id));
        call(f.user(),post(f.base()+"/booking-blocks").header("Idempotency-Key","pending").content(block(f,"STAFF",10).toString())).andExpect(status().isConflict());
        String blockId=json(call(f.user(),post(f.base()+"/booking-blocks").header("Idempotency-Key","staff").content(block(f,"STAFF",12).toString())).andExpect(status().isCreated())).get("id").asText();
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","blocked").content(body(f,c,12).toString())).andExpect(status().isConflict());
        var other=staff(f,null);call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","not-blocked").content(body(f,c,12).put("staffId",other.toString()).toString())).andExpect(status().isCreated());
        call(f.user(),delete(f.base()+"/booking-blocks/"+blockId).header("Idempotency-Key","remove")).andExpect(status().isOk());create(f,c,12);
        call(f.user(),post(f.base()+"/booking-blocks").header("Idempotency-Key","all").content(block(f,"STUDIO",14).toString())).andExpect(status().isCreated());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","all-blocked").content(body(f,c,14).put("staffId",other.toString()).toString())).andExpect(status().isConflict());
    }
    @Test void rescheduleRechecksOccupancyAndKeepsOldIntervalOnFailure() throws Exception {
        var f=fixture();var c=customer(f);String a=create(f,c,10);create(f,c,12);var edit=body(f,c,12);edit.remove(List.of("customerId","bookingKind"));
        call(f.user(),put(f.base()+"/bookings/"+a).header("Idempotency-Key","reschedule").content(edit.toString())).andExpect(status().isConflict());
        call(f.user(),get(f.base()+"/bookings/"+a)).andExpect(jsonPath("$.startAt").value(at(10).toString()));
        edit.put("startAt",at(14).toString()).put("endAt",at(15).toString());
        call(f.user(),put(f.base()+"/bookings/"+a).header("Idempotency-Key","reschedule").content(edit.toString())).andExpect(status().isOk());create(f,c,10);
    }
    @Test void crossTenantReferencesAndStaffReadBoundary() throws Exception {
        var a=fixture();var b=fixture();var c=customer(a);var foreign=customer(b);
        call(a.user(),post(a.base()+"/bookings").header("Idempotency-Key","foreign-c").content(body(a,foreign,10).toString())).andExpect(status().isNotFound());
        call(a.user(),post(a.base()+"/bookings").header("Idempotency-Key","foreign-s").content(body(a,c,10).put("staffId",b.staff().toString()).toString())).andExpect(status().isNotFound());
        String own=create(a,c,10),other=create(b,foreign,10);
        call(a.user(),get(b.base()+"/bookings/"+other)).andExpect(status().isForbidden());
        call(a.user(),post(b.base()+"/bookings/"+other+"/cancel").header("Idempotency-Key","foreign")).andExpect(status().isForbidden());
        call(a.user(),get(a.base()+"/bookings/"+other)).andExpect(status().isNotFound());
        UUID user=member(a,"STAFF"),staffId=staff(a,user);
        String assigned=json(call(a.user(),post(a.base()+"/bookings").header("Idempotency-Key","assigned").content(body(a,c,12).put("staffId",staffId.toString()).toString()))).get("id").asText();
        call(user,get(a.base()+"/bookings/"+assigned)).andExpect(status().isOk()).andExpect(jsonPath("$.note").isEmpty()).andExpect(jsonPath("$.memo").doesNotExist());
        call(user,get(a.base()+"/bookings/"+own)).andExpect(status().isForbidden());
        call(user,get(a.base()+"/bookings").param("from",at(8).toString()).param("to",at(22).toString())).andExpect(jsonPath("$.totalElements").value(1));
        call(user,post(a.base()+"/bookings/"+assigned+"/complete").header("Idempotency-Key","staff")).andExpect(status().isForbidden());
        call(user,get(a.base()+"/booking-blocks").param("from",at(8).toString()).param("to",at(22).toString())).andExpect(status().isForbidden());
    }
    @Test void idempotentRetryUsesNamespaceAndRechecksAuthorization() throws Exception {
        var f=fixture();var c=customer(f);var b=body(f,c,10);String key="same";
        String response=call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",key).content(b.toString())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",key).content(b.toString())).andExpect(status().isCreated()).andExpect(content().json(response));
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",key).content(body(f,c,12).toString())).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_MISMATCH"));
        var manager=member(f,"MANAGER");call(manager,post(f.base()+"/bookings").header("Idempotency-Key",key).content(body(f,c,12).toString())).andExpect(status().isCreated());
        jdbc.update("update studio_memberships set status='INACTIVE' where studio_id=? and user_id=?",f.studio(),f.user());
        call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",key).content(b.toString())).andExpect(status().isForbidden());
    }
    @Test void blockTenantScopeForeignStaffAndInvalidScopeAreRejected() throws Exception {
        var a=fixture();var b=fixture();
        String id=json(call(b.user(),post(b.base()+"/booking-blocks").header("Idempotency-Key","block").content(block(b,"STUDIO",10).toString()))).get("id").asText();
        call(a.user(),get(b.base()+"/booking-blocks").param("from",at(8).toString()).param("to",at(22).toString())).andExpect(status().isForbidden());
        call(a.user(),delete(b.base()+"/booking-blocks/"+id).header("Idempotency-Key","delete")).andExpect(status().isForbidden());
        call(a.user(),delete(a.base()+"/booking-blocks/"+id).header("Idempotency-Key","delete")).andExpect(status().isNotFound());
        call(a.user(),post(a.base()+"/booking-blocks").header("Idempotency-Key","foreign-staff").content(block(b,"STAFF",12).toString())).andExpect(status().isNotFound());
        call(a.user(),post(a.base()+"/booking-blocks").header("Idempotency-Key","invalid").content(block(a,"STUDIO",12).put("staffId",a.staff().toString()).toString())).andExpect(status().isBadRequest());
        call(a.user(),post(a.base()+"/booking-blocks").header("Idempotency-Key","time").content(block(a,"STUDIO",12).put("endAt",at(11).toString()).toString())).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from booking_blocks where studio_id=?",Integer.class,b.studio())).isEqualTo(1);
    }
    @Test void archiveRejectsUnresolvedBookingsAndLaterPreservesHistory() throws Exception {
        var f=fixture();var c=customer(f);String id=create(f,c,10);
        call(f.user(),post(f.base()+"/customers/"+c+"/archive")).andExpect(status().isConflict());
        call(f.user(),post(f.base()+"/bookings/"+id+"/cancel").header("Idempotency-Key","cancel")).andExpect(status().isOk());
        call(f.user(),post(f.base()+"/customers/"+c+"/archive")).andExpect(status().isOk());
        call(f.user(),get(f.base()+"/bookings/"+id)).andExpect(status().isOk());
    }
    @Test void concurrentConflictingBookingsHaveOneWinner() throws Exception {
        var f=fixture();var c=customer(f);var b=body(f,c,10);
        var results=race(()->call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key",UUID.randomUUID()).content(b.toString())).andReturn().getResponse().getStatus());
        assertThat(results).containsExactlyInAnyOrder(201,409);
        assertThat(jdbc.queryForObject("select count(*) from bookings where studio_id=? and status='CONFIRMED'",Integer.class,f.studio())).isEqualTo(1);
    }
    @Test void concurrentSameKeyReturnsOnePersistedBooking() throws Exception {
        var f=fixture();var c=customer(f);var b=body(f,c,10);
        assertThat(race(()->call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","retry").content(b.toString())).andReturn().getResponse().getStatus())).containsOnly(201);
        assertThat(jdbc.queryForObject("select count(*) from bookings where studio_id=?",Integer.class,f.studio())).isEqualTo(1);
    }
    @Test void concurrentBlockAndBookingCannotBothCommit() throws Exception {
        var f=fixture();var c=customer(f);
        assertThat(race(()->call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","booking").content(body(f,c,10).toString())).andReturn().getResponse().getStatus(),
            ()->call(f.user(),post(f.base()+"/booking-blocks").header("Idempotency-Key","block").content(block(f,"STUDIO",10).toString())).andReturn().getResponse().getStatus())).containsExactlyInAnyOrder(201,409);
        long bookings=jdbc.queryForObject("select count(*) from bookings where studio_id=?",Long.class,f.studio());
        long blocks=jdbc.queryForObject("select count(*) from booking_blocks where studio_id=?",Long.class,f.studio());assertThat(bookings+blocks).isEqualTo(1);
    }
    @Test void concurrentArchiveAndBookingKeepCustomerStateConsistent() throws Exception {
        var f=fixture();var c=customer(f);
        var results=race(()->call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","booking").content(body(f,c,10).toString())).andReturn().getResponse().getStatus(),
            ()->call(f.user(),post(f.base()+"/customers/"+c+"/archive")).andReturn().getResponse().getStatus());
        assertThat(results).contains(409);assertThat(results).anyMatch(s->s==200 || s==201);
        assertThat(jdbc.queryForObject("select count(*) from bookings b join customers c on c.studio_id=b.studio_id and c.id=b.customer_id where b.studio_id=? and c.status='ARCHIVED' and b.status='CONFIRMED'",Integer.class,f.studio())).isZero();
    }
    @Test void idempotencyFailureRollsBackBookingInsert() throws Exception {
        var f=fixture();var c=customer(f);
        jdbc.execute("create function reject_booking_result() returns trigger language plpgsql as $$ begin if NEW.operation='CREATE_BOOKING' then raise exception 'test rollback'; end if; return NEW; end $$");
        jdbc.execute("create trigger reject_booking_result before insert on idempotency_records for each row execute function reject_booking_result()");
        try{call(f.user(),post(f.base()+"/bookings").header("Idempotency-Key","retry").content(body(f,c,10).toString())).andExpect(status().is5xxServerError());}
        finally{jdbc.execute("drop trigger reject_booking_result on idempotency_records");jdbc.execute("drop function reject_booking_result()");}
        assertThat(jdbc.queryForObject("select count(*) from bookings where studio_id=?",Integer.class,f.studio())).isZero();create(f,c,10);
    }
    List<Integer> race(Callable<Integer> action) throws Exception{return race(action,action);}
    List<Integer> race(Callable<Integer> a,Callable<Integer> b) throws Exception{var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var one=pool.submit(()->{start.await();return a.call();});var two=pool.submit(()->{start.await();return b.call();});start.countDown();return List.of(one.get(),two.get());}}
}
