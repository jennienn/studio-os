package com.studioos.lesson.pass;
import com.studioos.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson/pass-products")
public class PassProductController {
 private final PassProductService service;public PassProductController(PassProductService service){this.service=service;}
 @GetMapping PageResult<PassProductService.View> list(@PathVariable UUID studio,@RequestParam(required=false) Boolean active,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(studio,active,page,size);}
 @GetMapping("/{id}") PassProductService.View get(@PathVariable UUID studio,@PathVariable UUID id){return service.get(studio,id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) PassProductService.View create(@PathVariable UUID studio,@Valid @RequestBody PassProductService.Edit body){return service.save(studio,null,body);}
 @PutMapping("/{id}") PassProductService.View edit(@PathVariable UUID studio,@PathVariable UUID id,@Valid @RequestBody PassProductService.Edit body){return service.save(studio,id,body);}
}
