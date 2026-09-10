package com.studioos.payment;
import com.studioos.common.*;
import com.studioos.customer.CustomerRepository;
import com.studioos.security.*;
import com.studioos.studio.StudioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import static com.studioos.payment.PaymentDto.*;
@Service
public class PaymentService {
    private final PaymentRepository payments;private final PaymentRefundRepository refunds;private final CustomerRepository customers;
    private final OperationalAccess access;private final StudioRepository studios;private final IdempotencyService idempotency;private final Validator validator;
    public PaymentService(PaymentRepository payments,PaymentRefundRepository refunds,CustomerRepository customers,OperationalAccess access,StudioRepository studios,IdempotencyService idempotency,Validator validator){
        this.payments=payments;this.refunds=refunds;this.customers=customers;this.access=access;this.studios=studios;this.idempotency=idempotency;this.validator=validator;
    }
    @Transactional(readOnly=true) public PageResult<View> list(UUID studio,String status,UUID customer,int page,int size){
        var actor=access.manager(studio);PageResult.validate(page,size);
        if(!Set.of("ALL","PENDING","PAID","REFUNDED","CANCELLED").contains(status))invalid();
        if(customer!=null) customer(studio,customer);
        return PageResult.from(payments.search(actor.authorizedStudioId(),status,customer,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(this::view));
    }
    @Transactional(readOnly=true) public View get(UUID studio,UUID id){return view(find(access.manager(studio).authorizedStudioId(),id));}
    @Transactional public View create(UUID studio,String key,Create body){
        var actor=lock(studio);validate(body);
        return idempotency.execute(actor,"CREATE_PAYMENT",key,body,201,View.class,()->{
            var customer=customer(studio,body.customerId());if(!customer.status.equals("ACTIVE"))throw new ApiException(409,"CUSTOMER_ARCHIVED","보관된 고객에게 신규 결제를 기록할 수 없습니다.");
            long amount;try{amount=Long.parseLong(body.amount());}catch(NumberFormatException e){throw new ApiException(400,"INVALID_AMOUNT","정수 원 단위 금액 범위를 확인해 주세요.");}
            if(amount<=0 || body.referenceId()!=null || !(body.status()==Status.PENDING || body.status()==Status.PAID) || (body.status()==Status.PENDING && body.paidAt()!=null))invalid();
            var p=new Payment(studio,body.customerId(),actor.authenticatedUserId());p.amount=amount;p.method=body.method().name();p.status=body.status().name();
            p.paidAt=body.status()==Status.PAID?(body.paidAt()==null?Instant.now():body.paidAt()):null;
            payments.save(p);studios.flush();return view(p);
        });
    }
    @Transactional public View confirm(UUID studio,UUID id,String key,Confirm body){
        var actor=lock(studio);validate(body);
        return idempotency.execute(actor,"CONFIRM_PAYMENT",key,Map.of("id",id,"body",body),200,View.class,()->{
            var p=find(studio,id);require(p,"PENDING");p.status="PAID";p.paidAt=body.paidAt()==null?Instant.now():body.paidAt();studios.flush();return view(p);
        });
    }
    @Transactional public View cancel(UUID studio,UUID id,String key){
        var actor=lock(studio);return idempotency.execute(actor,"CANCEL_PAYMENT",key,id,200,View.class,()->{var p=find(studio,id);require(p,"PENDING");p.status="CANCELLED";studios.flush();return view(p);});
    }
    @Transactional public View refund(UUID studio,UUID id,String key,Refund body){
        var actor=access.manager(studio);actor.requireOwner();studios.lockById(studio).orElseThrow();validate(body);
        return idempotency.execute(actor,"REFUND_PAYMENT",key,Map.of("id",id,"body",body),200,View.class,()->{
            var p=find(studio,id);require(p,"PAID");refunds.save(new PaymentRefund(p,body.reason().trim(),actor.authenticatedUserId()));p.status="REFUNDED";studios.flush();return view(p);
        });
    }
    private AuthorizedStudioContext lock(UUID studio){var actor=access.manager(studio);studios.lockById(actor.authorizedStudioId()).orElseThrow();return actor;}
    private com.studioos.customer.Customer customer(UUID studio,UUID id){return customers.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"CUSTOMER_NOT_FOUND","고객 정보를 찾을 수 없습니다."));}
    private Payment find(UUID studio,UUID id){return payments.findByStudioIdAndId(studio,id).orElseThrow(()->new ApiException(404,"PAYMENT_NOT_FOUND","결제 기록을 찾을 수 없습니다."));}
    private void require(Payment p,String status){if(!p.status.equals(status))throw new ApiException(409,"INVALID_PAYMENT_TRANSITION","현재 상태에서 처리할 수 없습니다.");}
    private void validate(Object body){if(body==null || !validator.validate(body).isEmpty())invalid();}
    private static void invalid(){throw new ApiException(400,"INVALID_PAYMENT","결제 금액·상태·참조 정보를 확인해 주세요.");}
    private View view(Payment p){var refund=refunds.findByStudioIdAndPaymentId(p.studioId,p.id).map(r->new RefundView(r.id,Long.toString(r.amount),r.reason,r.refundedAt,r.createdByUserId)).orElse(null);
        return new View(p.id,p.studioId,p.customerId,customer(p.studioId,p.customerId).name,Long.toString(p.amount),p.currency,p.method,p.status,p.referenceType,p.referenceId,p.paidAt,p.createdAt,p.createdByUserId,refund);}
}
