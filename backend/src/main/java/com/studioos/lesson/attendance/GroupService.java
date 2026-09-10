package com.studioos.lesson.attendance;
import com.studioos.booking.*;
import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import com.studioos.lesson.booking.LessonEntitlement;
import com.studioos.lesson.cycle.*;
import com.studioos.lesson.classes.ClassService;
import com.studioos.security.AuthorizedStudioContext;
import com.studioos.studio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import static com.studioos.lesson.cycle.CycleService.conflict;
@Service
public class GroupService implements BookingSpecialization {
 public record Create(@NotNull UUID customerId,@NotNull UUID enrollmentCycleId,@NotNull UUID classOccurrenceId){}
 public record Created(UUID id,UUID classOccurrenceId,String status){}
 public enum Status{PRESENT,ABSENT,CANCELLED}
 public record Mark(@NotNull UUID bookingId,@NotNull Status status){}
 public record Bulk(@NotEmpty @Size(max=100) List<@Valid Mark> entries){}
 public record Result(int count){}
 public record Row(UUID bookingId,UUID customerId,String customerName,String customerPhone,UUID enrollmentCycleId,String bookingStatus,String attendanceStatus,Long remaining,Long available,boolean paymentNeeded){}
 private final LessonAccess access;private final StudioRepository studios;private final BookingRepository bookings;private final BookingAvailability availability;private final ClassService classes;
 private final LessonEntitlement entitlement;private final CycleService cycles;private final LessonLedger ledger;private final JdbcTemplate jdbc;private final IdempotencyService idem;
 public GroupService(LessonAccess access,StudioRepository studios,BookingRepository bookings,BookingAvailability availability,ClassService classes,LessonEntitlement entitlement,CycleService cycles,LessonLedger ledger,JdbcTemplate jdbc,IdempotencyService idem){this.access=access;this.studios=studios;this.bookings=bookings;this.availability=availability;this.classes=classes;this.entitlement=entitlement;this.cycles=cycles;this.ledger=ledger;this.jdbc=jdbc;this.idem=idem;}
 @Transactional public Created create(UUID studio,String key,Create body){var actor=access.lock(studio);access.capability(studio,"GROUP_CLASS");return idem.execute(actor,"CREATE_GROUP_BOOKING",key,body,201,Created.class,()->{
  classes.lockOccurrence(studio,body.classOccurrenceId());var o=classes.occurrence(studio,body.classOccurrenceId());
  if(!classes.find(studio,o.classId()).active()||!o.status().equals("SCHEDULED")||!o.startAt().isAfter(Instant.now()))conflict("예약 가능한 미래 수업이 아닙니다.");
  if(o.bookedCount()>=o.capacitySnapshot())conflict("수업 정원이 찼습니다.");
  if(jdbc.queryForObject("select count(*) from attendance where studio_id=? and class_occurrence_id=? and customer_id=?",Integer.class,studio,o.id(),body.customerId())>0)conflict("출석이 확정된 회원은 재예약할 수 없습니다.");
  if(jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id where d.studio_id=? and d.class_occurrence_id=? and b.customer_id=? and b.status<>'CANCELLED'",Integer.class,studio,o.id(),body.customerId())>0)conflict("이미 예약된 회원입니다.");
  var c=entitlement.eligible(studio,body.enrollmentCycleId(),body.customerId(),"GROUP",o.classId(),o.startAt(),null);
  availability.hours(studios.findById(studio).orElseThrow(),o.startAt(),o.endAt());
  var b=new Booking(studio,body.customerId(),o.instructorStaffId(),"LESSON_GROUP",o.startAt(),o.endAt(),null);b.manualEntry=false;b=bookings.save(b);studios.flush();
  jdbc.update("insert into lesson_booking_details(booking_id,studio_id,enrollment_cycle_id,class_occurrence_id,lesson_mode) values (?,?,?,?,'GROUP')",b.id,studio,c.id,o.id());
  entitlement.confirm(c,b,actor.authenticatedUserId());studios.flush();return new Created(b.id,o.id(),b.status);
 });}
 @Transactional(readOnly=true) public List<Row> attendance(UUID studio,UUID occurrence,int page,int size){var actor=access.member(studio);classes.assigned(actor,classes.occurrence(studio,occurrence));PageResult.validate(page,size);
  return jdbc.query("select b.id,b.customer_id,c.name,c.phone,d.enrollment_cycle_id,b.status booking_status,a.status attendance_status from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id join customers c on c.studio_id=b.studio_id and c.id=b.customer_id left join attendance a on a.studio_id=b.studio_id and a.booking_id=b.id where d.studio_id=? and d.class_occurrence_id=? and not exists(select 1 from lesson_booking_details newer join bookings nb on nb.studio_id=newer.studio_id and nb.id=newer.booking_id where newer.studio_id=d.studio_id and newer.class_occurrence_id=d.class_occurrence_id and nb.customer_id=b.customer_id and (nb.created_at,nb.id)>(b.created_at,b.id)) order by c.name,b.id limit ? offset ?",(r,n)->{
   UUID cycle=r.getObject("enrollment_cycle_id",UUID.class);var c=cycles.find(studio,cycle);Long bal=c.productTypeSnapshot.equals("COUNT_BASED")?ledger.balance(studio,cycle):null;
   boolean due=jdbc.queryForObject("select count(*) from lesson_cycle_payments cp join payments p on p.studio_id=cp.studio_id and p.id=cp.payment_id where cp.studio_id=? and cp.enrollment_cycle_id=? and p.status='PENDING'",Integer.class,studio,cycle)>0;
   return new Row(r.getObject("id",UUID.class),r.getObject("customer_id",UUID.class),r.getString("name"),r.getString("phone"),cycle,r.getString("booking_status"),r.getString("attendance_status"),bal,bal==null?null:bal-ledger.reserved(studio,cycle),due);
  },studio,occurrence,size,(long)page*size);
 }
 @Transactional public Result mark(UUID studio,UUID occurrence,String key,Bulk body){var actor=access.member(studio);studios.lockById(studio).orElseThrow();classes.lockOccurrence(studio,occurrence);classes.assigned(actor,classes.occurrence(studio,occurrence));access.capability(studio,"ATTENDANCE");
  return idem.execute(actor,"FINALIZE_ATTENDANCE",key,Map.of("occurrence",occurrence,"body",body),200,Result.class,()->{
   if(body.entries().stream().map(Mark::bookingId).distinct().count()!=body.entries().size())conflict("중복된 출석 요청입니다.");
   for(var entry:body.entries()){
    var b=bookings.findByStudioIdAndId(studio,entry.bookingId()).orElseThrow(()->new ApiException(404,"BOOKING_NOT_FOUND","예약을 찾을 수 없습니다."));
    var ids=jdbc.queryForList("select enrollment_cycle_id from lesson_booking_details where studio_id=? and booking_id=? and class_occurrence_id=? and lesson_mode='GROUP'",UUID.class,studio,b.id,occurrence);if(ids.size()!=1)conflict("해당 수업의 예약이 아닙니다.");
    var previous=jdbc.queryForList("select status from attendance where studio_id=? and class_occurrence_id=? and customer_id=?",String.class,studio,occurrence,b.customerId);
    if(!previous.isEmpty()){if(!previous.getFirst().equals(entry.status().name()))conflict("확정된 출석은 변경할 수 없습니다.");continue;}
    UUID attendance=UUID.randomUUID();var c=cycles.find(studio,ids.getFirst());
    String command=switch(entry.status()){case PRESENT->"COMPLETE";case ABSENT->"NO_SHOW";case CANCELLED->"CANCEL";};
    boolean alreadyCancelled=b.status.equals("CANCELLED")&&entry.status()==Status.CANCELLED;
    if(!alreadyCancelled){BookingTransitions.apply(b,command);studios.flush();}
    jdbc.update("insert into attendance(id,studio_id,class_occurrence_id,customer_id,enrollment_cycle_id,booking_id,status,recorded_at,recorded_by_user_id) values (?,?,?,?,?,?,?,now(),?)",attendance,studio,occurrence,b.customerId,c.id,b.id,entry.status().name(),actor.authenticatedUserId());
    if(!alreadyCancelled)entitlement.resolve(c,b,entry.status()==Status.PRESENT?"PRESENT":command,actor.authenticatedUserId(),attendance);
   }
   studios.flush();return new Result(body.entries().size());
  });
 }
 public boolean supports(Booking b){return !b.manualEntry&&b.bookingKind.equals("LESSON_GROUP");}
 public void authorize(Booking b,AuthorizedStudioContext actor,String command){access.manager(b.studioId);if(command.equals("CONFIRM"))conflict("그룹 예약은 생성 시 확정됩니다.");
  UUID cycle=jdbc.queryForObject("select enrollment_cycle_id from lesson_booking_details where studio_id=? and booking_id=?",UUID.class,b.studioId,b.id);
  if(!command.equals("CANCEL")&&"ATTENDANCE_PRESENT".equals(cycles.find(b.studioId,cycle).deductionTriggerSnapshot))conflict("출석 차감 수업은 출석 화면에서 처리해 주세요.");}
 public void afterTransition(Booking b,AuthorizedStudioContext actor,String command){UUID cycle=jdbc.queryForObject("select enrollment_cycle_id from lesson_booking_details where studio_id=? and booking_id=?",UUID.class,b.studioId,b.id);entitlement.resolve(cycles.find(b.studioId,cycle),b,command,actor.authenticatedUserId(),null);}
 public void validateEdit(Booking b,BookingDto.Edit body){conflict("그룹 예약 시간은 수업 회차에 연결되어 있습니다.");}
}
