package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LessonCycleIntegrationTest extends OperationalTestBase {
 String product(Fixture f,String type,String trigger) throws Exception{
  var b=mapper.createObjectNode().put("name","7 sessions").put("productType",type).put("price","10000").put("validityStartRule",type.equals("TIME_BASED")?"PURCHASE_DATE":"FIRST_USE").put("active",true);
  if(type.equals("COUNT_BASED")){b.put("totalCount",7).put("validityDays",30).put("deductionTrigger",trigger);}else b.put("billingPeriod","MONTH");
  return json(call(f.user(),post(f.base()+"/lesson/pass-products").content(b.toString())).andExpect(status().isCreated())).get("id").asText();
 }
 String enrollment(Fixture f) throws Exception{return json(call(f.user(),post(f.base()+"/lesson/enrollments").header("Idempotency-Key",UUID.randomUUID()).content("{\"customerId\":\""+customer(f)+"\",\"kind\":\"PRIVATE\"}")).andExpect(status().isCreated())).get("id").asText();}
 String purchaseBody(String p){return "{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}";}
 com.fasterxml.jackson.databind.JsonNode buy(Fixture f,String e,String p,String key) throws Exception{return json(call(f.user(),post(f.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key",key).content(purchaseBody(p))).andExpect(status().isCreated()));}
 @Test void snapshotOpeningLedgerRenewalAndSuccessor() throws Exception{
  var f=fixture();String p=product(f,"COUNT_BASED","LESSON_COMPLETED"),e=enrollment(f);var first=buy(f,e,p,"one");assertThat(first.get("balance").asInt()).isEqualTo(7);assertThat(first.get("startDate").isNull()).isTrue();
  assertThat(buy(f,e,p,"one").get("id")).isEqualTo(first.get("id"));var second=buy(f,e,p,"two");assertThat(second.get("status").asText()).isEqualTo("SCHEDULED");
  call(f.user(),post(f.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key","three").content(purchaseBody(p))).andExpect(status().isConflict());
  jdbc.update("update pass_products set total_count=15,name='changed' where studio_id=? and id=?",f.studio(),UUID.fromString(p));
  call(f.user(),post(f.base()+"/lesson/cycles/"+first.get("id").asText()+"/adjustments").header("Idempotency-Key","adjust").content("{\"amount\":-7,\"reason\":\"Correction\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
  var list=json(call(f.user(),get(f.base()+"/lesson/enrollments/"+e+"/cycles")));
  assertThat(list.get(0).get("status").asText()).isEqualTo("ACTIVE");assertThat(list.get(0).get("balance").asInt()).isEqualTo(7);assertThat(list.get(1).get("purchasedCount").asInt()).isEqualTo(7);
 }
 @Test void timeAdvanceRenewalRejectedAndExpiryPreservesCount() throws Exception{
  var f=fixture();String e=enrollment(f),p=product(f,"TIME_BASED",null);var c=buy(f,e,p,"time");
  assertThat(c.get("validityDays").asInt()).isBetween(28,31);
  call(f.user(),post(f.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key","early").content(purchaseBody(p))).andExpect(status().isConflict());
  jdbc.update("update enrollment_cycles set start_date=current_date-40,valid_end_date=current_date-1 where studio_id=? and id=?",f.studio(),UUID.fromString(c.get("id").asText()));
  assertThat(buy(f,e,p,"after").get("status").asText()).isEqualTo("ACTIVE");
 }
 @Test void refundAndEndPreserveHistoryAndPermissions() throws Exception{
  var f=fixture();String e=enrollment(f),p=product(f,"COUNT_BASED","LESSON_COMPLETED");var c=buy(f,e,p,"buy");String cycle=c.get("id").asText();
  call(member(f,"MANAGER"),post(f.base()+"/lesson/cycles/"+cycle+"/adjustments").header("Idempotency-Key","no").content("{\"amount\":1,\"reason\":\"x\"}")).andExpect(status().isForbidden());
  call(f.user(),post(f.base()+"/payments/"+c.get("paymentId").asText()+"/refund").header("Idempotency-Key","refund").content("{\"reason\":\"Unused\"}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select status from enrollment_cycles where studio_id=? and id=?",String.class,f.studio(),UUID.fromString(cycle))).isEqualTo("CANCELLED");
  buy(f,e,p,"new");call(f.user(),post(f.base()+"/lesson/enrollments/"+e+"/end").header("Idempotency-Key","end")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENDED"));
  assertThat(jdbc.queryForObject("select sum(amount) from pass_usage_ledger where studio_id=?",Integer.class,f.studio())).isEqualTo(14);
 }
 @Test void incompatibleInactiveAndForeignProductsAreRejected() throws Exception{
  var a=fixture();String e=enrollment(a),p=product(a,"COUNT_BASED","ATTENDANCE_PRESENT");
  call(a.user(),post(a.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key","bad").content(purchaseBody(p))).andExpect(status().isBadRequest());
  var b=fixture();String foreign=product(b,"COUNT_BASED","LESSON_COMPLETED");
  call(a.user(),post(a.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key","foreign").content(purchaseBody(foreign))).andExpect(status().isNotFound());
  jdbc.update("update pass_products set active=false where studio_id=?",a.studio());
  call(a.user(),post(a.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key","inactive").content(purchaseBody(p))).andExpect(status().isConflict());
 }
}
