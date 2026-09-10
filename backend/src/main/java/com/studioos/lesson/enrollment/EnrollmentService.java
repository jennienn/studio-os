package com.studioos.lesson.enrollment;
import com.studioos.common.*;
import com.studioos.customer.CustomerRepository;
import com.studioos.lesson.LessonAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service
public class EnrollmentService {
 public enum Kind{PRIVATE,GROUP}
 public record Create(@NotNull UUID customerId,@NotNull Kind kind,UUID classId){}
 public record View(UUID id,UUID customerId,String kind,UUID classId,String status,Instant createdAt,Instant endedAt){}
 private final EnrollmentRepository enrollments;private final CustomerRepository customers;private final LessonAccess access;private final IdempotencyService idem;private final JdbcTemplate jdbc;
 public EnrollmentService(EnrollmentRepository enrollments,CustomerRepository customers,LessonAccess access,IdempotencyService idem,JdbcTemplate jdbc){this.enrollments=enrollments;this.customers=customers;this.access=access;this.idem=idem;this.jdbc=jdbc;}
 @Transactional(readOnly=true) public PageResult<View> list(UUID studio,UUID customer,int page,int size){access.manager(studio);PageResult.validate(page,size);return PageResult.from(enrollments.list(studio,customer,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(EnrollmentService::view));}
 @Transactional(readOnly=true) public View get(UUID studio,UUID id){access.manager(studio);return view(find(studio,id));}
 @Transactional public View create(UUID studio,String key,Create body){var actor=access.lock(studio);return idem.execute(actor,"CREATE_ENROLLMENT",key,body,201,View.class,()->{
  access.mode(studio,body.kind().name(),null);
  var c=customers.findByStudioIdAndId(studio,body.customerId()).orElseThrow(()->new ApiException(404,"CUSTOMER_NOT_FOUND","회원을 찾을 수 없습니다."));
  if(!c.status.equals("ACTIVE"))throw new ApiException(409,"CUSTOMER_ARCHIVED","보관 회원은 새 수강을 등록할 수 없습니다.");
  if(body.kind()==Kind.PRIVATE&&body.classId()!=null||body.kind()==Kind.GROUP&&body.classId()==null)throw new ApiException(400,"INVALID_ENROLLMENT","수강 형태와 반을 확인해 주세요.");
  if(body.classId()!=null&&jdbc.queryForObject("select count(*) from classes where studio_id=? and id=? and active",Integer.class,studio,body.classId())!=1)throw new ApiException(404,"CLASS_NOT_FOUND","활성 반을 찾을 수 없습니다.");
  var e=new Enrollment();e.id=UUID.randomUUID();e.studioId=studio;e.customerId=body.customerId();e.kind=body.kind().name();e.classId=body.classId();e.status="ACTIVE";e.createdAt=Instant.now();return view(enrollments.save(e));
 });}
 public Enrollment find(UUID studio,UUID id){return enrollments.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"ENROLLMENT_NOT_FOUND","수강을 찾을 수 없습니다."));}
 public static View view(Enrollment e){return new View(e.id,e.customerId,e.kind,e.classId,e.status,e.createdAt,e.endedAt);}
}
