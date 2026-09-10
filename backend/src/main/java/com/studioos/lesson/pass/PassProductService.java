package com.studioos.lesson.pass;
import com.studioos.common.*;
import com.studioos.lesson.LessonAccess;
import jakarta.validation.*;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service
public class PassProductService {
 public record Edit(@NotBlank @Size(max=100) String name,@NotNull Type productType,@Positive Integer totalCount,@Positive Integer validityDays,
   @NotNull Start validityStartRule,@NotBlank @Pattern(regexp="[0-9]{1,19}") String price,Trigger deductionTrigger,Period billingPeriod,boolean active){}
 public enum Type{COUNT_BASED,TIME_BASED} public enum Start{PURCHASE_DATE,FIRST_USE}
 public enum Trigger{BOOKING_CONFIRMED,LESSON_COMPLETED,ATTENDANCE_PRESENT} public enum Period{MONTH}
 public record View(UUID id,String name,String productType,Integer totalCount,Integer validityDays,String validityStartRule,String price,String deductionTrigger,String billingPeriod,boolean active){}
 private final PassProductRepository products;private final LessonAccess access;private final Validator validator;
 public PassProductService(PassProductRepository products,LessonAccess access,Validator validator){this.products=products;this.access=access;this.validator=validator;}
 @Transactional(readOnly=true) public PageResult<View> list(UUID studio,Boolean active,int page,int size){access.manager(studio);PageResult.validate(page,size);return PageResult.from(products.list(studio,active,PageRequest.of(page,size,Sort.by("name","id"))).map(PassProductService::view));}
 @Transactional(readOnly=true) public View get(UUID studio,UUID id){access.manager(studio);return view(find(studio,id));}
 @Transactional public View save(UUID studio,UUID id,Edit body){access.lock(studio);access.capability(studio,"PASS_MANAGEMENT");
  if(body==null||!validator.validate(body).isEmpty())invalid();
  long price;try{price=Long.parseLong(body.price());}catch(NumberFormatException e){throw new ApiException(400,"INVALID_PRICE","양수 원 단위 금액을 입력해 주세요.");}
  if(price<=0)invalid();
  if(body.productType()==Type.COUNT_BASED){if(body.totalCount()==null||body.deductionTrigger()==null||body.billingPeriod()!=null)invalid();}
  else if(body.totalCount()!=null||body.deductionTrigger()!=null||body.validityStartRule()!=Start.PURCHASE_DATE||(body.validityDays()==null)==(body.billingPeriod()==null))invalid();
  var p=id==null?new PassProduct():find(studio,id);
  if(id==null){p.id=UUID.randomUUID();p.studioId=studio;p.createdAt=Instant.now();}
  p.name=body.name().trim();p.productType=body.productType().name();p.totalCount=body.totalCount();p.validityDays=body.validityDays();p.validityStartRule=body.validityStartRule().name();p.price=price;
  p.deductionTrigger=body.deductionTrigger()==null?null:body.deductionTrigger().name();p.billingPeriod=body.billingPeriod()==null?null:body.billingPeriod().name();p.active=body.active();p.updatedAt=Instant.now();
  return view(products.save(p));
 }
 public PassProduct find(UUID studio,UUID id){return products.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"PASS_NOT_FOUND","이용권을 찾을 수 없습니다."));}
 public static View view(PassProduct p){return new View(p.id,p.name,p.productType,p.totalCount,p.validityDays,p.validityStartRule,Long.toString(p.price),p.deductionTrigger,p.billingPeriod,p.active);}
 private static void invalid(){throw new ApiException(400,"INVALID_PASS","상품 유형·회차·기간·차감 시점 조합을 확인해 주세요.");}
}
