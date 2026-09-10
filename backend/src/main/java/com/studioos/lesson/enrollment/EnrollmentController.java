package com.studioos.lesson.enrollment;
import com.studioos.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson/enrollments")
public class EnrollmentController {
 private final EnrollmentService service;public EnrollmentController(EnrollmentService service){this.service=service;}
 @GetMapping PageResult<EnrollmentService.View> list(@PathVariable UUID studio,@RequestParam(required=false) UUID customerId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(studio,customerId,page,size);}
 @GetMapping("/{id}") EnrollmentService.View get(@PathVariable UUID studio,@PathVariable UUID id){return service.get(studio,id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) EnrollmentService.View create(@PathVariable UUID studio,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody EnrollmentService.Create body){return service.create(studio,key,body);}
}
