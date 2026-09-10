package com.studioos.lesson.cycle;

import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import com.studioos.lesson.enrollment.*;
import com.studioos.lesson.pass.*;
import com.studioos.customer.CustomerRepository;
import com.studioos.payment.*;
import com.studioos.studio.StudioRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CycleService implements PaymentRefundEffect {
 public record Purchase(@NotNull UUID passProductId,@NotNull PaymentDto.Method method){}
 public record Adjustment(@NotNull Integer amount,@NotBlank @Size(max=2000) String reason){}
 public record View(UUID id,UUID enrollmentId,String productName,String productType,Integer purchasedCount,Integer validityDays,
  String billingPeriod,String purchasePrice,String deductionTrigger,String validityStartRule,LocalDate startDate,LocalDate validEndDate,LocalDate paymentDate,
  String status,Long balance,Long reserved,Long available,UUID paymentId){}
 private final CycleRepository cycles;private final LessonLedger ledger;private final LessonAccess access;private final EnrollmentService enrollments;
 private final PassProductService products;private final CustomerRepository customers;private final PaymentRepository payments;private final StudioRepository studios;
 private final IdempotencyService idem;private final JdbcTemplate jdbc;
 public CycleService(CycleRepository cycles,LessonLedger ledger,LessonAccess access,EnrollmentService enrollments,PassProductService products,
  CustomerRepository customers,PaymentRepository payments,StudioRepository studios,IdempotencyService idem,JdbcTemplate jdbc){
  this.cycles=cycles;this.ledger=ledger;this.access=access;this.enrollments=enrollments;this.products=products;this.customers=customers;this.payments=payments;this.studios=studios;this.idem=idem;this.jdbc=jdbc;
 }
 @Transactional(readOnly=true) public List<View> list(UUID studio,UUID enrollment){access.manager(studio);enrollments.find(studio,enrollment);return cycles.findByStudioIdAndEnrollmentIdOrderByCreatedAtDesc(studio,enrollment).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public List<LessonLedger.Entry> history(UUID studio,UUID cycle,int page,int size){access.manager(studio);find(studio,cycle);PageResult.validate(page,size);return ledger.history(studio,cycle,page,size);}
 @Transactional public View purchase(UUID studio,UUID enrollment,String key,Purchase body){var actor=access.lock(studio);
  return idem.execute(actor,"RENEW_ENROLLMENT",key,Map.of("enrollment",enrollment,"body",body),201,View.class,()->{
   var e=enrollments.find(studio,enrollment);if(!e.status.equals("ACTIVE"))conflict("종료된 수강입니다.");
   if(!customers.findByStudioIdAndId(studio,e.customerId).orElseThrow().status.equals("ACTIVE"))conflict("보관 회원은 결제할 수 없습니다.");
   var p=products.find(studio,body.passProductId());if(!p.active)conflict("비활성 이용권입니다.");access.mode(studio,e.kind,p.deductionTrigger);
   if(e.classId!=null&&jdbc.queryForObject("select count(*) from classes where studio_id=? and id=? and active",Integer.class,studio,e.classId)!=1)conflict("비활성 반입니다.");
   settleEnrollment(studio,enrollment,today(studio));
   var active=cycles.findByStudioIdAndEnrollmentIdAndStatus(studio,enrollment,"ACTIVE");
   if(cycles.findByStudioIdAndEnrollmentIdAndStatus(studio,enrollment,"SCHEDULED").isPresent())conflict("대기 중인 재등록이 이미 있습니다.");
   if(p.productType.equals("TIME_BASED")&&active.isPresent())conflict("기간권은 기존 이용 기간이 종료된 뒤 결제해 주세요.");
   var c=new EnrollmentCycle();c.id=UUID.randomUUID();c.studioId=studio;c.enrollmentId=enrollment;c.passProductId=p.id;c.productNameSnapshot=p.name;c.productTypeSnapshot=p.productType;
   c.purchasedCount=p.totalCount;c.validityDaysSnapshot=p.validityDays;c.billingPeriodSnapshot=p.billingPeriod;c.purchasePriceSnapshot=p.price;c.deductionTriggerSnapshot=p.deductionTrigger;c.validityStartRuleSnapshot=p.validityStartRule;
   c.paymentDate=today(studio);c.createdAt=Instant.now();c.status=active.isPresent()?"SCHEDULED":"ACTIVE";
   if("MONTH".equals(p.billingPeriod))c.validityDaysSnapshot=Math.toIntExact(ChronoUnit.DAYS.between(c.paymentDate,c.paymentDate.plusMonths(1)));
   if(c.validityStartRuleSnapshot.equals("PURCHASE_DATE"))initialize(c,c.paymentDate);
   cycles.save(c);studios.flush();
   if(c.productTypeSnapshot.equals("COUNT_BASED"))ledger.append(studio,c.id,"PURCHASE",c.purchasedCount,"이용권 구매","CYCLE",c.id,actor.authenticatedUserId());
   var payment=new Payment(studio,e.customerId,actor.authenticatedUserId());payment.amount=p.price;payment.method=body.method().name();payment.status="PAID";payment.paidAt=Instant.now();payment.referenceType="LESSON_CYCLE";payment.referenceId=c.id;payments.save(payment);studios.flush();
   jdbc.update("insert into lesson_cycle_payments(studio_id,enrollment_cycle_id,payment_id) values (?,?,?)",studio,c.id,payment.id);
   return view(c);
  });
 }
 @Transactional public View adjust(UUID studio,UUID cycle,String key,Adjustment body){var actor=access.lock(studio);actor.requireOwner();
  return idem.execute(actor,"ADJUST_LESSON_CYCLE",key,Map.of("cycle",cycle,"body",body),200,View.class,()->{
   var c=find(studio,cycle);if(!c.productTypeSnapshot.equals("COUNT_BASED")||body.amount()==0)conflict("회차권에 0이 아닌 조정값을 입력해 주세요.");
   if(ledger.balance(studio,cycle)+(long)body.amount()<ledger.reserved(studio,cycle))conflict("예약된 회차보다 잔액을 줄일 수 없습니다.");
   ledger.append(studio,cycle,"MANUAL_ADJUSTMENT",body.amount(),body.reason().trim(),"ADJUSTMENT",UUID.randomUUID(),actor.authenticatedUserId());
   settle(c,today(studio));return view(c);
  });
 }
 @Transactional public EnrollmentService.View end(UUID studio,UUID enrollment,String key){var actor=access.lock(studio);
  return idem.execute(actor,"END_ENROLLMENT",key,enrollment,200,EnrollmentService.View.class,()->{
   var e=enrollments.find(studio,enrollment);var all=cycles.findByStudioIdAndEnrollmentIdOrderByCreatedAtDesc(studio,enrollment);
   if(all.stream().anyMatch(c->ledger.outstanding(studio,c.id)))conflict("미해결 예약을 먼저 처리해 주세요.");
   e.status="ENDED";e.endedAt=e.endedAt==null?Instant.now():e.endedAt;
   all.stream().filter(c->Set.of("ACTIVE","SCHEDULED").contains(c.status)).forEach(c->c.status="CANCELLED");studios.flush();return EnrollmentService.view(e);
  });
 }
 @Override @Transactional(propagation=Propagation.MANDATORY) public void apply(Payment p){
  if(!Set.of("LESSON_CYCLE","RENEWAL").contains(p.referenceType))return;
  access.manager(p.studioId);
  var ids=jdbc.queryForList("select enrollment_cycle_id from lesson_cycle_payments where studio_id=? and payment_id=?",UUID.class,p.studioId,p.id);
  if(ids.size()!=1)conflict("연결된 수강 결제를 확인할 수 없습니다.");
  var c=find(p.studioId,ids.getFirst());
  settle(c,today(p.studioId));
  boolean performed=jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id where d.studio_id=? and d.enrollment_cycle_id=? and b.status='COMPLETED'",Long.class,p.studioId,c.id)>0;
  if(!Set.of("ACTIVE","SCHEDULED").contains(c.status)||ledger.outstanding(p.studioId,c.id)||ledger.consumed(p.studioId,c.id)||performed)conflict("사용 이력·미해결 예약이 있거나 종료된 이용권은 자동 환불할 수 없습니다.");
  c.status="CANCELLED";studios.flush();activate(p.studioId,c.enrollmentId,today(p.studioId));
 }
 public EnrollmentCycle find(UUID studio,UUID id){return cycles.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"CYCLE_NOT_FOUND","이용 이력을 찾을 수 없습니다."));}
 public LocalDate today(UUID studio){return LocalDate.now(ZoneId.of(studios.findById(studio).orElseThrow().timezone));}
 public void initialize(EnrollmentCycle c,LocalDate date){if(c.startDate==null){c.startDate=date;c.validEndDate=c.validityDaysSnapshot==null?null:date.plusDays(c.validityDaysSnapshot);c.nextDueDate=c.validEndDate;}}
 public void settleEnrollment(UUID studio,UUID enrollment,LocalDate today){cycles.findByStudioIdAndEnrollmentIdAndStatus(studio,enrollment,"ACTIVE").ifPresent(c->settle(c,today));}
 public void settle(EnrollmentCycle c,LocalDate today){
  if(!c.status.equals("ACTIVE")||ledger.outstanding(c.studioId,c.id))return;
  if(c.validEndDate!=null&&today.isAfter(c.validEndDate))c.status="EXPIRED";
  else if(c.productTypeSnapshot.equals("COUNT_BASED")&&ledger.balance(c.studioId,c.id)==0)c.status="COMPLETED";
  else return;
  studios.flush();activate(c.studioId,c.enrollmentId,today);
 }
 private void activate(UUID studio,UUID enrollment,LocalDate today){
  if(!enrollments.find(studio,enrollment).status.equals("ACTIVE")||cycles.findByStudioIdAndEnrollmentIdAndStatus(studio,enrollment,"ACTIVE").isPresent())return;
  cycles.findByStudioIdAndEnrollmentIdAndStatus(studio,enrollment,"SCHEDULED").ifPresent(c->{c.status="ACTIVE";studios.flush();settle(c,today);});
 }
 public View view(EnrollmentCycle c){Long balance=c.productTypeSnapshot.equals("COUNT_BASED")?ledger.balance(c.studioId,c.id):null;Long reserved=balance==null?null:ledger.reserved(c.studioId,c.id);
  var ids=jdbc.queryForList("select payment_id from lesson_cycle_payments where studio_id=? and enrollment_cycle_id=?",UUID.class,c.studioId,c.id);
  return new View(c.id,c.enrollmentId,c.productNameSnapshot,c.productTypeSnapshot,c.purchasedCount,c.validityDaysSnapshot,c.billingPeriodSnapshot,Long.toString(c.purchasePriceSnapshot),c.deductionTriggerSnapshot,c.validityStartRuleSnapshot,c.startDate,c.validEndDate,c.paymentDate,c.status,balance,reserved,balance==null?null:balance-reserved,ids.isEmpty()?null:ids.getFirst());
 }
 public static void conflict(String message){throw new ApiException(409,"LESSON_CONFLICT",message);}
}
