package com.studioos.booking;
import com.studioos.common.*;
import com.studioos.customer.CustomerRepository;
import com.studioos.security.*;
import com.studioos.staff.StaffRepository;
import com.studioos.studio.*;
import com.studioos.studio.configuration.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import static com.studioos.booking.BookingDto.*;
@Service
public class BookingService {
    private final List<BookingSpecialization> specializations;private final BookingRepository bookings;private final BookingBlockRepository blocks;private final CustomerRepository customers;
    private final StaffRepository staff;private final OperationalAccess access;private final StudioRepository studios;
    private final BookingAvailability availability;private final IdempotencyService idempotency;private final Validator validator;
    private final StudioCapabilityRepository capabilities;
    public BookingService(BookingRepository bookings,BookingBlockRepository blocks,CustomerRepository customers,StaffRepository staff,
        OperationalAccess access,StudioRepository studios,BookingAvailability availability,IdempotencyService idempotency,Validator validator,StudioCapabilityRepository capabilities,List<BookingSpecialization> specializations){this.specializations=specializations;
        this.bookings=bookings;this.blocks=blocks;this.customers=customers;this.staff=staff;this.access=access;this.studios=studios;this.availability=availability;this.idempotency=idempotency;this.validator=validator;this.capabilities=capabilities;
    }
    @Transactional(readOnly=true) public PageResult<View> list(UUID studio,Instant from,Instant to,String status,int page,int size){
        var actor=access.member(studio);BookingAvailability.interval(from,to);PageResult.validate(page,size);
        if(!Set.of("ALL","PENDING","CONFIRMED","COMPLETED","CANCELLED","NO_SHOW").contains(status))invalid();
        UUID assigned=null;if(actor.membershipRole()==StudioMembership.Role.STAFF)assigned=ownStaff(actor);
        return PageResult.from(bookings.range(studio,from,to,assigned,status,PageRequest.of(page,size,Sort.by("startAt","id"))).map(b->view(b,actor.membershipRole()==StudioMembership.Role.STAFF)));
    }
    @Transactional(readOnly=true) public View get(UUID studio,UUID id){var actor=access.member(studio);var b=find(studio,id);
        if(actor.membershipRole()==StudioMembership.Role.STAFF && !ownStaff(actor).equals(b.staffId))throw new ApiException(403,"BOOKING_FORBIDDEN","배정된 예약만 조회할 수 있습니다.");
        return view(b,actor.membershipRole()==StudioMembership.Role.STAFF);
    }
    @Transactional public View create(UUID studio,String key,Create body){var actor=lock(studio);validate(body);
        return idempotency.execute(actor,"CREATE_BOOKING",key,body,201,View.class,()->{
            var s=studios.findById(studio).orElseThrow();validateKind(s,body.bookingKind());
            var c=customers.findByStudioIdAndId(studio,body.customerId()).orElseThrow(()->new ApiException(404,"CUSTOMER_NOT_FOUND","고객 정보를 찾을 수 없습니다."));
            if(!c.status.equals("ACTIVE"))throw new ApiException(409,"CUSTOMER_ARCHIVED","보관된 고객은 신규 예약을 만들 수 없습니다.");
            availability.check(s,body.staffId(),body.startAt(),body.endAt(),null);
            var b=bookings.save(new Booking(studio,c.id,body.staffId(),body.bookingKind().name(),body.startAt(),body.endAt(),body.note()));studios.flush();return view(b,false);
        });
    }
    @Transactional public View edit(UUID studio,UUID id,String key,Edit body){var actor=lock(studio);validate(body);
        return idempotency.execute(actor,"RESCHEDULE_BOOKING",key,Map.of("id",id,"body",body),200,View.class,()->{
            var b=find(studio,id);if(!b.manualEntry)specialization(b).validateEdit(b,body);if(!Set.of("PENDING","CONFIRMED").contains(b.status))transitionError();
            availability.check(studios.findById(studio).orElseThrow(),body.staffId(),body.startAt(),body.endAt(),id);
            b.staffId=body.staffId();b.startAt=body.startAt();b.endAt=body.endAt();b.note=body.note();b.updatedAt=Instant.now();studios.flush();return view(b,false);
        });
    }
    @Transactional public View transition(UUID studio,UUID id,String key,String command){
        var actor=access.member(studio);studios.lockById(studio).orElseThrow();var current=find(studio,id);
        if(current.manualEntry)access.manager(studio);else specialization(current).authorize(current,actor,command);
        return idempotency.execute(actor,command+"_BOOKING",key,id,200,View.class,()->{
            var booking=find(studio,id);if(!booking.manualEntry)specialization(booking).validateTransition(booking,actor,command);BookingTransitions.apply(booking,command);studios.flush();
            if(!booking.manualEntry)specialization(booking).afterTransition(booking,actor,command);
            studios.flush();return view(booking,actor.membershipRole()==StudioMembership.Role.STAFF);
        });
    }
    private BookingSpecialization specialization(Booking booking){return specializations.stream().filter(s->s.supports(booking)).findFirst().orElseThrow(()->new ApiException(409,"SPECIALIZATION_REQUIRED","전문 예약 처리가 필요합니다."));}
    @Transactional(readOnly=true) public List<StaffView> staff(UUID studio){access.manager(studio);return staff.findByStudioIdAndActiveOrderByNameAsc(studio,true).stream().map(s->new StaffView(s.id,s.name)).toList();}
    @Transactional(readOnly=true) public Availability available(UUID studio,UUID staff,Instant start,Instant end){access.manager(studio);
        try{availability.check(studios.findById(studio).orElseThrow(),staff,start,end,null);return new Availability(true,"AVAILABLE","예약 가능한 시간입니다.");}
        catch(ApiException e){if(e.status!=409)throw e;return new Availability(false,e.code,e.getMessage());}
    }
    @Transactional(readOnly=true) public PageResult<BlockView> blocks(UUID studio,Instant from,Instant to,int page,int size){access.manager(studio);BookingAvailability.interval(from,to);PageResult.validate(page,size);return PageResult.from(blocks.range(studio,from,to,PageRequest.of(page,size,Sort.by("startAt","id"))).map(this::blockView));}
    @Transactional public BlockView block(UUID studio,String key,BlockCreate body){var actor=lock(studio);validate(body);
        return idempotency.execute(actor,"CREATE_BOOKING_BLOCK",key,body,201,BlockView.class,()->{
            BookingAvailability.interval(body.startAt(),body.endAt());
            if((body.scopeType()==Scope.STUDIO && body.staffId()!=null)||(body.scopeType()==Scope.STAFF && body.staffId()==null))invalid();
            if(body.staffId()!=null)availability.staff(studio,body.staffId());
            if(bookings.conflicts(studio,body.staffId(),body.startAt(),body.endAt(),null)>0)throw new ApiException(409,"BLOCK_CONFLICT","기존 예약을 먼저 이동하거나 취소해 주세요.");
            var b=blocks.save(new BookingBlock(studio,body.scopeType().name(),body.staffId(),body.startAt(),body.endAt(),body.reason()));studios.flush();return blockView(b);
        });
    }
    @Transactional public Deleted deleteBlock(UUID studio,UUID id,String key){var actor=lock(studio);return idempotency.execute(actor,"DELETE_BOOKING_BLOCK",key,id,200,Deleted.class,()->{
        blocks.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"BLOCK_NOT_FOUND","차단을 찾을 수 없습니다."));blocks.deleteByStudioIdAndId(studio,id);studios.flush();return new Deleted(id,true);
    });}
    private AuthorizedStudioContext lock(UUID studio){var actor=access.manager(studio);studios.lockById(actor.authorizedStudioId()).orElseThrow();return actor;}
    private UUID ownStaff(AuthorizedStudioContext actor){return staff.findByStudioIdAndUserId(actor.authorizedStudioId(),actor.authenticatedUserId()).orElseThrow(()->new ApiException(403,"STAFF_PROFILE_REQUIRED","배정된 담당자 프로필이 없습니다.")).id;}
    private Booking find(UUID studio,UUID id){return bookings.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"BOOKING_NOT_FOUND","예약을 찾을 수 없습니다."));}
    private void validateKind(Studio s,Kind kind){
        if(kind==Kind.LESSON_GROUP)throw new ApiException(409,"GROUP_NOT_AVAILABLE","그룹 예약은 아직 지원하지 않습니다.");
        if((s.businessCategory.equals("LESSON") && kind!=Kind.LESSON_PRIVATE)||(s.businessCategory.equals("BEAUTY") && kind!=Kind.BEAUTY_SERVICE))invalid();
        if(kind==Kind.LESSON_PRIVATE && capabilities.findByStudioIdOrderByCapabilityAsc(s.id).stream().noneMatch(c->c.capability==StudioTaxonomy.Capability.PRIVATE_LESSON && c.enabled))
            throw new ApiException(409,"PRIVATE_DISABLED","개인 레슨 운영이 비활성화되어 있습니다.");
    }
    private void manual(Booking b){if(!b.manualEntry)throw new ApiException(409,"SPECIALIZATION_REQUIRED","이 예약은 해당 전문 예약 기능에서 처리해야 합니다.");}
    private void validate(Object body){if(body==null || !validator.validate(body).isEmpty())invalid();}
    private static void invalid(){throw new ApiException(400,"INVALID_BOOKING","예약 입력과 사업장 유형을 확인해 주세요.");}
    private static void transitionError(){throw new ApiException(409,"INVALID_BOOKING_TRANSITION","현재 예약 상태에서 처리할 수 없습니다.");}
    private View view(Booking b,boolean limited){var c=customers.findByStudioIdAndId(b.studioId,b.customerId).orElseThrow();String name=b.staffId==null?null:staff.findByStudioIdAndId(b.studioId,b.staffId).orElseThrow().name;
        return new View(b.id,b.studioId,b.customerId,c.name,c.phone,b.staffId,name,b.bookingKind,b.manualEntry,b.startAt,b.endAt,b.status,limited?null:b.note,b.source);}
    private BlockView blockView(BookingBlock b){return new BlockView(b.id,b.scopeType,b.staffId,b.startAt,b.endAt,b.reason);}
}
