package com.studioos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.studioos.lesson.LessonMaintenance;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LessonScheduleIntegrationTest extends OperationalTestBase {
 @Autowired LessonMaintenance maintenance;
 void group(Fixture f){jdbc.update("update studio_capabilities set enabled=true where studio_id=? and capability in ('GROUP_CLASS','ATTENDANCE')",f.studio());}
 String clazz(Fixture f) throws Exception {group(f);return json(call(f.user(),post(f.base()+"/lesson/classes").header("Idempotency-Key",UUID.randomUUID()).content("{\"name\":\"Group\",\"capacity\":2,\"instructorStaffId\":\""+f.staff()+"\",\"active\":true}")).andExpect(status().isCreated())).get("id").asText();}
 String scheduleBody(){return "{\"weekday\":1,\"startTime\":\"10:00\",\"durationMinutes\":60,\"active\":true}";}
 @Test void generatesRollingHorizonSnapshotsAndRejectsInstructorConflicts() throws Exception{
  var f=fixture();String c=clazz(f),path=f.base()+"/lesson/classes/"+c+"/schedules";
  var s=json(call(f.user(),post(path).header("Idempotency-Key","s").content(scheduleBody())).andExpect(status().isCreated()));
  int count=jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio());assertThat(count).isBetween(12,13);
  maintenance.generate(f.studio(),UUID.fromString(s.get("id").asText()));assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=?",Integer.class,f.studio())).isEqualTo(count);
  call(f.user(),post(path).header("Idempotency-Key","overlap").content(scheduleBody())).andExpect(status().isConflict());
  call(f.user(),put(f.base()+"/lesson/classes/"+c).header("Idempotency-Key","edit").content("{\"name\":\"Renamed\",\"capacity\":9,\"active\":true}")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select min(capacity_snapshot) from class_occurrences where studio_id=?",Integer.class,f.studio())).isEqualTo(2);
 }
 @Test void scheduleEditRegeneratesOnlyFutureUnbookedAndPreservesPast() throws Exception{
  var f=fixture();String c=clazz(f),path=f.base()+"/lesson/classes/"+c+"/schedules";
  String s=json(call(f.user(),post(path).header("Idempotency-Key","s").content(scheduleBody()))).get("id").asText();
  UUID old=jdbc.queryForObject("select id from class_occurrences where studio_id=? order by start_at limit 1",UUID.class,f.studio());
  jdbc.update("update class_occurrences set start_at=now()-interval '2 days',end_at=now()-interval '1 day' where studio_id=? and id=?",f.studio(),old);
  call(f.user(),put(path+"/"+s).header("Idempotency-Key","change").content(scheduleBody().replace("10:00","11:00"))).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=? and id=?",Integer.class,f.studio(),old)).isEqualTo(1);
 }
 @Test void validatesHoursScopeAndStaffPermissions() throws Exception{
  var a=fixture();String c=clazz(a),path=a.base()+"/lesson/classes/"+c+"/schedules";var b=fixture();
  call(a.user(),post(path).header("Idempotency-Key","hours").content(scheduleBody().replace("10:00","23:00"))).andExpect(status().isConflict());
  call(b.user(),get(path)).andExpect(status().isForbidden());
  call(member(a,"STAFF"),post(path).header("Idempotency-Key","staff").content(scheduleBody())).andExpect(status().isForbidden());
  call(a.user(),post(a.base()+"/lesson/classes").header("Idempotency-Key","foreign").content("{\"name\":\"Wrong\",\"capacity\":2,\"instructorStaffId\":\""+b.staff()+"\",\"active\":true}")).andExpect(status().isNotFound());
 }
}
