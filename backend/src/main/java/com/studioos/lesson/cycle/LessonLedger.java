package com.studioos.lesson.cycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.*;
@Repository
public class LessonLedger {
 private final JdbcTemplate jdbc;public LessonLedger(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Entry(UUID id,String eventType,int amount,String reason,String referenceType,UUID referenceId,Instant createdAt){}
 public long balance(UUID studio,UUID cycle){return jdbc.queryForObject("select coalesce(sum(amount),0) from pass_usage_ledger where studio_id=? and enrollment_cycle_id=?",Long.class,studio,cycle);}
 public long reserved(UUID studio,UUID cycle){return jdbc.queryForObject("select count(*) from pass_entitlement_reservations where studio_id=? and enrollment_cycle_id=? and status='ACTIVE'",Long.class,studio,cycle);}
 public boolean outstanding(UUID studio,UUID cycle){return reserved(studio,cycle)>0||jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id where d.studio_id=? and d.enrollment_cycle_id=? and b.status in ('PENDING','CONFIRMED')",Long.class,studio,cycle)>0;}
 public void append(UUID studio,UUID cycle,String event,int amount,String reason,String type,UUID ref,UUID user){jdbc.update("insert into pass_usage_ledger(id,studio_id,enrollment_cycle_id,event_type,amount,reason,reference_type,reference_id,created_by_user_id,created_at) values (?,?,?,?,?,?,?,?,?,now())",UUID.randomUUID(),studio,cycle,event,amount,reason,type,ref,user);}
 public boolean consumed(UUID studio,UUID cycle){
  return jdbc.queryForObject("select count(*) from pass_usage_ledger l where l.studio_id=? and l.enrollment_cycle_id=? and l.amount<0 and not (l.event_type='BOOKING_DEDUCTION' and exists(select 1 from pass_usage_ledger r where r.studio_id=l.studio_id and r.enrollment_cycle_id=l.enrollment_cycle_id and r.event_type='CANCEL_RESTORE' and r.reference_type=l.reference_type and r.reference_id=l.reference_id))",Long.class,studio,cycle)>0;
 }
 public List<Entry> history(UUID studio,UUID cycle,int page,int size){return jdbc.query("select * from pass_usage_ledger where studio_id=? and enrollment_cycle_id=? order by created_at desc,id limit ? offset ?",(r,n)->new Entry(r.getObject("id",UUID.class),r.getString("event_type"),r.getInt("amount"),r.getString("reason"),r.getString("reference_type"),r.getObject("reference_id",UUID.class),r.getTimestamp("created_at").toInstant()),studio,cycle,size,(long)page*size);}
}
