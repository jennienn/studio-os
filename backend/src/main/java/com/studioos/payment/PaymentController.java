package com.studioos.payment;
import com.studioos.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.studioos.payment.PaymentDto.*;
@RestController @RequestMapping("/api/v1/studios/{studioId}/payments")
public class PaymentController {
    private final PaymentService payments;public PaymentController(PaymentService payments){this.payments=payments;}
    @GetMapping PageResult<View> list(@PathVariable UUID studioId,@RequestParam(defaultValue="ALL") String status,@RequestParam(required=false) UUID customerId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return payments.list(studioId,status,customerId,page,size);}
    @GetMapping("/{id}") View get(@PathVariable UUID studioId,@PathVariable UUID id){return payments.get(studioId,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) View create(@PathVariable UUID studioId,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Create body){return payments.create(studioId,key,body);}
    @PostMapping("/{id}/confirm") View confirm(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Confirm body){return payments.confirm(studioId,id,key,body);}
    @PostMapping("/{id}/cancel") View cancel(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return payments.cancel(studioId,id,key);}
    @PostMapping("/{id}/refund") View refund(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Refund body){return payments.refund(studioId,id,key,body);}
}
