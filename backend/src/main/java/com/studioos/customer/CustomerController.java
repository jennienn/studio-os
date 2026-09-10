package com.studioos.customer;
import com.studioos.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import static com.studioos.customer.CustomerDto.*;
@RestController @RequestMapping("/api/v1/studios/{studioId}/customers")
public class CustomerController {
    private final CustomerService customers;
    public CustomerController(CustomerService customers){this.customers=customers;}
    @GetMapping PageResult<View> list(@PathVariable UUID studioId,@RequestParam(defaultValue="") String search,
        @RequestParam(defaultValue="ACTIVE") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return customers.list(studioId,search,status,page,size);
    }
    @GetMapping("/{id}") View get(@PathVariable UUID studioId,@PathVariable UUID id){return customers.get(studioId,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) View create(@PathVariable UUID studioId,@Valid @RequestBody Edit edit){return customers.create(studioId,edit);}
    @PutMapping("/{id}") View edit(@PathVariable UUID studioId,@PathVariable UUID id,@Valid @RequestBody Edit edit){return customers.edit(studioId,id,edit);}
    @PostMapping("/{id}/archive") View archive(@PathVariable UUID studioId,@PathVariable UUID id){return customers.archive(studioId,id);}
}
