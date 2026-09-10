package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentIntegrationTest extends OperationalTestBase {
    String body(UUID customer,String status){return "{\"customerId\":\""+customer+"\",\"amount\":\"10000\",\"method\":\"CARD\",\"status\":\""+status+"\",\"currency\":\"KRW\",\"referenceType\":\"OTHER\"}";}
    String paid(Fixture f,UUID customer) throws Exception {return json(call(f.user(),post(f.base()+"/payments").header("Idempotency-Key",UUID.randomUUID()).content(body(customer,"PAID"))).andExpect(status().isCreated())).get("id").asText();}
    @Test void manualPaymentCreateListDetailAndFullRefundReplay() throws Exception {
        var f=fixture();var customer=customer(f);String id=paid(f,customer),path=f.base()+"/payments/"+id;
        call(f.user(),get(path)).andExpect(jsonPath("$.amount").value("10000")).andExpect(jsonPath("$.paidAt").isNotEmpty());
        call(f.user(),get(f.base()+"/payments?customerId="+customer)).andExpect(jsonPath("$.totalElements").value(1));
        String key=UUID.randomUUID().toString();String refund="{\"reason\":\"Requested refund\"}";
        var result=call(f.user(),post(path+"/refund").header("Idempotency-Key",key).content(refund)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUNDED")).andReturn().getResponse().getContentAsString();
        call(f.user(),post(path+"/refund").header("Idempotency-Key",key).content(refund)).andExpect(status().isOk()).andExpect(content().json(result));
        call(f.user(),post(path+"/refund").header("Idempotency-Key",UUID.randomUUID()).content(refund)).andExpect(status().isConflict());
        call(f.user(),post(path+"/refund").header("Idempotency-Key",key).content("{\"reason\":\"different\"}")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from payment_refunds where studio_id=? and payment_id=?",Integer.class,f.studio(),UUID.fromString(id))).isEqualTo(1);
    }
    @Test void pendingConfirmationAndCancellationEnforceTransitions() throws Exception {
        var f=fixture();var customer=customer(f);
        String id=json(call(f.user(),post(f.base()+"/payments").header("Idempotency-Key","pending").content(body(customer,"PENDING"))).andExpect(status().isCreated())).get("id").asText();
        String path=f.base()+"/payments/"+id;
        call(f.user(),post(path+"/refund").header("Idempotency-Key","refund").content("{\"reason\":\"No payment\"}")).andExpect(status().isConflict());
        call(f.user(),post(path+"/confirm").header("Idempotency-Key","confirm").content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        call(f.user(),post(path+"/cancel").header("Idempotency-Key","cancel")).andExpect(status().isConflict());
        id=json(call(f.user(),post(f.base()+"/payments").header("Idempotency-Key","pending2").content(body(customer,"PENDING")))).get("id").asText();
        path=f.base()+"/payments/"+id;
        call(f.user(),post(path+"/cancel").header("Idempotency-Key","cancel2")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        call(f.user(),post(path+"/confirm").header("Idempotency-Key","confirm2").content("{}")).andExpect(status().isConflict());
    }
    @Test void invalidAmountsMethodsReferencesAndArchivedCustomersAreRejected() throws Exception {
        var f=fixture();var customer=customer(f);String good=body(customer,"PAID");
        for(String bad:List.of(good.replace("10000","0"),good.replace("10000","1.5"),good.replace("10000","9223372036854775808"),good.replace("CARD","INVALID"),good.replace("OTHER","LESSON_CYCLE"),good.replace("PAID","REFUNDED")))
            call(f.user(),post(f.base()+"/payments").header("Idempotency-Key",UUID.randomUUID()).content(bad)).andExpect(status().isBadRequest());
        call(f.user(),post(f.base()+"/customers/"+customer+"/archive")).andExpect(status().isOk());
        call(f.user(),post(f.base()+"/payments").header("Idempotency-Key","archived").content(good)).andExpect(status().isConflict());
    }
    @Test void tenantReferencesRolesAndReplayReauthorization() throws Exception {
        var a=fixture();var b=fixture();var customer=customer(b);String id=paid(b,customer),path=b.base()+"/payments/"+id;
        call(a.user(),post(a.base()+"/payments").header("Idempotency-Key","foreign").content(body(customer,"PAID"))).andExpect(status().isNotFound());
        call(a.user(),get(path)).andExpect(status().isForbidden());call(a.user(),get(b.base()+"/payments")).andExpect(status().isForbidden());
        call(a.user(),post(path+"/refund").header("Idempotency-Key","refund").content("{\"reason\":\"Denied\"}")).andExpect(status().isForbidden());
        var manager=member(b,"MANAGER");var staff=member(b,"STAFF");
        call(manager,get(path)).andExpect(status().isOk());call(manager,post(path+"/refund").header("Idempotency-Key","refund").content("{\"reason\":\"Denied\"}")).andExpect(status().isForbidden());
        call(staff,get(path)).andExpect(status().isForbidden());call(staff,post(b.base()+"/payments").header("Idempotency-Key","staff").content(body(customer,"PAID"))).andExpect(status().isForbidden());
        jdbc.update("update studio_memberships set status='INACTIVE' where studio_id=? and user_id=?",b.studio(),b.user());
        call(b.user(),get(path)).andExpect(status().isForbidden());
    }
    @Test void refundFailureRollsBackPaymentAndIdempotency() throws Exception {
        var f=fixture();String id=paid(f,customer(f)),path=f.base()+"/payments/"+id;
        jdbc.execute("create function reject_refund() returns trigger language plpgsql as $$ begin raise exception 'test rollback'; end $$");
        jdbc.execute("create trigger reject_refund before insert on payment_refunds for each row execute function reject_refund()");
        try{call(f.user(),post(path+"/refund").header("Idempotency-Key","retry").content("{\"reason\":\"Test\"}")).andExpect(status().is5xxServerError());}
        finally{jdbc.execute("drop trigger reject_refund on payment_refunds");jdbc.execute("drop function reject_refund()");}
        call(f.user(),get(path)).andExpect(jsonPath("$.status").value("PAID"));
        call(f.user(),post(path+"/refund").header("Idempotency-Key","retry").content("{\"reason\":\"Test\"}")).andExpect(status().isOk());
    }
    @Test void concurrentRefundsCreateOnlyOneRefund() throws Exception {
        var f=fixture();String id=paid(f,customer(f));var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> action=()->{start.await();return call(f.user(),post(f.base()+"/payments/"+id+"/refund").header("Idempotency-Key",UUID.randomUUID()).content("{\"reason\":\"Concurrent\"}")).andReturn().getResponse().getStatus();};
            var one=pool.submit(action);var two=pool.submit(action);start.countDown();assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from payment_refunds where studio_id=?",Integer.class,f.studio())).isEqualTo(1);
    }
}
