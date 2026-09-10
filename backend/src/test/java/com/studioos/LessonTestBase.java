package com.studioos;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
abstract class LessonTestBase extends OperationalTestBase {
 record Lesson(UUID customer,UUID enrollment,UUID cycle,UUID payment){}
 void group(Fixture f){jdbc.update("update studio_capabilities set enabled=true where studio_id=? and capability in ('GROUP_CLASS','ATTENDANCE')",f.studio());}
 Lesson lesson(Fixture f,String trigger,int count,UUID clazz) throws Exception{
  UUID c=UUID.fromString(json(call(f.user(),post(f.base()+"/customers").content(customerBody("Member "+UUID.randomUUID(),"01012345678")))).get("id").asText());
  String p=json(call(f.user(),post(f.base()+"/lesson/pass-products").content("{\"name\":\"Flexible pass\",\"productType\":\"COUNT_BASED\",\"totalCount\":"+count+",\"validityDays\":30,\"validityStartRule\":\"FIRST_USE\",\"price\":\"10000\",\"deductionTrigger\":\""+trigger+"\",\"active\":true}")).andExpect(status().isCreated())).get("id").asText();
  String e=json(call(f.user(),post(f.base()+"/lesson/enrollments").header("Idempotency-Key",UUID.randomUUID()).content("{\"customerId\":\""+c+"\",\"kind\":\""+(clazz==null?"PRIVATE":"GROUP")+"\",\"classId\":"+(clazz==null?"null":"\""+clazz+"\"")+"}")).andExpect(status().isCreated())).get("id").asText();
  var cycle=json(call(f.user(),post(f.base()+"/lesson/enrollments/"+e+"/renew").header("Idempotency-Key",UUID.randomUUID()).content("{\"passProductId\":\""+p+"\",\"method\":\"CASH\"}")).andExpect(status().isCreated()));
  return new Lesson(c,UUID.fromString(e),UUID.fromString(cycle.get("id").asText()),UUID.fromString(cycle.get("paymentId").asText()));
 }
 Instant future(int days,int hour){return LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(days).atTime(hour,0).atZone(ZoneId.of("Asia/Seoul")).toInstant();}
 String privateBody(Fixture f,Lesson l,Instant start){return "{\"customerId\":\""+l.customer()+"\",\"enrollmentCycleId\":\""+l.cycle()+"\",\"staffId\":\""+f.staff()+"\",\"startAt\":\""+start+"\",\"endAt\":\""+start.plusSeconds(3600)+"\"}";}
 UUID book(Fixture f,Lesson l,Instant start) throws Exception{return UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/private-bookings").header("Idempotency-Key",UUID.randomUUID()).content(privateBody(f,l,start))).andExpect(status().isCreated())).get("id").asText());}
 long balance(Fixture f,Lesson l){return jdbc.queryForObject("select coalesce(sum(amount),0) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=?",Long.class,f.studio(),l.cycle());}
 String path(Fixture f,UUID b,String cmd){return f.base()+"/bookings/"+b+"/"+cmd;}
 UUID clazz(Fixture f) throws Exception{group(f);return UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/classes").header("Idempotency-Key",UUID.randomUUID()).content("{\"name\":\"Group\",\"capacity\":2,\"instructorStaffId\":\""+f.staff()+"\",\"active\":true}")).andExpect(status().isCreated())).get("id").asText());}
 UUID occurrence(Fixture f,UUID c) throws Exception{call(f.user(),post(f.base()+"/lesson/classes/"+c+"/schedules").header("Idempotency-Key",UUID.randomUUID()).content("{\"weekday\":1,\"startTime\":\"10:00\",\"durationMinutes\":60,\"active\":true}")).andExpect(status().isCreated());return jdbc.queryForObject("select id from class_occurrences where studio_id=? and class_id=? order by start_at limit 1",UUID.class,f.studio(),c);}
 String body(Lesson l,UUID o){return "{\"customerId\":\""+l.customer()+"\",\"enrollmentCycleId\":\""+l.cycle()+"\",\"classOccurrenceId\":\""+o+"\"}";}
 UUID groupBook(Fixture f,Lesson l,UUID o) throws Exception{return UUID.fromString(json(call(f.user(),post(f.base()+"/lesson/group-bookings").header("Idempotency-Key",UUID.randomUUID()).content(body(l,o))).andExpect(status().isCreated())).get("id").asText());}
 String mark(UUID b,String status){return "{\"entries\":[{\"bookingId\":\""+b+"\",\"status\":\""+status+"\"}]}";}
}
