package com.studioos.lesson.attendance;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson")
public class GroupController {
 private final GroupService service;public GroupController(GroupService service){this.service=service;}
 @PostMapping("/group-bookings") @ResponseStatus(HttpStatus.CREATED) GroupService.Created create(@PathVariable UUID studio,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody GroupService.Create body){return service.create(studio,key,body);}
 @GetMapping("/occurrences/{id}/attendance") List<GroupService.Row> list(@PathVariable UUID studio,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.attendance(studio,id,page,size);}
 @PutMapping("/occurrences/{id}/attendance") GroupService.Result mark(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody GroupService.Bulk body){return service.mark(studio,id,key,body);}
}
