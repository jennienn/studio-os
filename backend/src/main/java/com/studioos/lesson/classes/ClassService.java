package com.studioos.lesson.classes;
import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import com.studioos.booking.*;
import com.studioos.security.AuthorizedStudioContext;
import com.studioos.studio.*;
import com.studioos.staff.StaffRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;

@Service
public class ClassService {
 public record Edit(@NotBlank @Size(max=100) String name,@Positive int capacity,UUID instructorStaffId,boolean active){}
 public record ScheduleEdit(@Min(1) @Max(7) int weekday,@NotNull LocalTime startTime,@Min(1) @Max(1439) int durationMinutes,boolean active){}
 public record ClassView(UUID id,String name,int capacity,UUID instructorStaffId,boolean active){}
 public record Schedule(UUID id,UUID classId,int weekday,LocalTime startTime,int durationMinutes,boolean active){}
 public record Occurrence(UUID id,UUID classId,String className,UUID classScheduleId,Instant startAt,Instant endAt,UUID instructorStaffId,int capacitySnapshot,String status,long bookedCount){}
 private final JdbcTemplate jdbc;private final LessonAccess access;private final StudioRepository studios;private final StaffRepository staff;
 private final BookingAvailability availability;private final BookingRepository bookings;private final OccurrenceOccupancy occupancy;private final IdempotencyService idem;
 public ClassService(JdbcTemplate jdbc,LessonAccess access,StudioRepository studios,StaffRepository staff,BookingAvailability availability,BookingRepository bookings,OccurrenceOccupancy occupancy,IdempotencyService idem){this.jdbc=jdbc;this.access=access;this.studios=studios;this.staff=staff;this.availability=availability;this.bookings=bookings;this.occupancy=occupancy;this.idem=idem;}
 private final RowMapper<ClassView> classMapper=(r,n)->new ClassView(r.getObject("id",UUID.class),r.getString("name"),r.getInt("capacity"),r.getObject("instructor_staff_id",UUID.class),r.getBoolean("active"));
 private final RowMapper<Schedule> scheduleMapper=(r,n)->new Schedule(r.getObject("id",UUID.class),r.getObject("class_id",UUID.class),r.getInt("weekday"),r.getObject("start_time",LocalTime.class),r.getInt("duration_minutes"),r.getBoolean("active"));
 private final RowMapper<Occurrence> occurrenceMapper=(r,n)->new Occurrence(r.getObject("id",UUID.class),r.getObject("class_id",UUID.class),r.getString("class_name"),r.getObject("class_schedule_id",UUID.class),r.getTimestamp("start_at").toInstant(),r.getTimestamp("end_at").toInstant(),r.getObject("instructor_staff_id",UUID.class),r.getInt("capacity_snapshot"),r.getString("status"),r.getLong("booked_count"));
 private static final String OCC="select o.*,c.name class_name,(select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id where d.studio_id=o.studio_id and d.class_occurrence_id=o.id and b.status in ('PENDING','CONFIRMED')) booked_count from class_occurrences o join classes c on c.studio_id=o.studio_id and c.id=o.class_id ";
 public ClassView find(UUID studio,UUID id){return jdbc.query("select * from classes where studio_id=? and id=?",classMapper,studio,id).stream().findFirst().orElseThrow(ClassService::missing);}
 @Transactional(readOnly=true) public List<ClassView> list(UUID studio,int page,int size){var a=access.member(studio);PageResult.validate(page,size);UUID own=ownStaff(a);return jdbc.query("select * from classes where studio_id=? and (?::uuid is null or instructor_staff_id=?) order by name,id limit ? offset ?",classMapper,studio,own,own,size,(long)page*size);}
 @Transactional public ClassView save(UUID studio,UUID id,String key,Edit body){var a=access.lock(studio);access.capability(studio,"GROUP_CLASS");return idem.execute(a,"SAVE_CLASS",key,Map.of("id",id==null?"new":id.toString(),"body",body),id==null?201:200,ClassView.class,()->{
  if(body.instructorStaffId()!=null)availability.staff(studio,body.instructorStaffId());
  UUID target=id==null?UUID.randomUUID():find(studio,id).id();
  if(id==null)jdbc.update("insert into classes(id,studio_id,name,capacity,instructor_staff_id,active,created_at) values (?,?,?,?,?,?,now())",target,studio,body.name().trim(),body.capacity(),body.instructorStaffId(),body.active());
  else jdbc.update("update classes set name=?,capacity=?,instructor_staff_id=?,active=? where studio_id=? and id=?",body.name().trim(),body.capacity(),body.instructorStaffId(),body.active(),studio,id);
  return find(studio,target);
 });}
 @Transactional(readOnly=true) public List<Schedule> schedules(UUID studio,UUID clazz){access.manager(studio);find(studio,clazz);return jdbc.query("select * from class_schedules where studio_id=? and class_id=? order by weekday,start_time,id",scheduleMapper,studio,clazz);}
 @Transactional public Schedule schedule(UUID studio,UUID clazz,UUID id,String key,ScheduleEdit body){var a=access.lock(studio);access.capability(studio,"GROUP_CLASS");return idem.execute(a,"SAVE_CLASS_SCHEDULE",key,Map.of("class",clazz,"id",id==null?"new":id.toString(),"body",body),id==null?201:200,Schedule.class,()->{
  if(!find(studio,clazz).active())conflict("비활성 반입니다.");UUID target=id==null?UUID.randomUUID():id;
  if(id==null)jdbc.update("insert into class_schedules(id,studio_id,class_id,weekday,start_time,duration_minutes,active) values (?,?,?,?,?,?,?)",target,studio,clazz,body.weekday(),body.startTime(),body.durationMinutes(),body.active());
  else if(jdbc.update("update class_schedules set weekday=?,start_time=?,duration_minutes=?,active=? where studio_id=? and class_id=? and id=?",body.weekday(),body.startTime(),body.durationMinutes(),body.active(),studio,clazz,id)!=1)throw missing();
  // An occurrence with any historical booking is never changed, including cancellations.
  jdbc.update("delete from class_occurrences o where o.studio_id=? and o.class_schedule_id=? and o.start_at>? and not exists(select 1 from lesson_booking_details d where d.studio_id=o.studio_id and d.class_occurrence_id=o.id)",studio,target,Timestamp.from(Instant.now()));
  generate(studio,target);return jdbc.query("select * from class_schedules where studio_id=? and id=?",scheduleMapper,studio,target).getFirst();
 });}
 /** The caller holds the Studio lock; jobs and operator commands share this protocol. */
 public void generate(UUID studio,UUID schedule){
  var s=jdbc.query("select * from class_schedules where studio_id=? and id=?",scheduleMapper,studio,schedule).stream().findFirst().orElseThrow(ClassService::missing);
  var c=find(studio,s.classId());if(!s.active()||!c.active())return;
  var tenant=studios.findById(studio).orElseThrow();var zone=ZoneId.of(tenant.timezone);var today=LocalDate.now(zone);
  for(int i=0;i<90;i++){
   var date=today.plusDays(i);if(date.getDayOfWeek().getValue()!=s.weekday())continue;
   if(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=? and class_schedule_id=? and occurrence_date=?",Integer.class,studio,schedule,date)>0)continue;
   var local=date.atTime(s.startTime());var offsets=zone.getRules().getValidOffsets(local);
   if(offsets.size()!=1)conflict("일광절약시간 전환으로 수업 시각이 모호합니다. 일정을 확인해 주세요.");
   var start=local.toInstant(offsets.getFirst());if(!start.isAfter(Instant.now()))continue;var end=start.plusSeconds(s.durationMinutes()*60L);
   availability.hours(tenant,start,end);
   if(c.instructorStaffId()!=null){availability.staff(studio,c.instructorStaffId());occupancy.check(studio,c.instructorStaffId(),start,end);
    if(bookings.conflicts(studio,c.instructorStaffId(),start,end,null)>0)conflict("강사의 개인 예약과 시간이 겹칩니다.");}
   jdbc.update("insert into class_occurrences(id,studio_id,class_id,class_schedule_id,occurrence_date,start_at,end_at,instructor_staff_id,capacity_snapshot,status) values (?,?,?,?,?,?,?,?,?,'SCHEDULED')",UUID.randomUUID(),studio,c.id(),schedule,date,Timestamp.from(start),Timestamp.from(end),c.instructorStaffId(),c.capacity());
  }
 }
 @Transactional(readOnly=true) public List<Occurrence> occurrences(UUID studio,Instant from,Instant to,UUID clazz,int page,int size){var a=access.member(studio);BookingAvailability.interval(from,to);PageResult.validate(page,size);UUID own=ownStaff(a);
  return jdbc.query(OCC+"where o.studio_id=? and o.start_at<? and o.end_at>? and (?::uuid is null or o.class_id=?) and (?::uuid is null or o.instructor_staff_id=?) order by o.start_at,o.id limit ? offset ?",occurrenceMapper,studio,Timestamp.from(to),Timestamp.from(from),clazz,clazz,own,own,size,(long)page*size);
 }
 public Occurrence occurrence(UUID studio,UUID id){return jdbc.query(OCC+"where o.studio_id=? and o.id=?",occurrenceMapper,studio,id).stream().findFirst().orElseThrow(ClassService::missing);}
 public void assigned(AuthorizedStudioContext actor,Occurrence o){UUID own=ownStaff(actor);if(own!=null&&!own.equals(o.instructorStaffId()))throw new ApiException(403,"INSTRUCTOR_REQUIRED","담당 강사만 처리할 수 있습니다.");}
 public void lockOccurrence(UUID studio,UUID id){if(jdbc.queryForList("select id from class_occurrences where studio_id=? and id=? for update",UUID.class,studio,id).isEmpty())throw missing();}
 @Transactional public Occurrence complete(UUID studio,UUID id,String key){var actor=access.lock(studio);return idem.execute(actor,"COMPLETE_OCCURRENCE",key,id,200,Occurrence.class,()->{lockOccurrence(studio,id);var o=occurrence(studio,id);if(o.status().equals("CANCELLED")||o.bookedCount()>0)conflict("모든 예약을 먼저 처리해 주세요.");jdbc.update("update class_occurrences set status='COMPLETED' where studio_id=? and id=?",studio,id);return occurrence(studio,id);});}
 private UUID ownStaff(AuthorizedStudioContext a){if(a.membershipRole()!=StudioMembership.Role.STAFF)return null;return staff.findByStudioIdAndUserId(a.authorizedStudioId(),a.authenticatedUserId()).filter(s->s.active).orElseThrow(()->new ApiException(403,"STAFF_REQUIRED","담당자 정보가 필요합니다.")).id;}
 private static ApiException missing(){return new ApiException(404,"CLASS_NOT_FOUND","수업 정보를 찾을 수 없습니다.");}
 private static void conflict(String message){throw new ApiException(409,"CLASS_CONFLICT",message);}
}
