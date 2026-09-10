package com.studioos.lesson.cycle;
import com.studioos.lesson.enrollment.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson")
public class CycleController {
 private final CycleService service;public CycleController(CycleService service){this.service=service;}
 @GetMapping("/enrollments/{id}/cycles") List<CycleService.View> list(@PathVariable UUID studio,@PathVariable UUID id){return service.list(studio,id);}
 @PostMapping("/enrollments/{id}/renew") @ResponseStatus(HttpStatus.CREATED) CycleService.View renew(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CycleService.Purchase body){return service.purchase(studio,id,key,body);}
 @PostMapping("/enrollments/{id}/end") EnrollmentService.View end(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key){return service.end(studio,id,key);}
 @PostMapping("/cycles/{id}/adjustments") CycleService.View adjust(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CycleService.Adjustment body){return service.adjust(studio,id,key,body);}
 @GetMapping("/cycles/{id}/ledger") List<LessonLedger.Entry> ledger(@PathVariable UUID studio,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.history(studio,id,page,size);}
}
